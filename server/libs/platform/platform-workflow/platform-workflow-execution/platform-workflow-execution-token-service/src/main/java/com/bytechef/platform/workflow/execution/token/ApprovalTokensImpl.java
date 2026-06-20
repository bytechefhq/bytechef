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
 * HMAC-SHA256 signer/verifier for approval/resume capability links. Token format: {@code v1.<exp>.<payload>.<sig>}
 * where {@code <exp>} is decimal Unix epoch seconds, {@code <payload>} is the Base64URL-without-padding rendering of
 * the inner token string ({@code JobResumeId}/{@code ApprovalId} Base64 body), and {@code <sig>} is
 * Base64URL-without-padding HMAC-SHA256 over the literal ASCII string {@code v1.<exp>.<payload>} using the active
 * signing secret. Mirrors {@code FileEntryTokensImpl}.
 *
 * @author Ivica Cardic
 */
public class ApprovalTokensImpl implements ApprovalTokens {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String VERSION = "v1";
    private static final char SEPARATOR = '.';

    private final Clock clock;
    @Nullable
    private final byte[] activeSecret;
    private final List<byte[]> allSecrets;
    private final Duration defaultTtl;
    private final Duration clockSkew;
    private final boolean signedTokenRequired;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ApprovalTokensImpl(
        Clock clock, @Nullable String activeSecret, List<String> previousSecrets, Duration defaultTtl,
        Duration clockSkew, boolean signedTokenRequired) {

        this.clock = clock;
        this.activeSecret = decodeSecret(activeSecret);
        this.defaultTtl = defaultTtl;
        this.clockSkew = clockSkew;
        this.signedTokenRequired = signedTokenRequired;

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
    public String toSignedToken(String innerToken, Duration ttl) {
        if (activeSecret == null) {
            throw new IllegalStateException(
                "Cannot mint signed approval token: no signing secret configured "
                    + "(bytechef.approval.signed-token.secret is missing and no EncryptionKey is available)");
        }

        long exp = clock.instant()
            .plus(ttl)
            .getEpochSecond();

        String payload = toUrlSafeBase64(innerToken.getBytes(StandardCharsets.UTF_8));
        String signingInput = VERSION + SEPARATOR + exp + SEPARATOR + payload;
        String signature = toUrlSafeBase64(hmac(signingInput, activeSecret));

        return signingInput + SEPARATOR + signature;
    }

    @Override
    public String toSignedToken(String innerToken) {
        return toSignedToken(innerToken, defaultTtl);
    }

    @Override
    public Optional<String> toSignedTokenIfConfigured(String innerToken) {
        if (activeSecret == null) {
            return Optional.empty();
        }

        return Optional.of(toSignedToken(innerToken, defaultTtl));
    }

    @Override
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public Optional<String> parseSignedToken(String token) {
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
            return Optional.empty();
        }

        String signingInput = parts[0] + SEPARATOR + parts[1] + SEPARATOR + parts[2];
        byte[] presentedSignature = fromUrlSafeBase64(parts[3]);

        if (presentedSignature == null) {
            return Optional.empty();
        }

        boolean signatureValid = false;

        for (byte[] secret : allSecrets) {
            try {
                byte[] expected = hmac(signingInput, secret);

                if (MessageDigest.isEqual(expected, presentedSignature)) {
                    signatureValid = true;

                    break;
                }
            } catch (IllegalStateException e) {
                // A degenerate secret (e.g. zero-length) reached the verifier. Skip it and try the next key.
            }
        }

        if (!signatureValid) {
            return Optional.empty();
        }

        byte[] payloadBytes = fromUrlSafeBase64(parts[2]);

        if (payloadBytes == null) {
            return Optional.empty();
        }

        return Optional.of(new String(payloadBytes, StandardCharsets.UTF_8));
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

    @Override
    public boolean isSignedTokenRequired() {
        return signedTokenRequired;
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
                "Configured bytechef.approval.signed-token.secret is not a valid HMAC key", e);
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
