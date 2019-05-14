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

import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.PasswordDecryptor;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

@Named("maven")
public final class MyDefaultSecDispatcher extends DefaultSecDispatcher implements SecDispatcher {
  @Inject
  public MyDefaultSecDispatcher(@Named("my") PlexusCipher myCipher, Map<String, PasswordDecryptor> myDecrypters) {
    super();
    _cipher = Objects.requireNonNull(myCipher);
    _decryptors = myDecrypters;
  }

}
