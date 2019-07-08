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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.DefaultSettingsReader;
import org.apache.maven.settings.io.SettingsReader;
import org.infrastructurebuilder.util.IBUtils;
import org.infrastructurebuilder.util.MirrorProxy;
import org.infrastructurebuilder.util.ProfileProxy;
import org.infrastructurebuilder.util.ProxyProxy;
import org.infrastructurebuilder.util.ServerProxy;
import org.infrastructurebuilder.util.SettingsProxy;
import org.infrastructurebuilder.util.SettingsSupplier;

public class LocalModifiableSettingsSupplier implements SettingsSupplier {

  SettingsReader sr = new DefaultSettingsReader();

  private final Settings s;

  public LocalModifiableSettingsSupplier(String settingsResource) {
    try (InputStream ins = getClass().getResourceAsStream(settingsResource)) {
      this.s = sr.read(ins, null);
    } catch (IOException e) {
      throw new RuntimeException("Reading " + settingsResource, e);
    }
  }

  @Override
  public SettingsProxy get() {
    List<ProxyProxy> proxies = Collections.emptyList();
    List<String> pluginGroups= Collections.emptyList();
    List<MirrorProxy> mirrors= Collections.emptyList();
    List<ProfileProxy> profiles= Collections.emptyList();
    List<ServerProxy> servers= Collections.emptyList();
    Charset modelEncoding  = IBUtils.UTF_8;
    Path localRepo = Paths.get("target");
    return new SettingsProxy(false, localRepo, modelEncoding, servers, profiles, mirrors, pluginGroups, proxies);
  }

}
