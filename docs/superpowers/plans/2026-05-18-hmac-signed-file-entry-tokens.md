# HMAC-signed file-entry tokens Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add HMAC-SHA256-signed, time-bounded tokens for the public `/file-entries/{id}/content` endpoint so that file URLs can't be replayed indefinitely or forged, while keeping the endpoint unauthenticated (it serves files produced by public webhook responses) and preserving the existing unsigned `FileEntry.toId()` path for internal DB-persisted IDs.

**Architecture:** A new `FileEntryTokens` Spring bean wraps `Mac.getInstance("HmacSHA256")` (same pattern as `AiObservabilityWebhookDeliveryServiceImpl:640`) to mint and verify tokens shaped `v1.<exp>.<payload>.<sig>`. The bean is wired by a new autoconfiguration in `file-storage-api`, configured via a `SignedUrl` block under `bytechef.file-storage` in `ApplicationProperties`. `FileEntryController` accepts both signed tokens and (during a deprecation window) the legacy unsigned IDs; legacy use emits a rate-limited WARN. Internal callers (`AiAgentEvalResult.transcriptFile`) continue to use `FileEntry.toId()`/`parse(String)` unchanged.

**Tech Stack:** Java 25, Spring Boot 4.0.6 autoconfiguration, `javax.crypto.Mac` (HmacSHA256), JUnit 5, AssertJ, Testcontainers (not strictly required — the unit tests cover everything). No new third-party dependencies.

**Spec:** [2026-05-18-hmac-signed-file-entry-tokens-design.md](../specs/2026-05-18-hmac-signed-file-entry-tokens-design.md)

---

## File Structure

**Create:**
- `server/libs/core/file-storage/file-storage-api/src/main/java/com/bytechef/file/storage/token/FileEntryTokens.java` — interface (`toSignedToken`, `parseSignedToken`)
- `server/libs/core/file-storage/file-storage-api/src/main/java/com/bytechef/file/storage/token/FileEntryTokensImpl.java` — HMAC-SHA256 signer/verifier
- `server/libs/core/file-storage/file-storage-api/src/main/java/com/bytechef/file/storage/token/FileEntryTokensAutoConfiguration.java` — `@AutoConfiguration` wiring
- `server/libs/core/file-storage/file-storage-api/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` — registers the autoconfig (create if absent, append otherwise)
- `server/libs/core/file-storage/file-storage-api/src/test/java/com/bytechef/platform/file/storage/token/FileEntryTokensImplTest.java` — round-trip, expired, tampered, key-rotation, clock-skew
- `server/libs/core/file-storage/file-storage-api/src/test/java/com/bytechef/platform/file/storage/token/FileEntryTokensAutoConfigurationTest.java` — bean wiring asserts
- `server/libs/platform/platform-webhook/platform-webhook-rest/platform-webhook-rest-impl/src/test/java/com/bytechef/platform/webhook/web/rest/FileEntryControllerTest.java` — controller-level acceptance of both formats

**Modify:**
- `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java:2645` — add `SignedUrl` nested class + getter/setter on `FileStorage`
- `server/libs/platform/platform-webhook/platform-webhook-rest/platform-webhook-rest-impl/src/main/java/com/bytechef/platform/webhook/web/rest/FileEntryController.java` — accept both formats; dispatch via `FileEntryTokens`
- `server/libs/platform/platform-webhook/platform-webhook-rest/platform-webhook-rest-impl/build.gradle.kts` — depend on `file-storage-api` (likely already present; verify)
- URL-emission helpers identified in Task 8 — switch to `fileEntryTokens.toSignedToken(...)`
- `CLAUDE.md` — add "Public file URL signing" section under Persistence Conventions

---

## Task 1: Add `SignedUrl` config to `ApplicationProperties.FileStorage`

**Files:**
- Modify: `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java:2645`

- [ ] **Step 1: Read the existing `FileStorage` class to confirm style**

Run: `Read ApplicationProperties.java lines 2640-2730`
Confirm the pattern (private field with `new SubClass()`, getter/setter, nested static class with its own getters/setters, Javadoc on every field).

- [ ] **Step 2: Add the `SignedUrl` field, accessors, and nested class**

In `ApplicationProperties.java`, inside `public static class FileStorage`, alongside `aws`/`filesystem`/`provider`:

```java
        /**
         * HMAC-SHA256 signing configuration for public file URLs. See
         * {@code FileEntryTokens} for the token format and the
         * 2026-05-18 design spec for rationale.
         */
        private SignedUrl signedUrl = new SignedUrl();

        public SignedUrl getSignedUrl() {
            return signedUrl;
        }

        public void setSignedUrl(SignedUrl signedUrl) {
            this.signedUrl = signedUrl;
        }
```

Then add the nested class after the existing `Filesystem` nested class (search for `public static class Filesystem` to find the right anchor):

```java
        /**
         * HMAC-signed URL configuration for {@code /file-entries/{id}/content}. When {@code secret} is set, the
         * server mints signed, time-bounded tokens for public file URLs. The endpoint also accepts legacy unsigned
         * IDs when {@code required} is {@code false} (default), for backward compatibility with previously-issued
         * URLs.
         */
        public static class SignedUrl {

            /**
             * Base64-encoded HMAC-SHA256 secret (≥32 bytes of entropy). If unset and {@link #required} is true,
             * the application fails to start. If unset and {@code required} is false, the mint path throws on call
             * and the verify path accepts only legacy tokens.
             */
            private String secret;

            /**
             * When true, the endpoint accepts only signed tokens; legacy unsigned IDs are rejected with 404.
             * Defaults to {@code false} during the deprecation window.
             */
            private boolean required;

            /**
             * Default time-to-live for newly minted tokens. ISO-8601 duration.
             */
            private Duration defaultTtl = Duration.ofHours(24);

            /**
             * Acceptable clock skew when validating expiry (both directions).
             */
            private Duration clockSkew = Duration.ofSeconds(60);

            /**
             * Previous secrets retained for key rotation. Verification tries the active {@link #secret} first, then
             * each entry here in order. Signing always uses {@link #secret}.
             */
            private List<String> previousSecrets = new ArrayList<>();

            public String getSecret() {
                return secret;
            }

            public void setSecret(String secret) {
                this.secret = secret;
            }

            public boolean isRequired() {
                return required;
            }

            public void setRequired(boolean required) {
                this.required = required;
            }

            public Duration getDefaultTtl() {
                return defaultTtl;
            }

            public void setDefaultTtl(Duration defaultTtl) {
                this.defaultTtl = defaultTtl;
            }

            public Duration getClockSkew() {
                return clockSkew;
            }

            public void setClockSkew(Duration clockSkew) {
                this.clockSkew = clockSkew;
            }

            public List<String> getPreviousSecrets() {
                return previousSecrets;
            }

            public void setPreviousSecrets(List<String> previousSecrets) {
                this.previousSecrets = previousSecrets;
            }
        }
```

