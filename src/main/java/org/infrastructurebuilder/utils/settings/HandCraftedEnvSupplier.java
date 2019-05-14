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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

public class HandCraftedEnvSupplier implements EnvSupplier {

  private Map<String, String> map;

  public HandCraftedEnvSupplier() {
    this(new HashMap<String, String>());
  }

  public HandCraftedEnvSupplier(Map<String, String> map) {
    this.map = new HashMap<>();
    this.map.putAll(Objects.requireNonNull(map));
  }

  public HandCraftedEnvSupplier(InputStream props) throws IOException {
    this();
    Properties p = new Properties();

      p.load(Objects.requireNonNull(props));
    for (String name : p.keySet().stream().map(Object::toString).collect(Collectors.toSet())) {
      this.map.put(name, p.getProperty(name));
    }
  }

  public synchronized HandCraftedEnvSupplier set(String key, String val) {
    this.map.put(Objects.requireNonNull(key), Objects.requireNonNull(val));
    return this;
  }

  public synchronized HandCraftedEnvSupplier setAll(Map<String,String> env) {
    this.map.putAll(Objects.requireNonNull(env));
    return this;
  }

  @Override
  public Map<String, String> get() {
    return new HashMap<>(this.map);
  }

}
