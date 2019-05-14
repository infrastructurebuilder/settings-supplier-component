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

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.infrastructurebuilder.utils.settings.HandCraftedEnvSupplier;
import org.junit.Before;
import org.junit.Test;

public class HandCraftedEnvSupplierTest {

  private HandCraftedEnvSupplier h;

  @Before
  public void setUp() throws Exception {
    h = new HandCraftedEnvSupplier();
  }

  @Test
  public void test() {
    assertNotNull(h);
    h.set("a", "c");
    Map<String, String> m = h.get();
    assertEquals("c", m.get("a"));
  }

  @Test
  public void testLoad() throws IOException {
    try (InputStream k = getClass().getResourceAsStream("/test1.properties")) {
      h = new HandCraftedEnvSupplier(k);
    }
    Map<String, String> m = h.get();
    assertEquals(2,m.size());
    assertEquals("b", m.get("a"));
    assertEquals("e", m.get("d"));

  }

}