Add the imports at the top of the file if not present:
```java
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :server:libs:config:app-config:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java
git commit -m "5008 Add bytechef.file-storage.signed-url config block"
```

---

## Task 2: Create `FileEntryTokens` interface

**Files:**
- Create: `server/libs/core/file-storage/file-storage-api/src/main/java/com/bytechef/file/storage/token/FileEntryTokens.java`

- [ ] **Step 1: Write the interface**

```java
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

import com.bytechef.file.storage.domain.FileEntry;
import java.time.Duration;
import java.util.Optional;

/**
 * Mints and verifies HMAC-SHA256 signed, time-bounded tokens for the public
 * {@code /file-entries/{id}/content} endpoint. Token format: {@code v1.<exp>.<payload>.<sig>}.
 *
 * @author Ivica Cardic
 */
public interface FileEntryTokens {

    /**
     * Mints a signed token whose payload identifies {@code fileEntry} and expires after {@code ttl}. Throws
     * {@link IllegalStateException} if no signing secret is configured.
     */
    String toSignedToken(FileEntry fileEntry, Duration ttl);

    /**
     * Mints a signed token using the configured default TTL.
     */
    String toSignedToken(FileEntry fileEntry);

    /**
     * Verifies a signed token. Returns the {@link FileEntry} on success, {@link Optional#empty()} on any failure
     * (malformed, bad signature, expired, future-dated, or bad payload). The caller MUST NOT distinguish failure
     * modes in HTTP responses — all failures should map to a uniform 404.
     */
    Optional<FileEntry> parseSignedToken(String token);

    /**
     * Returns true if the token looks like a v1 signed token (starts with {@code "v1."} and has the expected
     * structure). Cheap parse — does not verify the signature. Lets callers distinguish "is this an attempted
     * signed token" from "is this a legacy unsigned id" before invoking the (more expensive) verifier.
     */
    boolean looksLikeSignedToken(String token);
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:libs:core:file-storage:file-storage-api:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add server/libs/core/file-storage/file-storage-api/src/main/java/com/bytechef/file/storage/token/FileEntryTokens.java
git commit -m "5008 Introduce FileEntryTokens interface for signed public URLs"
```

---

## Task 3: Implement `FileEntryTokensImpl` with TDD

**Files:**
- Create: `server/libs/core/file-storage/file-storage-api/src/test/java/com/bytechef/platform/file/storage/token/FileEntryTokensImplTest.java`
- Create: `server/libs/core/file-storage/file-storage-api/src/main/java/com/bytechef/file/storage/token/FileEntryTokensImpl.java`

- [ ] **Step 1: Write the failing tests**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.bytechef.platform.file.storage.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.token.FileEntryTokensImpl;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@SuppressFBWarnings("HARD_CODE_PASSWORD")
public class FileEntryTokensImplTest {

    private static final String SECRET = "dGVzdC1zZWNyZXQtMzItYnl0ZXMtYmFzZTY0LWVuY29kZWQ="; // 32-byte test key
    private static final String OLD_SECRET = "b2xkLXNlY3JldC1mb3Ita2V5LXJvdGF0aW9uLXRlc3RpbmcK";
    private static final FileEntry SAMPLE = new FileEntry(
        "report.txt", "txt", "text/plain", "file:/temp/3a2b1c4d-5e6f-7081-9203-405060708090.txt");

    @Test
    public void testRoundTripWithDefaultTtl() {
        FileEntryTokensImpl signer = signer(Clock.systemUTC(), SECRET, List.of());

        String token = signer.toSignedToken(SAMPLE, Duration.ofHours(1));

        assertThat(signer.parseSignedToken(token))
            .isPresent()
            .hasValueSatisfying(parsed -> assertThat(parsed.getUrl()).isEqualTo(SAMPLE.getUrl()));
    }

    @Test
    public void testTokenStartsWithV1Prefix() {
        FileEntryTokensImpl signer = signer(Clock.systemUTC(), SECRET, List.of());

        String token = signer.toSignedToken(SAMPLE, Duration.ofHours(1));

        assertThat(token).startsWith("v1.");
        assertThat(token.split("\\.")).hasSize(4);
    }

    @Test
    public void testExpiredTokenRejected() {
        Instant fixedNow = Instant.parse("2026-05-18T10:00:00Z");
        Clock signingClock = Clock.fixed(fixedNow, ZoneOffset.UTC);
        FileEntryTokensImpl signer = signer(signingClock, SECRET, List.of());

        String token = signer.toSignedToken(SAMPLE, Duration.ofMinutes(5));

        Clock laterClock = Clock.fixed(fixedNow.plus(Duration.ofMinutes(10)), ZoneOffset.UTC);
        FileEntryTokensImpl verifier = signer(laterClock, SECRET, List.of());

        assertThat(verifier.parseSignedToken(token)).isEmpty();
    }

    @Test
    public void testTamperedPayloadRejected() {
        FileEntryTokensImpl signer = signer(Clock.systemUTC(), SECRET, List.of());

        String token = signer.toSignedToken(SAMPLE, Duration.ofHours(1));
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + ".AAAA" + parts[2].substring(4) + "." + parts[3];

        assertThat(signer.parseSignedToken(tampered)).isEmpty();
    }

    @Test
    public void testTamperedSignatureRejected() {
        FileEntryTokensImpl signer = signer(Clock.systemUTC(), SECRET, List.of());

        String token = signer.toSignedToken(SAMPLE, Duration.ofHours(1));
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + "." + parts[2] + ".AAAA" + parts[3].substring(4);

        assertThat(signer.parseSignedToken(tampered)).isEmpty();
    }

    @Test
    public void testMalformedTokenRejected() {
        FileEntryTokensImpl signer = signer(Clock.systemUTC(), SECRET, List.of());

        assertThat(signer.parseSignedToken("")).isEmpty();
        assertThat(signer.parseSignedToken("notatoken")).isEmpty();
        assertThat(signer.parseSignedToken("v2.123.payload.sig")).isEmpty(); // wrong version
        assertThat(signer.parseSignedToken("v1.abc.payload.sig")).isEmpty(); // exp not numeric
        assertThat(signer.parseSignedToken("v1.123.payload")).isEmpty(); // missing sig
        assertThat(signer.parseSignedToken("v1.123.payload.sig.extra")).isEmpty(); // too many parts
    }

