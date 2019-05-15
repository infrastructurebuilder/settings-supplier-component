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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.codehaus.plexus.util.StringUtils;
import org.infrastructurebuilder.IBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class DefaultSettingsSupplier implements SettingsSupplier {
  public final static Logger log = LoggerFactory.getLogger(DefaultSettingsSupplier.class);
  public static final String SETTINGS_XML = "settings.xml";
  public static final String MAVEN_HOME = "MAVEN_HOME";
  public static final String GLOBAL_SETTINGS_FILE = "GLOBAL_SETTINGS_FILE";
  public static final String USER_SETTINGS_FILE = "USER_SETTINGS_FILE";

  public static final String USER_HOME = System.getProperty("user.home");

  public static final Path USER_MAVEN_CONFIGURATION_HOME = Paths.get(USER_HOME).resolve(".m2");
  public static final Path DEFAULT_MAVEN_LOCAL_REPO = USER_MAVEN_CONFIGURATION_HOME.resolve("repository");

  public static final Path DEFAULT_USER_SETTINGS_FILE = USER_MAVEN_CONFIGURATION_HOME
      .resolve(DefaultSettingsSupplier.SETTINGS_XML);

  public static final Optional<Path> DEFAULT_GLOBAL_SETTINGS_FILE = Optional
      .ofNullable(System.getenv(DefaultSettingsSupplier.MAVEN_HOME)).map(Paths::get)
      .map(x -> x.resolve("conf").resolve(DefaultSettingsSupplier.SETTINGS_XML));

  private final Map<String, String> env;
  private final Settings settings;

  @Inject
  public DefaultSettingsSupplier(EnvSupplier envSupplier, @Named("default") PropertiesSupplier propSupplier,
      SettingsBuilder settingsBuilder) {
    this.env = Objects.requireNonNull(envSupplier).get();
    SettingsBuildingRequest settingsRequest = new DefaultSettingsBuildingRequest();
    log.debug("Got the SettingsBuildingRequest");
    settingsRequest.setGlobalSettingsFile(getGlobalSettingsFile().toFile());
    log.debug("setGlobalSettingsFile");
    settingsRequest.setUserSettingsFile(getUserSettingsFile().toFile());
    log.debug("setUserSettingsFile");
    settingsRequest.setSystemProperties(propSupplier.get()); // Here is where you'd set anything you wanted to
    log.debug("setSystemProperties");
    settingsRequest.setUserProperties(new Properties()); // No additional properties within this component
    log.debug("setUserProperties");
    this.settings = getEffectiveSettingsForRequest.apply(settingsBuilder, settingsRequest);

  }

  public Path getUserSettingsFile() {
    Path p = Optional.ofNullable(Objects.requireNonNull(env).get(DefaultSettingsSupplier.USER_SETTINGS_FILE))
        .map(Paths::get).orElse(DEFAULT_USER_SETTINGS_FILE).toAbsolutePath();
    if (!(Files.exists(p) && Files.isRegularFile(p) && Files.isReadable(p)))
      throw new RuntimeException("User Settings file " + p.toString() + " is not a readable, regular file");
    return p;
  }

  public Path getGlobalSettingsFile() {
    Path p;
    String alternateGlobalSettings = Objects.requireNonNull(env).get(DefaultSettingsSupplier.GLOBAL_SETTINGS_FILE);
    if (alternateGlobalSettings == null) {
      String mavenHome = env.get(DefaultSettingsSupplier.MAVEN_HOME);
      if (mavenHome == null) {
        log.warn("No MAVEN_HOME set!!!  Resolving from SDKMAN...");
        String home = System.getProperty("user.home");
        Path sdkMan = Paths.get(home).resolve(".sdkman").resolve("candidates").resolve("maven").resolve("current");
        if (!Files.isSymbolicLink(sdkMan))
          throw new RuntimeException("MAVEN_HOME is not set to symlink of 'current'");
        mavenHome = sdkMan.toAbsolutePath().toString();
      }
      p = Paths.get(mavenHome).resolve("conf").resolve(DefaultSettingsSupplier.SETTINGS_XML);
    } else
      p = Paths.get(alternateGlobalSettings);
    if (!(Files.exists(p) && Files.isRegularFile(p) && Files.isReadable(p)))
      throw new RuntimeException("Global Settings file " + p.toString() + " is not available");
    return p;
  }

  @Override
  public Settings get() {
    return settings;
  }

  public final static Function<SettingsBuildingResult, Settings> logProblems = (
      SettingsBuildingResult settingsResult) -> {
    settingsResult.getProblems()
        .forEach(problem -> log.warn("Problem: " + problem.getMessage() + " @ " + problem.getLocation()));
    Settings s = settingsResult.getEffectiveSettings();
    if (StringUtils.isBlank(s.getLocalRepository())) {
      s.setLocalRepository(DEFAULT_MAVEN_LOCAL_REPO.toAbsolutePath().toString());
    }
    Path lp = Paths.get(s.getLocalRepository());
    if (!Files.exists(lp))
      IBException.cet.withTranslation(() -> Files.createDirectories(lp));
    return s;
  };

  public final static BiFunction<SettingsBuilder, SettingsBuildingRequest, Settings> getEffectiveSettingsForRequest = (
      settingsBuilder, settingsRequest) -> {
    try {
      SettingsBuildingResult settingsResult;
      return logProblems.apply(settingsBuilder.build(settingsRequest));
    } catch (NullPointerException | SettingsBuildingException e) {
      throw new RuntimeException(e);
    }
  };

}
