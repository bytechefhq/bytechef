/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.oauth2.authorizationserver.config;

import com.bytechef.encryption.Encryption;
import com.bytechef.encryption.EncryptionImpl;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Supplies a real {@link Encryption} with a fixed test key for the persisted JWK signing key.
 *
 * @author Ivica Cardic
 */
@TestConfiguration(proxyBeanMethods = false)
public class Oauth2AuthorizationServerEncryptionIntTestConfiguration {

    @Bean
    Encryption encryption() {
        return new EncryptionImpl(() -> "tTB1/UBIbYLuCXVi4PPfzA==");
    }
}
