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

package com.bytechef.file.storage.token;

import com.bytechef.encryption.EncryptionKey;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Wires {@link FileEntryTokens} from {@code bytechef.file-storage.signed-url.*} configuration.
 *
 * <p>
 * Signing key resolution order (highest to lowest priority):
 * <ol>
 * <li>Explicit {@code bytechef.file-storage.signed-url.secret} property — used as-is. Lets operators rotate the signing
 * key independently of the encryption key.</li>
 * <li>{@link EncryptionKey} bean available — the signing key is derived via
 * {@code HMAC-SHA256(decode(encryptionKey.getKey()), DERIVATION_LABEL)} and base64-encoded. The domain-separation label
 * {@value #DERIVATION_LABEL} ensures the derived key is mathematically independent from the AES master key, even though
 * both originate from the same secret bytes.</li>
 * <li>Neither present — unconfigured mode. The mint path throws on call; the verify path only accepts legacy unsigned
 * tokens.</li>
 * </ol>
 *
 * <p>
 * Fails fast when {@code required=true} and no secret can be resolved. Logs a single WARN at startup when
 * {@code required=false} and no secret is available (legacy-only mode).
 *
 * @author Ivica Cardic
 */
@AutoConfiguration
@EnableConfigurationProperties(FileEntryTokensProperties.class)
public class FileEntryTokensAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(FileEntryTokensAutoConfiguration.class);

    /**
     * Domain-separation label used when deriving the signing key from the EncryptionKey bean. The {@code -v1} suffix
     * allows rolling forward to a new derivation scheme in the future without rotating the encryption key.
     */
    static final String DERIVATION_LABEL = "bytechef-file-storage-signed-url-v1";

    @Bean
    @ConditionalOnMissingBean
    public FileEntryTokens fileEntryTokens(
        FileEntryTokensProperties properties, ObjectProvider<Clock> clockProvider,
        ObjectProvider<EncryptionKey> encryptionKeyProvider) {

        String explicitSecret = properties.getSecret();
        String effectiveSecret;

        if (explicitSecret != null && !explicitSecret.isBlank()) {
            effectiveSecret = explicitSecret;
        } else {
            EncryptionKey encryptionKey = encryptionKeyProvider.getIfAvailable();

            if (encryptionKey != null) {
                effectiveSecret = deriveSecretFromEncryptionKey(encryptionKey);
            } else {
                effectiveSecret = null;
            }
        }

        if (properties.isRequired() && (effectiveSecret == null || effectiveSecret.isBlank())) {
            throw new IllegalStateException(
                "bytechef.file-storage.signed-url.required=true but no signing secret is available. "
                    + "Either set bytechef.file-storage.signed-url.secret directly or ensure an EncryptionKey bean is "
                    + "on the classpath.");
        }

        if (effectiveSecret == null || effectiveSecret.isBlank()) {
            log.warn(
                "No signing secret resolved for public /file-entries URLs. Set bytechef.file-storage.signed-url.secret "
                    + "or provide an EncryptionKey bean to enable signed tokens.");
        }

        Clock clock = clockProvider.getIfAvailable(Clock::systemUTC);

        return new FileEntryTokensImpl(
            clock, effectiveSecret, properties.getPreviousSecrets(), properties.getDefaultTtl(),
            properties.getClockSkew());
    }

    private static String deriveSecretFromEncryptionKey(EncryptionKey encryptionKey) {
        try {
            byte[] masterKey = Base64.getDecoder()
                .decode(encryptionKey.getKey()
                    .trim());

            Mac mac = Mac.getInstance("HmacSHA256");

            mac.init(new SecretKeySpec(masterKey, "HmacSHA256"));

            byte[] derived = mac.doFinal(DERIVATION_LABEL.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder()
                .encodeToString(derived);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException(
                "Failed to derive file-storage signing key from EncryptionKey", e);
        }
    }
}
