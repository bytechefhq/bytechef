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

import static com.bytechef.file.storage.token.FileEntryTokensAutoConfiguration.DERIVATION_LABEL;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.encryption.EncryptionKey;
import com.bytechef.file.storage.domain.FileEntry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SuppressFBWarnings("HARD_CODE_PASSWORD")
public class FileEntryTokensAutoConfigurationTest {

    private static final String EXPLICIT_SECRET = "dGVzdC1zZWNyZXQtMzItYnl0ZXMtYmFzZTY0LWVuY29kZWQ=";

    /**
     * A known base64-encoded key that the FakeEncryptionKeyConfig bean returns. Must be valid base64 so that the HMAC
     * derivation does not throw.
     */
    private static final String ENCRYPTION_KEY_BASE64 = "dGVzdC1lbmNyeXB0aW9uLWtleS1mb3ItZGVyaXZhdGlvbg==";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(FileEntryTokensAutoConfiguration.class));

    @Test
    public void testBeanCreatedWithExplicitSecret() {
        runner.withPropertyValues("bytechef.file-storage.signed-url.secret=" + EXPLICIT_SECRET)
            .run(context -> {
                assertThat(context).hasSingleBean(FileEntryTokens.class);

                FileEntryTokens tokens = context.getBean(FileEntryTokens.class);
                FileEntry sample = new FileEntry(
                    "report.txt", "txt", "text/plain", "file:/temp/uuid.txt");

                String token = tokens.toSignedToken(sample);

                assertThat(tokens.parseSignedToken(token)).isPresent();
            });
    }

    @Test
    public void testBeanCreatedWithoutSecretInPermissiveMode() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(FileEntryTokens.class);

            FileEntryTokens tokens = context.getBean(FileEntryTokens.class);

            assertThat(tokens.parseSignedToken("v1.123.x.y")).isEmpty();
        });
    }

    @Test
    public void testFailFastWhenRequiredButNoSecret() {
        runner.withPropertyValues("bytechef.file-storage.signed-url.required=true")
            .run(context -> assertThat(context).hasFailed()
                .getFailure()
                .rootCause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no signing secret is available"));
    }

    @Test
    public void testDerivesSecretFromEncryptionKeyWhenExplicitNotSet() {
        runner.withUserConfiguration(FakeEncryptionKeyConfig.class)
            .run(context -> {
                assertThat(context).hasSingleBean(FileEntryTokens.class);

                FileEntryTokens tokens = context.getBean(FileEntryTokens.class);
                FileEntry sample = new FileEntry(
                    "report.txt", "txt", "text/plain", "file:/temp/uuid-derived.txt");

                // toSignedTokenIfConfigured returns present only when a secret is configured — either
                // explicit or derived. An empty Optional here would mean derivation did not happen.
                assertThat(tokens.toSignedTokenIfConfigured(sample)).isPresent();

                // Verify round-trip to confirm the token was signed with the derived key.
                String token = tokens.toSignedToken(sample);

                assertThat(tokens.parseSignedToken(token)).isPresent();
            });
    }

    @Test
    public void testExplicitSecretOverridesEncryptionKeyDerivation() throws NoSuchAlgorithmException,
        InvalidKeyException {

        runner
            .withPropertyValues("bytechef.file-storage.signed-url.secret=" + EXPLICIT_SECRET)
            .withUserConfiguration(FakeEncryptionKeyConfig.class)
            .run(context -> {
                assertThat(context).hasSingleBean(FileEntryTokens.class);

                FileEntryTokens tokens = context.getBean(FileEntryTokens.class);
                FileEntry sample = new FileEntry(
                    "report.txt", "txt", "text/plain", "file:/temp/uuid-override.txt");

                String token = tokens.toSignedToken(sample);

                // Verify with a separately-constructed impl using the explicit secret — must succeed.
                FileEntryTokensImpl explicitVerifier = new FileEntryTokensImpl(
                    Clock.systemUTC(), EXPLICIT_SECRET, List.of(), Duration.ofHours(24),
                    Duration.ofSeconds(60));

                assertThat(explicitVerifier.parseSignedToken(token)).isPresent();

                // Verify with a separately-constructed impl using the derived-from-encryption-key secret —
                // must fail, confirming the explicit secret took precedence over derivation.
                String derivedSecret = computeDerivedSecret(ENCRYPTION_KEY_BASE64);

                FileEntryTokensImpl derivedVerifier = new FileEntryTokensImpl(
                    Clock.systemUTC(), derivedSecret, List.of(), Duration.ofHours(24),
                    Duration.ofSeconds(60));

                assertThat(derivedVerifier.parseSignedToken(token)).isEmpty();
            });
    }

    @Test
    public void testFailFastWhenRequiredAndNoSecretEvenWithoutEncryptionKey() {
        // Equivalent to testFailFastWhenRequiredButNoSecret — made explicit to document that
        // required=true with neither explicit secret nor EncryptionKey bean triggers fail-fast.
        runner.withPropertyValues("bytechef.file-storage.signed-url.required=true")
            .run(context -> assertThat(context).hasFailed()
                .getFailure()
                .rootCause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no signing secret is available"));
    }

    // --- helpers ---

    private static String computeDerivedSecret(String encryptionKeyBase64)
        throws NoSuchAlgorithmException, InvalidKeyException {

        byte[] masterKey = Base64.getDecoder()
            .decode(encryptionKeyBase64.trim());

        Mac mac = Mac.getInstance("HmacSHA256");

        mac.init(new SecretKeySpec(masterKey, "HmacSHA256"));

        byte[] derived = mac.doFinal(DERIVATION_LABEL.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder()
            .encodeToString(derived);
    }

    @Configuration
    static class FakeEncryptionKeyConfig {

        @Bean
        public EncryptionKey encryptionKey() {
            return () -> ENCRYPTION_KEY_BASE64;
        }
    }
}
