/*
 * Copyright (c) 2022 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.rbellogger.key;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import java.security.Key;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RbelKeyManagerTest {

    private RbelKeyManager keyManager = new RbelKeyManager();
    private Key mock = mock(Key.class);

    @BeforeEach
    public void initEach() {
        doReturn(new byte[]{}).when(mock).getEncoded();
    }

    @Test
    public void shouldFindPrivateKeyIfPresent() {
        RbelKey publicKey = RbelKey.builder()
            .keyName("publicKey")
            .key(mock)
            .build();
        keyManager.addKey(publicKey);
        RbelKey falsePrivateKey = RbelKey.builder()
            .matchingPublicKey(RbelKey.builder()
                .keyName("other publicKey")
                .key(mock)
                .build())
            .keyName("falsePrivateKey")
            .key(mock)
            .build();
        keyManager.addKey(falsePrivateKey);

        RbelKey privateKey = RbelKey.builder()
            .matchingPublicKey(publicKey)
            .keyName("privateKey")
            .key(mock)
            .build();
        keyManager.addKey(privateKey);

        assertThat(keyManager.findCorrespondingPrivateKey(publicKey.getKeyName()))
            .get()
            .isEqualTo(privateKey);
    }

    @Test
    public void shouldThrowExceptionWhenPrivateKeyNotPresent() {
        keyManager.getAllKeys().collect(Collectors.toList()).clear();

        RbelKey publicKey = RbelKey.builder()
            .keyName("publicKey")
            .key(mock)
            .build();
        keyManager.addKey(publicKey);

        assertThat(keyManager.findCorrespondingPrivateKey(publicKey.getKeyName()))
            .isEmpty();
    }
}
