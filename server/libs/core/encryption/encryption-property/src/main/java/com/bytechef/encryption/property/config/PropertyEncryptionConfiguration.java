/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.encryption.property.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.encryption.EncryptionKey;
import com.bytechef.encryption.property.PropertyEncryptionKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Caardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.encryption", name = "provider", havingValue = "property")
public class PropertyEncryptionConfiguration {

    private static final Logger log = LoggerFactory.getLogger(PropertyEncryptionConfiguration.class);

    public PropertyEncryptionConfiguration() {
        if (log.isInfoEnabled()) {
            log.info("Encryption provider type enabled: property");
        }
    }

    @Bean
    EncryptionKey encryptionKey(ApplicationProperties applicationProperties) {
        ApplicationProperties.Encryption encryption = applicationProperties.getEncryption();

        return new PropertyEncryptionKey(encryption.getProperty());
    }
}
