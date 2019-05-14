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
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.DefaultSettingsReader;
import org.apache.maven.settings.io.SettingsReader;
import org.infrastructurebuilder.utils.settings.SettingsSupplier;

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
  public Settings get() {
    return s;
  }

}