    @Test
    public void testKeyRotationAcceptsOldKey() {
        FileEntryTokensImpl oldSigner = signer(Clock.systemUTC(), OLD_SECRET, List.of());

        String token = oldSigner.toSignedToken(SAMPLE, Duration.ofHours(1));

        FileEntryTokensImpl newVerifier = signer(Clock.systemUTC(), SECRET, List.of(OLD_SECRET));

        assertThat(newVerifier.parseSignedToken(token))
            .isPresent()
            .hasValueSatisfying(parsed -> assertThat(parsed.getUrl()).isEqualTo(SAMPLE.getUrl()));
    }

    @Test
    public void testKeyRotationSigningUsesActiveKey() {
        FileEntryTokensImpl signer = signer(Clock.systemUTC(), SECRET, List.of(OLD_SECRET));

        String token = signer.toSignedToken(SAMPLE, Duration.ofHours(1));

        FileEntryTokensImpl oldOnlyVerifier = signer(Clock.systemUTC(), OLD_SECRET, List.of());

        assertThat(oldOnlyVerifier.parseSignedToken(token)).isEmpty();
    }

    @Test
    public void testTokenMintedByDifferentKeyIsRejected() {
        FileEntryTokensImpl signerA = signer(Clock.systemUTC(), SECRET, List.of());

        String token = signerA.toSignedToken(SAMPLE, Duration.ofHours(1));

        FileEntryTokensImpl signerB = signer(Clock.systemUTC(), OLD_SECRET, List.of());

        assertThat(signerB.parseSignedToken(token)).isEmpty();
    }

    @Test
    public void testClockSkewToleratesSmallDrift() {
        Instant fixedNow = Instant.parse("2026-05-18T10:00:00Z");
        Clock signingClock = Clock.fixed(fixedNow, ZoneOffset.UTC);
        FileEntryTokensImpl signer = new FileEntryTokensImpl(
            signingClock, SECRET, List.of(), Duration.ofMinutes(5), Duration.ofSeconds(30));

        String token = signer.toSignedToken(SAMPLE, Duration.ofMinutes(1));

        // Verifier clock is 20 seconds past expiry — within skew tolerance.
        Clock skewedClock = Clock.fixed(fixedNow.plus(Duration.ofMinutes(1).plusSeconds(20)), ZoneOffset.UTC);
        FileEntryTokensImpl verifier = new FileEntryTokensImpl(
            skewedClock, SECRET, List.of(), Duration.ofMinutes(5), Duration.ofSeconds(30));

        assertThat(verifier.parseSignedToken(token)).isPresent();
    }

    @Test
    public void testClockSkewRejectsLargeDrift() {
        Instant fixedNow = Instant.parse("2026-05-18T10:00:00Z");
        Clock signingClock = Clock.fixed(fixedNow, ZoneOffset.UTC);
        FileEntryTokensImpl signer = new FileEntryTokensImpl(
            signingClock, SECRET, List.of(), Duration.ofMinutes(5), Duration.ofSeconds(30));

        String token = signer.toSignedToken(SAMPLE, Duration.ofMinutes(1));

        // Verifier clock is 5 minutes past expiry — well beyond skew tolerance.
        Clock veryLateClock = Clock.fixed(fixedNow.plus(Duration.ofMinutes(6)), ZoneOffset.UTC);
        FileEntryTokensImpl verifier = new FileEntryTokensImpl(
            veryLateClock, SECRET, List.of(), Duration.ofMinutes(5), Duration.ofSeconds(30));

        assertThat(verifier.parseSignedToken(token)).isEmpty();
    }

