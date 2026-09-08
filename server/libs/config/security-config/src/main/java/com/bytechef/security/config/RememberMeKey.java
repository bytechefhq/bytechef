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

package com.bytechef.security.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Security;
import com.bytechef.config.ApplicationProperties.Security.RememberMe;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Signs the remember-me cookie. A configured key wins; otherwise one is generated on first start and kept in the
 * ByteChef home directory, so sessions survive a restart without the key having to be supplied.
 *
 * @author Ivica Cardic
 */
@Component
public class RememberMeKey {

    private static final String KEY_FILE_NAME = "remember-me-key";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final String key;

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public RememberMeKey(ApplicationProperties applicationProperties) {
        Security security = applicationProperties.getSecurity();

        RememberMe rememberMe = security.getRememberMe();

        Path bytechefPath = Path.of(System.getProperty("user.home"), ".bytechef");

        key = resolveKey(rememberMe.getKey(), bytechefPath);
    }

    public String getKey() {
        return key;
    }

    static String resolveKey(String configuredKey, Path bytechefPath) {
        if (StringUtils.isNotBlank(configuredKey)) {
            return configuredKey;
        }

        try {
            Files.createDirectories(bytechefPath);

            Path keyPath = bytechefPath.resolve(KEY_FILE_NAME);

            if (Files.exists(keyPath)) {
                String persistedKey = Files.readString(keyPath);

                persistedKey = persistedKey.strip();

                if (StringUtils.isNotBlank(persistedKey)) {
                    return persistedKey;
                }
            }

            String generatedKey = generateKey();

            Files.writeString(keyPath, generatedKey);

            return generatedKey;
        } catch (IOException ioException) {
            throw new UncheckedIOException(ioException);
        }
    }

    private static String generateKey() {
        byte[] bytes = new byte[32];

        SECURE_RANDOM.nextBytes(bytes);

        Base64.Encoder encoder = Base64.getEncoder();

        return encoder.encodeToString(bytes);
    }
}
