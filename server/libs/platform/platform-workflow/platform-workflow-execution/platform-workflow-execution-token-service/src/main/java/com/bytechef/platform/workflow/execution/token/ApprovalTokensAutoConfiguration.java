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

package com.bytechef.platform.workflow.execution.token;

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
 * Wires {@link ApprovalTokens} from {@code bytechef.approval.signed-token.*} configuration. Signing key resolution
 * order mirrors {@code FileEntryTokensAutoConfiguration}: explicit {@code secret} property, else derive from the
 * {@link EncryptionKey} bean via {@code HMAC-SHA256(decode(encryptionKey), DERIVATION_LABEL)} (independent from the AES
 * master key and from the file-storage signing key by domain separation), else unconfigured (legacy-only).
 *
 * @author Ivica Cardic
 */
@AutoConfiguration
@EnableConfigurationProperties(ApprovalTokensProperties.class)
public class ApprovalTokensAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ApprovalTokensAutoConfiguration.class);

    /**
     * Domain-separation label used when deriving the signing key from the EncryptionKey bean. The {@code -v1} suffix
     * allows rolling forward to a new derivation scheme without rotating the encryption key.
     */
    static final String DERIVATION_LABEL = "bytechef-approval-token-signed-v1";

    @Bean
    @ConditionalOnMissingBean
    public ApprovalTokens approvalTokens(
        ApprovalTokensProperties properties, ObjectProvider<Clock> clockProvider,
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
                "bytechef.approval.signed-token.required=true but no signing secret is available. Either set "
                    + "bytechef.approval.signed-token.secret directly or ensure an EncryptionKey bean is on the "
                    + "classpath.");
        }

        if (effectiveSecret == null || effectiveSecret.isBlank()) {
            log.warn(
                "No signing secret resolved for approval/resume links. Set bytechef.approval.signed-token.secret or "
                    + "provide an EncryptionKey bean to enable signed tokens.");
        }

        Clock clock = clockProvider.getIfAvailable(Clock::systemUTC);

        return new ApprovalTokensImpl(
            clock, effectiveSecret, properties.getPreviousSecrets(), properties.getDefaultTtl(),
            properties.getClockSkew(), properties.isRequired());
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
            throw new IllegalStateException("Failed to derive approval token signing key from EncryptionKey", e);
        }
    }
}