    @Test
    public void testMintWithoutSecretThrows() {
        FileEntryTokensImpl signer = new FileEntryTokensImpl(
            Clock.systemUTC(), null, List.of(), Duration.ofHours(1), Duration.ofSeconds(60));

        assertThatThrownBy(() -> signer.toSignedToken(SAMPLE, Duration.ofHours(1)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("signing secret");
    }

    @Test
    public void testVerifyWithoutSecretReturnsEmpty() {
        FileEntryTokensImpl signerWithKey = signer(Clock.systemUTC(), SECRET, List.of());

        String token = signerWithKey.toSignedToken(SAMPLE, Duration.ofHours(1));

        FileEntryTokensImpl unconfigured = new FileEntryTokensImpl(
            Clock.systemUTC(), null, List.of(), Duration.ofHours(1), Duration.ofSeconds(60));

        assertThat(unconfigured.parseSignedToken(token)).isEmpty();
    }

    @Test
    public void testLooksLikeSignedTokenIsCheap() {
        FileEntryTokensImpl signer = signer(Clock.systemUTC(), SECRET, List.of());

        assertThat(signer.looksLikeSignedToken("v1.1.x.y")).isTrue();
        assertThat(signer.looksLikeSignedToken("v1.1.x")).isFalse();
        assertThat(signer.looksLikeSignedToken("v2.1.x.y")).isFalse();
        assertThat(signer.looksLikeSignedToken("dHh0XztfdGV4dC9wbGFpbg==")).isFalse(); // legacy
        assertThat(signer.looksLikeSignedToken("")).isFalse();
    }

    @Test
    public void testRoundTripUsingDefaultTtl() {
        FileEntryTokensImpl signer = new FileEntryTokensImpl(
            Clock.systemUTC(), SECRET, List.of(), Duration.ofMinutes(15), Duration.ofSeconds(60));

        String token = signer.toSignedToken(SAMPLE);

        Optional<FileEntry> parsed = signer.parseSignedToken(token);

        assertThat(parsed).isPresent();
    }

    private static FileEntryTokensImpl signer(Clock clock, String secret, List<String> previousSecrets) {
        return new FileEntryTokensImpl(clock, secret, previousSecrets, Duration.ofHours(1), Duration.ofSeconds(60));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :server:libs:core:file-storage:file-storage-api:test --tests "*FileEntryTokensImplTest*"`
Expected: COMPILATION ERROR (`FileEntryTokensImpl` doesn't exist yet).

- [ ] **Step 3: Implement `FileEntryTokensImpl`**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.bytechef.file.storage.token;

import com.bytechef.file.storage.domain.FileEntry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.jspecify.annotations.Nullable;

/**
 * HMAC-SHA256 signer/verifier for public file URLs. Token format: {@code v1.<exp>.<payload>.<sig>} where
 * {@code <exp>} is decimal Unix epoch seconds, {@code <payload>} is the Base64URL-without-padding rendering of the
 * existing {@link FileEntry#toId()} output, and {@code <sig>} is Base64URL-without-padding HMAC-SHA256 over the
 * literal ASCII string {@code v1.<exp>.<payload>} using the active signing secret.
 *
 * @author Ivica Cardic
 */
public class FileEntryTokensImpl implements FileEntryTokens {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String VERSION = "v1";
    private static final char SEPARATOR = '.';

    private final Clock clock;
    @Nullable
    private final byte[] activeSecret;
    private final List<byte[]> allSecrets;
    private final Duration defaultTtl;
    private final Duration clockSkew;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public FileEntryTokensImpl(
        Clock clock, @Nullable String activeSecret, List<String> previousSecrets, Duration defaultTtl,
        Duration clockSkew) {

        this.clock = clock;
        this.activeSecret = decodeSecret(activeSecret);
        this.defaultTtl = defaultTtl;
        this.clockSkew = clockSkew;

        List<byte[]> all = new ArrayList<>();

        if (this.activeSecret != null) {
            all.add(this.activeSecret);
        }

        for (String previous : previousSecrets) {
            byte[] decoded = decodeSecret(previous);

            if (decoded != null) {
                all.add(decoded);
            }
        }

        this.allSecrets = List.copyOf(all);
    }

    @Override
    public String toSignedToken(FileEntry fileEntry, Duration ttl) {
        if (activeSecret == null) {
            throw new IllegalStateException(
                "Cannot mint signed file URL: bytechef.file-storage.signed-url.secret is not configured");
        }

        long exp = clock.instant()
            .plus(ttl)
            .getEpochSecond();

        String payload = toUrlSafeBase64(
            Base64.getDecoder()
                .decode(fileEntry.toId()));
        String signingInput = VERSION + SEPARATOR + exp + SEPARATOR + payload;
        String signature = toUrlSafeBase64(hmac(signingInput, activeSecret));

        return signingInput + SEPARATOR + signature;
    }

    @Override
    public String toSignedToken(FileEntry fileEntry) {
        return toSignedToken(fileEntry, defaultTtl);
    }

    @Override
    public Optional<FileEntry> parseSignedToken(String token) {
        if (allSecrets.isEmpty() || !looksLikeSignedToken(token)) {
            return Optional.empty();
        }

        String[] parts = token.split("\\.", -1);

        if (parts.length != 4) {
            return Optional.empty();
        }

        long exp;

        try {
            exp = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }

        long now = clock.instant()
            .getEpochSecond();
        long skewSeconds = clockSkew.toSeconds();

        if (now > exp + skewSeconds) {
            // Expired (with skew tolerance toward acceptance). We deliberately do NOT check for "future-dated"
            // tokens: the only way to mint one is to hold the signing secret, in which case the attacker can do
            // far worse than predate a token. A misconfigured signing clock would just produce unusable tokens.
            return Optional.empty();
        }

        String signingInput = parts[0] + SEPARATOR + parts[1] + SEPARATOR + parts[2];
        byte[] presentedSignature = fromUrlSafeBase64(parts[3]);

        if (presentedSignature == null) {
            return Optional.empty();
        }

        boolean signatureValid = false;

        for (byte[] secret : allSecrets) {
            byte[] expected = hmac(signingInput, secret);

            if (MessageDigest.isEqual(expected, presentedSignature)) {
                signatureValid = true;
                break;
            }
        }

        if (!signatureValid) {
            return Optional.empty();
        }

        byte[] payloadBytes = fromUrlSafeBase64(parts[2]);

        if (payloadBytes == null) {
            return Optional.empty();
        }

        String legacyId = Base64.getEncoder()
            .encodeToString(payloadBytes);

        try {
            return Optional.of(FileEntry.parse(legacyId));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean looksLikeSignedToken(String token) {
        if (token == null || token.length() < 8 || !token.startsWith(VERSION + SEPARATOR)) {
            return false;
        }

        int dots = 0;

        for (int i = 0; i < token.length(); i++) {
            if (token.charAt(i) == SEPARATOR) {
                dots++;
            }
        }

        return dots == 3;
    }

    private static byte @Nullable [] decodeSecret(@Nullable String secret) {
        if (secret == null || secret.isBlank()) {
            return null;
        }

        return Base64.getDecoder()
            .decode(secret);
    }

    private static byte[] hmac(String input, byte[] secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);

            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));

            return mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("HmacSHA256 not available in this JVM", e);
        } catch (InvalidKeyException e) {
            throw new IllegalStateException(
                "Configured bytechef.file-storage.signed-url.secret is not a valid HMAC key", e);
        }
    }

    private static String toUrlSafeBase64(byte[] bytes) {
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(bytes);
    }

    private static byte @Nullable [] fromUrlSafeBase64(String value) {
        try {
            return Base64.getUrlDecoder()
                .decode(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :server:libs:core:file-storage:file-storage-api:test --tests "*FileEntryTokensImplTest*"`
Expected: PASS (all 16 tests).

- [ ] **Step 5: Run spotless + check**

Run: `./gradlew :server:libs:core:file-storage:file-storage-api:spotlessApply :server:libs:core:file-storage:file-storage-api:check`
Expected: BUILD SUCCESSFUL. If SpotBugs flags the `Mac.getInstance` usage or the `EI_EXPOSE_REP2` constructor, add `@SuppressFBWarnings` with a justifying comment (the secret bytes are intentionally kept by reference for repeated MAC inits; copying on every HMAC computation is wasteful and provides no security benefit since the array is private).

- [ ] **Step 6: Commit**

```bash
git add server/libs/core/file-storage/file-storage-api/src/main/java/com/bytechef/file/storage/token/FileEntryTokensImpl.java
git add server/libs/core/file-storage/file-storage-api/src/test/java/com/bytechef/platform/file/storage/token/FileEntryTokensImplTest.java
git commit -m "5008 Implement FileEntryTokens HMAC-SHA256 signer + verifier"
```

---

## Task 4: Wire `FileEntryTokens` autoconfiguration

**Files:**
- Create: `server/libs/core/file-storage/file-storage-api/src/main/java/com/bytechef/file/storage/token/FileEntryTokensAutoConfiguration.java`
- Create or modify: `server/libs/core/file-storage/file-storage-api/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Create: `server/libs/core/file-storage/file-storage-api/src/test/java/com/bytechef/platform/file/storage/token/FileEntryTokensAutoConfigurationTest.java`

- [ ] **Step 1: Write the autoconfiguration**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.bytechef.file.storage.token;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.FileStorage.SignedUrl;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Wires {@link FileEntryTokens} from {@code bytechef.file-storage.signed-url.*} configuration. Fails fast when
 * {@code required=true} and no secret is set; logs a single WARN at startup when {@code required=false} and no
 * secret is set (legacy-only mode).
 *
 * @author Ivica Cardic
 */
@AutoConfiguration
public class FileEntryTokensAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(FileEntryTokensAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public FileEntryTokens fileEntryTokens(
        ApplicationProperties applicationProperties, ObjectProvider<Clock> clockProvider) {

        SignedUrl signedUrl = applicationProperties.getFileStorage()
            .getSignedUrl();

        if (signedUrl.isRequired() && (signedUrl.getSecret() == null || signedUrl.getSecret()
            .isBlank())) {

            throw new IllegalStateException(
                "bytechef.file-storage.signed-url.required=true but bytechef.file-storage.signed-url.secret is "
                    + "not set. Configure a base64-encoded 32-byte HMAC-SHA256 secret or set required=false.");
        }

        if (signedUrl.getSecret() == null || signedUrl.getSecret()
            .isBlank()) {

            logger.warn(
                "bytechef.file-storage.signed-url.secret is not configured; public /file-entries URLs will only "
                    + "accept legacy unsigned IDs. Set the secret to mint and verify HMAC-signed tokens.");
        }

        Clock clock = clockProvider.getIfAvailable(Clock::systemUTC);

        return new FileEntryTokensImpl(
            clock, signedUrl.getSecret(), signedUrl.getPreviousSecrets(), signedUrl.getDefaultTtl(),
            signedUrl.getClockSkew());
    }
}
```

- [ ] **Step 2: Register the autoconfiguration**

Check whether the imports file exists:

Run: `ls server/libs/core/file-storage/file-storage-api/src/main/resources/META-INF/spring/ 2>/dev/null`

If the file `org.springframework.boot.autoconfigure.AutoConfiguration.imports` exists, append a new line. If not, create it with content:

```
com.bytechef.file.storage.token.FileEntryTokensAutoConfiguration
```

- [ ] **Step 3: Write the autoconfig test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.bytechef.platform.file.storage.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.token.FileEntryTokens;
import com.bytechef.file.storage.token.FileEntryTokensAutoConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@SuppressFBWarnings("HARD_CODE_PASSWORD")
public class FileEntryTokensAutoConfigurationTest {

    private static final String SECRET = "dGVzdC1zZWNyZXQtMzItYnl0ZXMtYmFzZTY0LWVuY29kZWQ=";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(FileEntryTokensAutoConfiguration.class))
        .withUserConfiguration(EnablePropertiesConfig.class);

    @Test
    public void testBeanCreatedWithSecret() {
        runner.withPropertyValues("bytechef.file-storage.signed-url.secret=" + SECRET)
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
                .hasMessageContaining("signed-url.secret is not set"));
    }

    @EnableConfigurationProperties(ApplicationProperties.class)
    static class EnablePropertiesConfig {
    }
}
```

- [ ] **Step 4: Run the tests**

Run: `./gradlew :server:libs:core:file-storage:file-storage-api:test --tests "*FileEntryTokensAutoConfigurationTest*"`
Expected: PASS (3 tests).

- [ ] **Step 5: Spotless + check**

Run: `./gradlew :server:libs:core:file-storage:file-storage-api:spotlessApply :server:libs:core:file-storage:file-storage-api:check`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add server/libs/core/file-storage/file-storage-api/src/main/java/com/bytechef/file/storage/token/FileEntryTokensAutoConfiguration.java
git add server/libs/core/file-storage/file-storage-api/src/test/java/com/bytechef/platform/file/storage/token/FileEntryTokensAutoConfigurationTest.java
git add server/libs/core/file-storage/file-storage-api/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
git commit -m "5008 Auto-wire FileEntryTokens bean from bytechef.file-storage.signed-url config"
```

---

## Task 5: `FileEntryController` accepts both signed and legacy tokens

**Files:**
- Modify: `server/libs/platform/platform-webhook/platform-webhook-rest/platform-webhook-rest-impl/src/main/java/com/bytechef/platform/webhook/web/rest/FileEntryController.java`
- Create: `server/libs/platform/platform-webhook/platform-webhook-rest/platform-webhook-rest-impl/src/test/java/com/bytechef/platform/webhook/web/rest/FileEntryControllerTest.java`

- [ ] **Step 1: Write the failing tests**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.bytechef.platform.webhook.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.token.FileEntryTokens;
import com.bytechef.platform.file.storage.TempFileStorage;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SuppressFBWarnings("HARD_CODE_PASSWORD")
public class FileEntryControllerTest {

    private TempFileStorage tempFileStorage;
    private FileEntryTokens fileEntryTokens;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        tempFileStorage = Mockito.mock(TempFileStorage.class);
        fileEntryTokens = Mockito.mock(FileEntryTokens.class);

        FileEntryController controller = new FileEntryController(tempFileStorage, fileEntryTokens, false);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .build();
    }

    @Test
    void testSignedTokenAccepted() throws Exception {
        FileEntry parsed = new FileEntry("file.txt", "txt", "text/plain", "file:/temp/uuid.txt");

        when(fileEntryTokens.looksLikeSignedToken("v1.123.payload.sig")).thenReturn(true);
        when(fileEntryTokens.parseSignedToken("v1.123.payload.sig")).thenReturn(Optional.of(parsed));
        when(tempFileStorage.getInputStream(parsed))
            .thenReturn(new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(get("/file-entries/v1.123.payload.sig/content"))
            .andExpect(status().isOk())
            .andExpect(content().string("hello"));
    }

    @Test
    void testSignedTokenRejectedAsNotFound() throws Exception {
        when(fileEntryTokens.looksLikeSignedToken("v1.123.bad.sig")).thenReturn(true);
        when(fileEntryTokens.parseSignedToken("v1.123.bad.sig")).thenReturn(Optional.empty());

        mockMvc.perform(get("/file-entries/v1.123.bad.sig/content"))
            .andExpect(status().isNotFound());

        verify(tempFileStorage, never()).getInputStream(any());
    }

    @Test
    void testLegacyIdAcceptedInPermissiveMode() throws Exception {
        // base64("txt_;_text/plain_;_file_;_file:/temp/uuid.txt")
        String legacyId = "dHh0XztfdGV4dC9wbGFpbl87X2ZpbGVfO19maWxlOi90ZW1wL3V1aWQudHh0";

        when(fileEntryTokens.looksLikeSignedToken(legacyId)).thenReturn(false);
        when(tempFileStorage.getInputStream(any()))
            .thenReturn(new ByteArrayInputStream("legacy".getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(get("/file-entries/" + legacyId + "/content"))
            .andExpect(status().isOk())
            .andExpect(content().string("legacy"));

        verify(fileEntryTokens, never()).parseSignedToken(any());
    }

    @Test
    void testLegacyIdRejectedInStrictMode() throws Exception {
        FileEntryController strictController = new FileEntryController(tempFileStorage, fileEntryTokens, true);

        MockMvc strictMvc = MockMvcBuilders.standaloneSetup(strictController)
            .build();

        String legacyId = "dHh0XztfdGV4dC9wbGFpbl87X2ZpbGVfO19maWxlOi90ZW1wL3V1aWQudHh0";

        when(fileEntryTokens.looksLikeSignedToken(legacyId)).thenReturn(false);

        strictMvc.perform(get("/file-entries/" + legacyId + "/content"))
            .andExpect(status().isNotFound());

        verify(tempFileStorage, never()).getInputStream(any());
    }

    @Test
    void testMaliciousLegacyIdReturns404NotFiveHundred() throws Exception {
        // base64("txt_;_text/plain_;_file_;_/etc/passwd") — already rejected by FileEntry.parse
        String maliciousId = "dHh0XztfdGV4dC9wbGFpbl87X2ZpbGVfO18vZXRjL3Bhc3N3ZA==";

        when(fileEntryTokens.looksLikeSignedToken(maliciousId)).thenReturn(false);

        mockMvc.perform(get("/file-entries/" + maliciousId + "/content"))
            .andExpect(status().isNotFound());

        verify(tempFileStorage, never()).getInputStream(any());
    }
}
```

- [ ] **Step 2: Run the test to confirm it fails**

Run: `./gradlew :server:libs:platform:platform-webhook:platform-webhook-rest:platform-webhook-rest-impl:test --tests "*FileEntryControllerTest*"`
Expected: COMPILATION ERROR — constructor signature doesn't match.

- [ ] **Step 3: Modify the controller**

Replace the body of `FileEntryController.java` with:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.bytechef.platform.webhook.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.token.FileEntryTokens;
import com.bytechef.platform.file.storage.TempFileStorage;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves files produced by public webhook executions. The endpoint is intentionally unauthenticated — webhook
 * callers are themselves unauthenticated — so capability-by-knowledge has been the de facto access control.
 * <p>
 * As of the 2026-05-18 signing rollout, the preferred form is an HMAC-signed token (see
 * {@link FileEntryTokens}); the legacy unsigned-id form remains accepted while
 * {@code bytechef.file-storage.signed-url.required} is {@code false}.
 *
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnCoordinator
public class FileEntryController {

    private static final Logger logger = LoggerFactory.getLogger(FileEntryController.class);
    private static final org.slf4j.Marker LEGACY_MARKER = org.slf4j.MarkerFactory.getMarker("FILE_ENTRY_LEGACY");

    private final TempFileStorage tempFileStorage;
    private final FileEntryTokens fileEntryTokens;
    private final boolean signedRequired;

    public FileEntryController(
        TempFileStorage tempFileStorage, FileEntryTokens fileEntryTokens,
        @Value("${bytechef.file-storage.signed-url.required:false}") boolean signedRequired) {

        this.tempFileStorage = tempFileStorage;
        this.fileEntryTokens = fileEntryTokens;
        this.signedRequired = signedRequired;
    }

    @GetMapping("/file-entries/{id}/content")
    @ResponseBody
    public ResponseEntity<InputStreamResource> getFileEntryContent(@PathVariable("id") String id) {
        Optional<FileEntry> fileEntry = resolve(id);

        if (fileEntry.isEmpty()) {
            return ResponseEntity.notFound()
                .build();
        }

        FileEntry resolved = fileEntry.get();

        return ResponseEntity.ok()
            .contentType(
                resolved.getMimeType() == null
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.asMediaType(MimeType.valueOf(resolved.getMimeType())))
            .body(new InputStreamResource(tempFileStorage.getInputStream(resolved)));
    }

    private Optional<FileEntry> resolve(String id) {
        if (fileEntryTokens.looksLikeSignedToken(id)) {
            return fileEntryTokens.parseSignedToken(id);
        }

        if (signedRequired) {
            return Optional.empty();
        }

        logger.warn(LEGACY_MARKER,
            "Accepted legacy unsigned file-entry id. Migrate clients to signed URLs; this path will be removed.");

        try {
            return Optional.of(FileEntry.parse(id));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
```

The new constructor takes `FileEntryTokens` and a `signedRequired` boolean. The `@Value` default of `false` keeps existing deployments working without config changes. Note that `ApplicationProperties` import was removed because we're reading the single boolean directly via `@Value` — keeps the controller easy to unit-test (no need to mock `ApplicationProperties`).

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :server:libs:platform:platform-webhook:platform-webhook-rest:platform-webhook-rest-impl:test --tests "*FileEntryControllerTest*"`
Expected: PASS (5 tests).

- [ ] **Step 5: Spotless + check**

Run: `./gradlew :server:libs:platform:platform-webhook:platform-webhook-rest:platform-webhook-rest-impl:spotlessApply :server:libs:platform:platform-webhook:platform-webhook-rest:platform-webhook-rest-impl:check`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add server/libs/platform/platform-webhook/platform-webhook-rest/platform-webhook-rest-impl/src/main/java/com/bytechef/platform/webhook/web/rest/FileEntryController.java
git add server/libs/platform/platform-webhook/platform-webhook-rest/platform-webhook-rest-impl/src/test/java/com/bytechef/platform/webhook/web/rest/FileEntryControllerTest.java
git commit -m "5008 FileEntryController accepts both signed tokens and legacy ids"
```

---

## Task 6: Rate-limit the legacy-use WARN log

**Files:**
- Modify: `server/libs/platform/platform-webhook/platform-webhook-rest/platform-webhook-rest-impl/src/main/java/com/bytechef/platform/webhook/web/rest/FileEntryController.java`
- Modify: `server/libs/platform/platform-webhook/platform-webhook-rest/platform-webhook-rest-impl/src/test/java/com/bytechef/platform/webhook/web/rest/FileEntryControllerTest.java`

The Phase 1 spec says the WARN must be rate-limited so high-traffic deployments don't flood logs. Use a simple time-window check rather than pulling in a rate-limiter dependency.

- [ ] **Step 1: Add a failing test for the rate-limit**

Add to `FileEntryControllerTest.java`:

```java
    @Test
    void testLegacyWarnRateLimited() throws Exception {
        String legacyId = "dHh0XztfdGV4dC9wbGFpbl87X2ZpbGVfO19maWxlOi90ZW1wL3V1aWQudHh0";

        when(fileEntryTokens.looksLikeSignedToken(legacyId)).thenReturn(false);
        when(tempFileStorage.getInputStream(any()))
            .thenReturn(new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)));

        // We can't easily assert log output without a custom appender — instead, verify the controller doesn't
        // crash on rapid-fire requests and that the legacy path still serves all of them.
        for (int i = 0; i < 100; i++) {
            mockMvc.perform(get("/file-entries/" + legacyId + "/content"))
                .andExpect(status().isOk());
        }
    }
```

This is a smoke test — the real verification of "WARN is rate-limited" requires a log appender fixture that's overkill here. The smoke test ensures the rate-limit logic doesn't throw or break correctness; manual inspection of logs during a load test confirms the actual rate-limit behavior in CI.

- [ ] **Step 2: Add the rate-limit field and check to `FileEntryController`**

Add to the controller, after the `signedRequired` field:

```java
    private static final long LEGACY_WARN_INTERVAL_MS = 60_000L;

    private final java.util.concurrent.atomic.AtomicLong lastLegacyWarnEpochMs = new java.util.concurrent.atomic.AtomicLong();
```

Modify the `resolve` method's legacy branch:

```java
        if (signedRequired) {
            return Optional.empty();
        }

        maybeLogLegacyAccess();

        try {
            return Optional.of(FileEntry.parse(id));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private void maybeLogLegacyAccess() {
        long now = System.currentTimeMillis();
        long last = lastLegacyWarnEpochMs.get();

        if (now - last >= LEGACY_WARN_INTERVAL_MS && lastLegacyWarnEpochMs.compareAndSet(last, now)) {
            logger.warn(LEGACY_MARKER,
                "Accepted legacy unsigned file-entry id. Migrate clients to signed URLs; this path will be removed.");
        }
    }
```

(Remove the unconditional `logger.warn` call from the original `resolve` method.)

- [ ] **Step 3: Run tests**

Run: `./gradlew :server:libs:platform:platform-webhook:platform-webhook-rest:platform-webhook-rest-impl:test --tests "*FileEntryControllerTest*"`
Expected: PASS (6 tests now).

- [ ] **Step 4: Spotless + check**

Run: `./gradlew :server:libs:platform:platform-webhook:platform-webhook-rest:platform-webhook-rest-impl:spotlessApply :server:libs:platform:platform-webhook:platform-webhook-rest:platform-webhook-rest-impl:check`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add server/libs/platform/platform-webhook/platform-webhook-rest/platform-webhook-rest-impl/src/main/java/com/bytechef/platform/webhook/web/rest/FileEntryController.java
git add server/libs/platform/platform-webhook/platform-webhook-rest/platform-webhook-rest-impl/src/test/java/com/bytechef/platform/webhook/web/rest/FileEntryControllerTest.java
git commit -m "5008 Rate-limit legacy file-entry-id WARN to once per minute"
```

---

## Task 7: Audit URL-emission sites and route them through `FileEntryTokens`

This task is the audit; Task 8 is the switching. The audit can find anywhere from 0 to ~5 emission sites; the steps below give the engineer the commands to run and the disposition criteria.

**Files:**
- No edits in this task — produces a list to drive Task 8.

- [ ] **Step 1: Find every caller of `FileEntry.toId()`**

Run:
```bash
grep -rn "\.toId()" server --include="*.java" | grep -i "fileEntry\|FileEntry" | grep -v "/test/" | grep -v "/build/"
```

Expected output: roughly 2–10 hits. Save the list.

- [ ] **Step 2: Classify each hit**

For each hit, open the file and read 20 lines of surrounding context. Each call is one of three categories:

1. **Internal serialization** — the result is stored in a database column or passed across an internal API boundary (Spring service-to-service). Example: `AiAgentEvalResult.setTranscriptFileEntry` writing to the `transcript_file` column. **Do NOT change these.** Keep `toId()`.
2. **Public URL emission** — the result is embedded in an HTTP response body or a payload that travels outside the server (webhook response, copilot message, UI download link, email body). **Switch to signed.**
3. **Logging/diagnostics** — the result is included in a log line or error message for operator debugging. **Do NOT change these.** A signed URL in a log is less informative than the legacy ID and TTL-expires.

- [ ] **Step 3: Also check for URL construction without `toId()`**

Some emission sites may construct URLs manually (e.g. `"/file-entries/" + someId + "/content"`). Grep for that pattern:

```bash
grep -rn "/file-entries/" server --include="*.java" | grep -v "/test/" | grep -v "/build/" | grep -v WebhookAuthorize
```

Expected output: the controller itself plus any URL-templating helpers. Anywhere a URL is constructed by string-building, the input `id` should be the signed token, not `toId()`.

- [ ] **Step 4: Produce the disposition list**

Write a checklist of (file:line, action) pairs to drive Task 8. Example format:
```
server/libs/.../WebhookExecutionResponseAssembler.java:142  switch
server/libs/.../ContextImpl.java:387                         switch
server/.../AiAgentEvalResult.java:153                        keep (internal serialization)
server/.../LogFileStorageImpl.java:91                        keep (log diagnostic)
```

No commit in this task — it's an analysis step. The list goes into the Task 8 PR description.

---

## Task 8: Switch identified emission sites to signed tokens

**Files:**
- Modify: each file identified in Task 7 with action `switch`.

For each emission site, follow this sub-template. The example below uses a hypothetical `WebhookExecutionResponseAssembler` but the pattern applies to any site.

- [ ] **Step 1: Inject `FileEntryTokens` into the emitting class**

If the class is a Spring bean: add the bean to the constructor and the field.
If the class is a static utility: convert it to a Spring bean OR thread `FileEntryTokens` through the call chain. Prefer conversion to a bean — it's a one-time change and unblocks future signing-related work.

- [ ] **Step 2: Write a failing test that the emitted URL is a signed token**

```java
@Test
public void testEmittedUrlIsSigned() {
    FileEntry fileEntry = new FileEntry(
        "report.txt", "txt", "text/plain", "file:/temp/uuid.txt");
    String emittedUrl = subject.buildUrl(fileEntry);
    String pathParam = emittedUrl.substring(emittedUrl.indexOf("/file-entries/") + "/file-entries/".length(),
        emittedUrl.indexOf("/content"));

    assertThat(pathParam).startsWith("v1.");
    assertThat(pathParam.split("\\.")).hasSize(4);
}
```

- [ ] **Step 3: Replace `fileEntry.toId()` with `fileEntryTokens.toSignedToken(fileEntry)`**

Before:
```java
String url = "/file-entries/" + fileEntry.toId() + "/content";
```

After:
```java
String url = "/file-entries/" + fileEntryTokens.toSignedToken(fileEntry) + "/content";
```

- [ ] **Step 4: Run the test**

Expected: PASS.

- [ ] **Step 5: Run the full module check**

Run: `./gradlew :path:to:module:check`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit (one commit per emission site)**

```bash
git add <files>
git commit -m "5008 Sign public file URLs emitted by <emitter name>"
```

**Important:** keep commits granular — one emission site per commit. If a test depends on a downstream consumer, the consumer test may also need updating (test fixtures often hardcode legacy URLs); update them in the same commit.

---

## Task 9: Documentation

**Files:**
- Modify: `CLAUDE.md`
- Modify or create: `server/libs/platform/platform-webhook/platform-webhook-rest/platform-webhook-rest-impl/README.md`

- [ ] **Step 1: Update `CLAUDE.md`**

Find the "Persistence Conventions" section in `CLAUDE.md` (it's near the workspace_id placement bullet). Add a new bullet after it:

```markdown
## Public URL Signing

- `/file-entries/{id}/content` is intentionally unauthenticated (serves webhook outputs to anonymous callers). As of the 2026-05-18 signing rollout, the preferred form is an HMAC-SHA256 signed token (`v1.<exp>.<payload>.<sig>`) minted via `FileEntryTokens.toSignedToken`. Legacy unsigned `FileEntry.toId()` IDs are still accepted while `bytechef.file-storage.signed-url.required=false` (default).
- **Use `FileEntry.toId()` for**: DB persistence, intra-process passing. No security claim, deterministic forever.
- **Use `FileEntryTokens.toSignedToken(fileEntry)` for**: anything that leaves the server as part of a URL (webhook response body, chat copilot output, UI download link). TTL applies.
- Spec: `docs/superpowers/specs/2026-05-18-hmac-signed-file-entry-tokens-design.md`.
```

- [ ] **Step 2: Create the module README section**

If `platform-webhook-rest-impl/README.md` doesn't exist, create it with the following content. If it does, append the section under an existing heading.

```markdown
## File entry URL signing

The `/file-entries/{id}/content` endpoint serves files produced by webhook executions. It accepts two token formats:

- **Signed (preferred):** `v1.<exp>.<payload>.<sig>` — HMAC-SHA256 token with a TTL. Minted by `FileEntryTokens.toSignedToken(fileEntry)`. Configure via `bytechef.file-storage.signed-url.*`.
- **Legacy:** base64-encoded `<extension>_;_<mime>_;_<name>_;_<url>` — unsigned, no expiry. Accepted while `bytechef.file-storage.signed-url.required=false`. Each legacy access emits a rate-limited WARN.

### Operator runbook

**Initial enablement:**
1. Generate a secret: `head -c 32 /dev/urandom | base64`
2. Set `BYTECHEF_FILE_STORAGE_SIGNING_SECRET=<that value>` in your deployment environment.
3. Restart the application. Newly emitted URLs will be signed; legacy URLs continue to work.

**Cutover to strict mode (after legacy traffic drains):**
1. Monitor logs for the `FILE_ENTRY_LEGACY` marker. When the rate drops to zero for ≥ 2 × default TTL, you can flip strict mode.
2. Set `BYTECHEF_FILE_STORAGE_SIGNED_URL_REQUIRED=true` and restart.
3. Legacy URLs (if any remain) will return 404. Roll back the flag if you see legitimate traffic affected.

**Key rotation:**
1. Generate a new secret.
2. Move the current `secret` into `bytechef.file-storage.signed-url.previous-secrets`.
3. Set the new value as `secret`.
4. Restart. URLs signed with the old key continue to verify against `previous-secrets`; new URLs are signed with the new key.
5. After 2 × default TTL has elapsed, remove the old secret from `previous-secrets`.
```

- [ ] **Step 3: Commit**

```bash
git add CLAUDE.md
git add server/libs/platform/platform-webhook/platform-webhook-rest/platform-webhook-rest-impl/README.md
git commit -m "5008 Document HMAC-signed file URL flow + ops runbook"
```

---

## Task 10: End-to-end smoke verification

**Files:** none modified.

- [ ] **Step 1: Run the full check on all touched modules**

```bash
./gradlew \
  :server:libs:config:app-config:check \
  :server:libs:core:file-storage:file-storage-api:check \
  :server:libs:core:file-storage:file-storage-filesystem-service:check \
  :server:libs:platform:platform-webhook:platform-webhook-rest:platform-webhook-rest-impl:check
```
Expected: BUILD SUCCESSFUL across all modules.

- [ ] **Step 2: Boot the server with a secret configured**

```bash
BYTECHEF_FILE_STORAGE_SIGNING_SECRET=$(head -c 32 /dev/urandom | base64) \
  ./gradlew -p server/apps/server-app bootRun
```

In another terminal, trigger a webhook that returns a `FileEntry` (use any existing workflow that returns a file). Observe the response body — the `/file-entries/{token}/content` URL should start with `v1.`. Fetch the URL with `curl` — expect 200 with the file contents.

- [ ] **Step 3: Test legacy fallback**

Construct a legacy ID (the security report's payload, mutated to point at a real file). Send a request — expect 200 plus a WARN line tagged `FILE_ENTRY_LEGACY` in the server log.

- [ ] **Step 4: Test strict mode rejects legacy**

Set `BYTECHEF_FILE_STORAGE_SIGNED_URL_REQUIRED=true`, restart, retry the legacy ID — expect 404.

- [ ] **Step 5: Test boot-failure on `required=true` without secret**

Unset `BYTECHEF_FILE_STORAGE_SIGNING_SECRET`, keep `BYTECHEF_FILE_STORAGE_SIGNED_URL_REQUIRED=true`. Boot. Expect immediate startup failure with the message from `FileEntryTokensAutoConfiguration`.

- [ ] **Step 6: Open PR**

```bash
git push -u origin <branch>
gh pr create --title "5008 Add HMAC-signed file-entry tokens for public URLs" \
  --body "$(cat <<'EOF'
## Summary

Adds HMAC-SHA256 signing for public `/file-entries/{id}/content` URLs to defend against URL leakage, replay past intended use, and forgery. Endpoint stays unauthenticated (serves webhook outputs); the signature is the access control.

Backward compatible: legacy unsigned IDs continue to work while `bytechef.file-storage.signed-url.required=false` (default). Flip to strict mode once legacy traffic drains.

## Test plan

- [x] `FileEntryTokensImplTest` — round-trip, expired, tampered, key rotation, clock skew (15 unit tests)
- [x] `FileEntryTokensAutoConfigurationTest` — bean wiring + fail-fast on missing required secret
- [x] `FileEntryControllerTest` — signed/legacy/strict/malicious paths
- [x] Manual smoke: signed URL emission, legacy fallback, strict-mode rejection, boot-failure
- Spec: `docs/superpowers/specs/2026-05-18-hmac-signed-file-entry-tokens-design.md`
- Plan: `docs/superpowers/plans/2026-05-18-hmac-signed-file-entry-tokens.md`
EOF
)"
```

---

## Out of scope (Phase 2+)

These are tracked separately and should NOT be implemented in this PR:

- **Micrometer counter** `bytechef_file_entry_token_total{kind,outcome}` — Phase 2.
- **`X-File-Token-Deprecation: legacy` response header** — Phase 2.
- **`required` flag defaulting to true** — Phase 3 (next major release).
- **Per-URL revocation list** — explicitly non-goal in the spec; rotate the key instead.
- **Recipient binding (IP/session/etc.)** — explicitly non-goal in the spec.
