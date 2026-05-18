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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.file.storage.domain.FileEntry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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
        Clock skewedClock = Clock.fixed(fixedNow.plus(Duration.ofMinutes(1)
            .plusSeconds(20)), ZoneOffset.UTC);
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
    public void testToSignedTokenIfConfiguredReturnsEmptyWhenNoSecret() {
        FileEntryTokensImpl unconfigured = new FileEntryTokensImpl(
            Clock.systemUTC(), null, List.of(), Duration.ofHours(1), Duration.ofSeconds(60));

        assertThat(unconfigured.toSignedTokenIfConfigured(SAMPLE)).isEmpty();
    }

    @Test
    public void testToSignedTokenIfConfiguredReturnsPresentTokenWhenSecretConfigured() {
        FileEntryTokensImpl signerWithKey = signer(Clock.systemUTC(), SECRET, List.of());

        Optional<String> token = signerWithKey.toSignedTokenIfConfigured(SAMPLE);

        assertThat(token).isPresent();
        assertThat(token.get()).startsWith("v1.");
        assertThat(signerWithKey.parseSignedToken(token.get()))
            .isPresent()
            .hasValueSatisfying(parsed -> assertThat(parsed.getUrl()).isEqualTo(SAMPLE.getUrl()));
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

    @Test
    public void testValidSignatureWithInvalidPayloadRejected() throws NoSuchAlgorithmException, InvalidKeyException {
        FileEntryTokensImpl signer = signer(Clock.systemUTC(), SECRET, List.of());

        String validToken = signer.toSignedToken(SAMPLE, Duration.ofHours(1));
        String[] parts = validToken.split("\\.");

        // Replace payload with a Base64URL encoding of a FileEntry.toId()-shaped string whose url is /etc/passwd
        // (rejected by FileEntry.parse() since the path-traversal hardening).
        String maliciousPayload = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString("txt_;_text/plain_;_file_;_/etc/passwd".getBytes(StandardCharsets.UTF_8));

        // Re-sign so the signature is valid for the tampered payload — proves the rejection is from FileEntry.parse,
        // not from signature mismatch.
        String signingInput = parts[0] + "." + parts[1] + "." + maliciousPayload;

        Mac mac = Mac.getInstance("HmacSHA256");

        mac.init(new SecretKeySpec(Base64.getDecoder()
            .decode(SECRET), "HmacSHA256"));

        String reSignature = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        String maliciousToken = signingInput + "." + reSignature;

        assertThat(signer.parseSignedToken(maliciousToken)).isEmpty();
    }

    private static FileEntryTokensImpl signer(Clock clock, String secret, List<String> previousSecrets) {
        return new FileEntryTokensImpl(clock, secret, previousSecrets, Duration.ofHours(1), Duration.ofSeconds(60));
    }
}
