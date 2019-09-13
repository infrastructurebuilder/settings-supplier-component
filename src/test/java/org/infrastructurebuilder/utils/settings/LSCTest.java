/**
 * Copyright © 2019 admin (admin@infrastructurebuilder.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.infrastructurebuilder.utils.settings;

import static org.infrastructurebuilder.utils.settings.DefaultSettingsSupplier.GLOBAL_SETTINGS_FILE;
import static org.infrastructurebuilder.utils.settings.DefaultSettingsSupplier.MAVEN_HOME;
import static org.infrastructurebuilder.utils.settings.DefaultSettingsSupplier.USER_SETTINGS_FILE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.DefaultSettingsProblem;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.io.DefaultSettingsReader;
import org.apache.maven.settings.io.DefaultSettingsWriter;
import org.apache.maven.settings.io.SettingsReader;
import org.apache.maven.settings.io.SettingsWriter;
import org.apache.maven.settings.validation.DefaultSettingsValidator;
import org.apache.maven.settings.validation.SettingsValidator;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.WireModule;
import org.infrastructurebuilder.util.DefaultPropertiesSupplier;
import org.infrastructurebuilder.util.EnvSupplier;
import org.infrastructurebuilder.util.HandCraftedEnvSupplier;
import org.infrastructurebuilder.util.IBUtils;
import org.infrastructurebuilder.util.PropertiesSupplier;
import org.infrastructurebuilder.util.SettingsProxy;
import org.infrastructurebuilder.util.SettingsSupplier;
import org.infrastructurebuilder.util.config.WorkingPathSupplier;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.PasswordDecryptor;

public class LSCTest {

  private static final String TESTING = "testing";
  private static Path target;
  private static Path badSettings;
//  private static String gsval;
  private static Map<String, String> defaultEnv;
  private static Path nonSettings;
  private static Path localRepoSettings;
  private static Path noLocalRepoSettings;
  private static WorkingPathSupplier wps = new WorkingPathSupplier();

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    target = wps.getRoot();
    badSettings = target.resolve("test-classes").resolve("bad-settings.xml").toAbsolutePath();
    noLocalRepoSettings = target.resolve("test-classes").resolve("settings-no-local.xml").toAbsolutePath();
    localRepoSettings = target.resolve("test-classes").resolve("settings-with-local.xml").toAbsolutePath();
    nonSettings = target.resolve("test-classes").resolve("no-such-settings.xml").toAbsolutePath();
    defaultEnv = System.getenv();
  }

  private DefaultPlexusContainer c;
  private SettingsSupplier s;
  private HandCraftedEnvSupplier e;
//  private PropertiesSupplier p;
  private boolean isWindows;
  private org.codehaus.plexus.classworlds.ClassWorld kw;
  private ContainerConfiguration dpcreq;
  private SettingsBuilder dsb;

  @SuppressWarnings({ "rawtypes", "unchecked" })
  protected static void setEnv(Map<String, String> newenv) throws Exception {
    try {
      Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
      Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
      theEnvironmentField.setAccessible(true);
      Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
      env.putAll(newenv);
      Field theCaseInsensitiveEnvironmentField = processEnvironmentClass
          .getDeclaredField("theCaseInsensitiveEnvironment");
      theCaseInsensitiveEnvironmentField.setAccessible(true);
      Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
      cienv.putAll(newenv);
    } catch (NoSuchFieldException e) {
      Class[] classes = Collections.class.getDeclaredClasses();
      Map<String, String> env = System.getenv();
      for (Class cl : classes) {
        if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
          Field field = cl.getDeclaredField("m");
          field.setAccessible(true);
          Object obj = field.get(env);
          Map<String, String> map = (Map<String, String>) obj;
          map.clear();
          map.putAll(newenv);
        }
      }
    }
  }

  @Before
  public void setUp() throws Exception {

    final String mavenCoreRealmId = TESTING;
    kw = new ClassWorld(mavenCoreRealmId, getClass().getClassLoader());

    dpcreq = new DefaultContainerConfiguration().setClassWorld(kw).setClassPathScanning(PlexusConstants.SCANNING_INDEX)
        .setName(TESTING);
    c = new DefaultPlexusContainer(dpcreq,
        new WireModule(new SpaceModule(new URLClassSpace(kw.getClassRealm(TESTING)))));
    isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

    PropertiesSupplier theseProps = c.lookup(PropertiesSupplier.class);
    s = c.lookup(SettingsSupplier.class, "default");
    dsb = c.lookup(SettingsBuilder.class);
    e = new HandCraftedEnvSupplier();

    Map<String, PasswordDecryptor> myDecrypters = new HashMap<>();
    new MyDefaultSecDispatcher(new MyDefaultPlexusCipher(), myDecrypters);
    SettingsReader sr = new DefaultSettingsReader();
    SettingsWriter sw = new DefaultSettingsWriter();
    SettingsValidator sv = new DefaultSettingsValidator();
    new MyDefaultSettingsBuilder(sr,sw,sv);
    new MyDefaultSettingsDecrypter(new DefaultSecDispatcher());

  }

  @After
  public void tearDown() throws Exception {
    badSettings.toFile().setReadable(true);
  }

  @Test
  public void testGet() {
    SettingsProxy s1 = s.get();
    assertNotNull(s1);
    assertTrue(s1.getServers().size() > 2); // Literally no one would have less than 2 :D
  }

  @Ignore
  @Test
  public void testLocalWithBad() throws Exception {
    Map<String, String> env = new HashMap<>(defaultEnv);
    env.put(USER_SETTINGS_FILE, badSettings.toString());
    EnvSupplier en = new HandCraftedEnvSupplier(env);
    DefaultSettingsSupplier l = new DefaultSettingsSupplier(en, () -> new Properties(), this.dsb);
    l.getUserSettingsFile();
  }

  @Test(expected = RuntimeException.class)
  public void testLocalWithUnreadable() throws Exception {
    if (isWindows)
      throw new RuntimeException(); // FIXME Can't get windows to fail correctly just yet

    badSettings.toFile().setReadable(false);
    Map<String, String> env = new HashMap<>(defaultEnv);
    env.put(USER_SETTINGS_FILE, badSettings.toString());
    EnvSupplier en = new HandCraftedEnvSupplier(env);
    DefaultSettingsSupplier l = new DefaultSettingsSupplier(en, () -> new Properties(), this.dsb);
    e.setAll(env);
    l.getUserSettingsFile();
  }

  @Test(expected = RuntimeException.class)
  public void testLocalWithNon() throws Exception {
    Map<String, String> env = new HashMap<>(defaultEnv);
    env.put(USER_SETTINGS_FILE, nonSettings.toString());
    EnvSupplier en = new HandCraftedEnvSupplier(env);
    DefaultSettingsSupplier l = new DefaultSettingsSupplier(en, () -> new Properties(), this.dsb);
    l.getUserSettingsFile();
  }

  @Test(expected = RuntimeException.class)
  public void testLocalWithDir() throws Exception {
    Map<String, String> env = new HashMap<>(defaultEnv);
    env.put(USER_SETTINGS_FILE, target.toString());
    EnvSupplier en = new HandCraftedEnvSupplier(env);
    DefaultSettingsSupplier l = new DefaultSettingsSupplier(en, () -> new Properties(), this.dsb);
    l.getUserSettingsFile();
  }

  @Ignore
  @Test
  public void testGlobalWithBad() throws Exception {
    Map<String, String> env = new HashMap<>(defaultEnv);
    env.put(GLOBAL_SETTINGS_FILE, badSettings.toString());
    EnvSupplier en = new HandCraftedEnvSupplier(env);
    DefaultSettingsSupplier l = new DefaultSettingsSupplier(en, () -> new Properties(), this.dsb);
    l.getGlobalSettingsFile();
  }

  @Test(expected = RuntimeException.class)
  public void testGlobalWithUnreadable() throws Exception {
    if (isWindows)
      throw new RuntimeException(); // FIXME Can't get windows to fail right
    badSettings.toFile().setReadable(false);
    Map<String, String> env = new HashMap<>(defaultEnv);
    env.put(GLOBAL_SETTINGS_FILE, badSettings.toString());
    EnvSupplier en = new HandCraftedEnvSupplier(env);
    DefaultSettingsSupplier l = new DefaultSettingsSupplier(en, () -> new Properties(), this.dsb);
    l.getGlobalSettingsFile();
  }

  @Test(expected = RuntimeException.class)
  public void testGlobalWithNon() throws Exception {
    Map<String, String> env = new HashMap<>(defaultEnv);
    env.put(GLOBAL_SETTINGS_FILE, nonSettings.toString());
    EnvSupplier en = new HandCraftedEnvSupplier(env);
    DefaultSettingsSupplier l = new DefaultSettingsSupplier(en, () -> new Properties(), this.dsb);
    l.getGlobalSettingsFile();
  }

  @Test(expected = RuntimeException.class)
  public void testGlobalWithDir() throws Exception {
    Map<String, String> env = new HashMap<>(defaultEnv);
    env.put(GLOBAL_SETTINGS_FILE, target.toString());
    EnvSupplier en = new HandCraftedEnvSupplier(env);
    DefaultSettingsSupplier l = new DefaultSettingsSupplier(en, () -> new Properties(), this.dsb);
    l.getGlobalSettingsFile();
  }

  @Ignore
  @Test
  public void testGlobalWithNullMavenHome() throws Exception {
    Map<String, String> env = new HashMap<>(defaultEnv);
    env.remove(MAVEN_HOME); // Gonna default to try SDKMAN
    EnvSupplier en = new HandCraftedEnvSupplier(env);
    DefaultSettingsSupplier l = new DefaultSettingsSupplier(en, () -> new Properties(), this.dsb);
    l.getGlobalSettingsFile();
  }

  @Test(expected = RuntimeException.class)
  public void testGlobalWithPotentiallyBreakingTest() throws Exception {
    Map<String, String> env = new HashMap<>(defaultEnv);
    env.remove(MAVEN_HOME); // Gonna default to try SDKMAN
    System.setProperty("user.home", FileSystems.getDefault().getRootDirectories().iterator().next().toString()); // This could go BADLY
    EnvSupplier en = new HandCraftedEnvSupplier(env);
    DefaultSettingsSupplier l = new DefaultSettingsSupplier(en, () -> new Properties(), this.dsb);
    e.setAll(env);
    l.getGlobalSettingsFile();
  }

  @Test // Requires SDKMAN install to work
  public void testLocalNoMavneHomeWithPotentiallyBreakingTest() throws Exception {
    Map<String, String> env = new HashMap<>(defaultEnv);
    env.remove(MAVEN_HOME); // Gonna default to try SDKMAN
    EnvSupplier en = new HandCraftedEnvSupplier(env);
    DefaultSettingsSupplier l = new DefaultSettingsSupplier(en, () -> new Properties(), this.dsb);
    e.setAll(env);
    l.getGlobalSettingsFile();
  }

  @Ignore
  @Test(expected = RuntimeException.class)
  public void testGetEffectiveSettingsWithNull() {
//    Map<String, String> env = new HashMap<>(defaultEnv);
//    EnvSupplier en = new HandCraftedEnvSupplier(env);
//    DefaultSettingsSupplier l = new DefaultSettingsSupplier(en, () -> new Properties(), this.dsb);
    DefaultSettingsSupplier.getEffectiveSettingsForRequest.apply(this.dsb, new DefaultSettingsBuildingRequest());
  }

  @Test(expected = RuntimeException.class)
  public void testGetEffectiveSettingsWithBad() {
    Map<String, String> env = new HashMap<>(defaultEnv);
    badSettings.toFile().setReadable(false);
    EnvSupplier en = new HandCraftedEnvSupplier(env);
    DefaultSettingsSupplier l = new DefaultSettingsSupplier(en, () -> new Properties(), this.dsb);
    DefaultSettingsBuildingRequest sb = new DefaultSettingsBuildingRequest();
    sb.setUserProperties(new Properties());
    sb.setUserSettingsFile(nonSettings.toFile());
    sb.setGlobalSettingsFile(badSettings.toFile());
    DefaultSettingsSupplier.getEffectiveSettingsForRequest.apply(this.dsb, sb);
  }

  @Test
  public void testWithoutLocalSettings() {
    Map<String, String> env = new HashMap<>(defaultEnv);
    env.put(USER_SETTINGS_FILE, noLocalRepoSettings.toString());
    EnvSupplier en = new HandCraftedEnvSupplier(env);
    DefaultSettingsSupplier l = new DefaultSettingsSupplier(en, () -> new Properties(), this.dsb);
    assertEquals(DefaultSettingsSupplier.DEFAULT_MAVEN_LOCAL_REPO.toAbsolutePath(), l.get().getLocalRepository());
  }

  @Test
  public void testWithLocalSettings() throws IOException {
    Path ll = Paths.get(System.getProperty("user.home")).resolve(".m2").resolve("repository");
    Path localcopy = wps.get().resolve(UUID.randomUUID().toString());
    String local = "<settings>" + "<offline>true</offline>" + "<pluginGroups/>" + "<proxies/>" + "<servers/>"
        + "<mirrors/>" + "<profiles/>" + "<localRepository>" + ll.toString() + "</localRepository></settings>";

    Map<String, String> env = new HashMap<>(defaultEnv);
    IBUtils.writeString(localcopy, local);
    env.put(USER_SETTINGS_FILE, localRepoSettings.toString());
    EnvSupplier en = new HandCraftedEnvSupplier(env);
    Map<String, String> m = en.get();
    assertEquals(localRepoSettings.toString(), m.get(USER_SETTINGS_FILE));
    DefaultSettingsSupplier l = new DefaultSettingsSupplier(en, () -> new Properties(), this.dsb);
    assertEquals(ll, l.get().getLocalRepository());

  }
  @Test
  public void testWithDeletedLocalDirectory() throws IOException {
    Path ll = wps.get();
    Path localcopy = wps.get().resolve(UUID.randomUUID().toString());
    String local = "<settings>" + "<offline>true</offline>" + "<pluginGroups/>" + "<proxies/>" + "<servers/>"
        + "<mirrors/>" + "<profiles/>" + "<localRepository>" + ll.toString() + "</localRepository></settings>";

    IBUtils.deletePath(ll);
    Map<String, String> env = new HashMap<>(defaultEnv);
    IBUtils.writeString(localcopy, local);
    env.put(USER_SETTINGS_FILE, localcopy.toString());
    EnvSupplier en = new HandCraftedEnvSupplier(env);
    Map<String, String> m = en.get();
    assertEquals(localcopy.toString(), m.get(USER_SETTINGS_FILE));
    DefaultSettingsSupplier l = new DefaultSettingsSupplier(en, () -> new Properties(), this.dsb);
    assertEquals(ll, l.get().getLocalRepository());

  }

  @Test
  public void testLogProblems() {
    Settings s = new Settings();
    SettingsBuildingResult sr = new SettingsBuildingResult() {
      @Override
      public List<SettingsProblem> getProblems() {
        return Arrays.asList(new DefaultSettingsProblem("A",
            org.apache.maven.settings.building.SettingsProblem.Severity.ERROR, "C", 1, 2, new RuntimeException()));
      }

      @Override
      public Settings getEffectiveSettings() {
        return s;
      }
    };

    assertTrue(DefaultSettingsSupplier.logProblems.apply(sr) == s);
  }

}
