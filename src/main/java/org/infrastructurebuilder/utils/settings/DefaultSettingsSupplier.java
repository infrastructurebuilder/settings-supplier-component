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

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.settings.Activation;
import org.apache.maven.settings.ActivationFile;
import org.apache.maven.settings.ActivationOS;
import org.apache.maven.settings.ActivationProperty;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.RepositoryPolicy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;
import org.infrastructurebuilder.IBException;
import org.infrastructurebuilder.util.ActivationFileProxy;
import org.infrastructurebuilder.util.ActivationOSProxy;
import org.infrastructurebuilder.util.ActivationPropertyProxy;
import org.infrastructurebuilder.util.ActivationProxy;
import org.infrastructurebuilder.util.ChecksumPolicy;
import org.infrastructurebuilder.util.EnvSupplier;
import org.infrastructurebuilder.util.IBUtils;
import org.infrastructurebuilder.util.Layout;
import org.infrastructurebuilder.util.MirrorProxy;
import org.infrastructurebuilder.util.ProfileProxy;
import org.infrastructurebuilder.util.PropertiesSupplier;
import org.infrastructurebuilder.util.ProxyProxy;
import org.infrastructurebuilder.util.RepositoryPolicyProxy;
import org.infrastructurebuilder.util.RepositoryProxy;
import org.infrastructurebuilder.util.ServerProxy;
import org.infrastructurebuilder.util.SettingsProxy;
import org.infrastructurebuilder.util.SettingsSupplier;
import org.infrastructurebuilder.util.UpdatePolicy;
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

  public final static Function<String, Optional<Path>> s2p = (p) -> {
    return Optional.ofNullable(p).map(Paths::get);
  };

  @Override
  public SettingsProxy get() {
    return proxyFromSettings.apply(settings);
  }

  public final static Function<Server, ServerProxy> proxyFromServer = (s2) -> {
    Optional<String> config = ofNullable(s2.getConfiguration()).map(dom -> {
      StringWriter writer = new StringWriter();
      Xpp3DomWriter.write(writer, (Xpp3Dom) dom);
      return writer.toString();
    });
    return new ServerProxy(s2.getId(), ofNullable(s2.getUsername()), ofNullable(s2.getPassword()),
        ofNullable(s2.getPassphrase()), ofNullable(s2.getPrivateKey()).map(Paths::get),
        ofNullable(s2.getFilePermissions()), ofNullable(s2.getDirectoryPermissions()), config);
  };

  public final static Function<ActivationFile, Optional<ActivationFileProxy>> proxyFromActFile = (a) -> {
    return Optional.ofNullable(a)
        .map(af -> new ActivationFileProxy(s2p.apply(af.getExists()), s2p.apply(af.getMissing())));
  };
  public final static Function<ActivationOS, Optional<ActivationOSProxy>> proxyFromActOs = (o1) -> {
    return ofNullable(o1).map(o -> new ActivationOSProxy(ofNullable(o.getArch()), ofNullable(o.getFamily()),
        ofNullable(o.getName()), ofNullable(o.getVersion())));
  };
  public final static Function<ActivationProperty, Optional<ActivationPropertyProxy>> proxyFromActProperty = (p1) -> {
    return ofNullable(p1).map(p -> new ActivationPropertyProxy(p.getName(), ofNullable(p.getValue())));
  };
  public final static Function<Activation, Optional<ActivationProxy>> proxyFromActivation = (a1) -> {
    return ofNullable(a1).map(a -> new ActivationProxy(a.isActiveByDefault(), proxyFromActFile.apply(a.getFile()),
        Optional.ofNullable(a.getJdk()), proxyFromActOs.apply(a.getOs()), proxyFromActProperty.apply(a.getProperty())));
  };

  public final static Function<RepositoryPolicy, Optional<RepositoryPolicyProxy>> proxyFromRepoPolicy = (p1) -> {
    return Optional.ofNullable(p1).map(p -> {
      UpdatePolicy upd = ofNullable(p.getUpdatePolicy()).map(String::toUpperCase)
          // We have to check for "interval:xx" here
          .map(g -> g.contains(UpdatePolicy.INTERVAL.name() + ":") ? UpdatePolicy.INTERVAL : UpdatePolicy.valueOf(g))
          // Default is daily
          .orElse(UpdatePolicy.DAILY);
      return new RepositoryPolicyProxy(p.isEnabled(),
          // Checksum Policy
          ofNullable(p.getChecksumPolicy()).map(String::toUpperCase).map(ChecksumPolicy::valueOf)
              .orElse(ChecksumPolicy.WARN),
          // Update Policy
          upd,
          // Interval if available
          (upd == UpdatePolicy.INTERVAL && p.getUpdatePolicy().contains(":"))
              ? Integer.parseInt(p.getUpdatePolicy().split(":")[1])
              : 0
      // The end
      );
    });
  };
  public final static Function<Repository, RepositoryProxy> proxyFromRepo = (r) -> {
    return new RepositoryProxy(r.getId(),
        ofNullable(r.getLayout()).map(String::toUpperCase).map(Layout::valueOf).orElse(Layout.DEFAULT),
        ofNullable(r.getName()), IBException.cet.withReturningTranslation(() -> new URL(r.getUrl())),
        proxyFromRepoPolicy.apply(r.getReleases()), proxyFromRepoPolicy.apply(r.getSnapshots()));
  };
  public final static BiFunction<Boolean, Profile, ProfileProxy> proxyFromProfile = (active, p) -> {
    return new ProfileProxy(p.getId(), active,
        // Activation
        proxyFromActivation.apply(p.getActivation()),
        p.getPluginRepositories().stream().map(proxyFromRepo).collect(toList()), p.getProperties(),
        p.getRepositories().stream().map(proxyFromRepo).collect(toList()));
  };
  public final static Function<Mirror, MirrorProxy> proxyFromMirror = (m) -> {
    return new MirrorProxy(m.getId(),
        ofNullable(m.getLayout()).map(String::toUpperCase).map(Layout::valueOf).orElse(Layout.DEFAULT),
        Arrays.asList(m.getMirrorOf().split(",")),
        Arrays.asList(ofNullable(m.getLayout()).orElse(Layout.DEFAULT.name())).stream().map(String::toUpperCase)
            .map(Layout::valueOf).collect(toList()),
        ofNullable(m.getName()), IBException.cet.withReturningTranslation(() -> new URL(m.getUrl())));
  };
  public final static Function<Proxy, ProxyProxy> proxyFromProxy = (p) -> {
    return new ProxyProxy(p.getId(), p.getHost(),
        Arrays.asList((ofNullable(p.getNonProxyHosts()).orElse("")).split("|")).stream().collect(toList()),
        ofNullable(p.getPassword()), p.getPort(), ofNullable(p.getProtocol()).orElse("http"),
        ofNullable(p.getUsername()), p.isActive());
  };
  public final static Function<Settings, SettingsProxy> proxyFromSettings = (s) -> {
    return new SettingsProxy(s.isOffline(), Paths.get(s.getLocalRepository()),
        ofNullable(s.getModelEncoding()).map(Charset::forName).orElse(IBUtils.UTF_8),
        //Servers
        s.getServers().stream().map(proxyFromServer).collect(toList()),
        // Profiles
        s.getProfiles().stream().map(pp -> proxyFromProfile.apply(s.getActiveProfiles().contains(pp.getId()), pp))
            .collect(toList()),
        // Mirrors
        s.getMirrors().stream().map(proxyFromMirror).collect(toList()),
        // PluginGroups
        s.getPluginGroups().stream().collect(toList()),
        // Proxies
        s.getProxies().stream().map(proxyFromProxy).collect(toList()));
  };

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
      return logProblems.apply(settingsBuilder.build(settingsRequest));
    } catch (NullPointerException | SettingsBuildingException e) {
      throw new RuntimeException(e);
    }
  };

}
