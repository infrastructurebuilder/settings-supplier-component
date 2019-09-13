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

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.settings.building.DefaultSettingsBuilder;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.io.SettingsReader;
import org.apache.maven.settings.io.SettingsWriter;
import org.apache.maven.settings.validation.SettingsValidator;

@Named("my")
final class MyDefaultSettingsBuilder extends DefaultSettingsBuilder implements SettingsBuilder {

  @Inject
  public MyDefaultSettingsBuilder(SettingsReader settingsReader, SettingsWriter settingsWriter,
      SettingsValidator settingsValidator) {
    super(settingsReader, settingsWriter, settingsValidator);
  }

}
