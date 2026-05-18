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
 * HMAC-SHA256 signer/verifier for public file URLs. Token format: {@code v1.<exp>.<payload>.<sig>} where {@code <exp>}
 * is decimal Unix epoch seconds, {@code <payload>} is the Base64URL-without-padding rendering of the existing
 * {@link FileEntry#toId()} output, and {@code <sig>} is Base64URL-without-padding HMAC-SHA256 over the literal ASCII
 * string {@code v1.<exp>.<payload>} using the active signing secret.
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
                "Cannot mint signed file URL: no signing secret configured "
                    + "(bytechef.file-storage.signed-url.secret is missing)");
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
    @SuppressWarnings("PMD.EmptyCatchBlock")
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
            try {
                byte[] expected = hmac(signingInput, secret);

                if (MessageDigest.isEqual(expected, presentedSignature)) {
                    signatureValid = true;
                    break;
                }
            } catch (IllegalStateException e) {
                // A degenerate secret (e.g. zero-length) reached the verifier. Skip it and try the next key.
                // The verify path must never throw — the spec contract is uniform Optional.empty() on any failure.
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
    public Optional<String> toSignedTokenIfConfigured(FileEntry fileEntry) {
        if (activeSecret == null) {
            return Optional.empty();
        }

        return Optional.of(toSignedToken(fileEntry, defaultTtl));
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
