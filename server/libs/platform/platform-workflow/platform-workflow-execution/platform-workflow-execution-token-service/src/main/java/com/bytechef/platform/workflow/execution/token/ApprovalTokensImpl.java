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

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.platform.workflow.execution.JobResumeId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public final class ApprovalTokensImpl implements ApprovalTokens {

    private static final Logger log = LoggerFactory.getLogger(ApprovalTokensImpl.class);

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int MINIMUM_SECRET_LENGTH = 32;
    private static final String VERSION = "v1";
    private static final char SEPARATOR = '.';
    private static final byte SHORT_TOKEN_VERSION = 1;
    private static final byte[] SHORT_TOKEN_MAC_LABEL = "bytechef-approval-short-token-v1".getBytes(
        StandardCharsets.UTF_8);
    private static final int SHORT_TOKEN_MAC_LENGTH = 12;
    private static final int SHORT_TOKEN_MAXIMUM_BODY_LENGTH = 1 + 4 + 10 + 16 + 1 + 255;
    private static final int SHORT_TOKEN_MAXIMUM_LENGTH = 62;

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

        return sign(innerToken, exp, activeSecret);
    }

    @Override
    public Optional<String> toSignedTokenIfConfigured(String innerToken) {
        return toSignedTokenIfConfigured(innerToken, defaultTtl);
    }

    @Override
    public Optional<String> toSignedTokenIfConfigured(String innerToken, Duration ttl) {
        if (activeSecret == null) {
            return Optional.empty();
        }

        return Optional.of(toSignedToken(innerToken, ttl));
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
        } catch (NumberFormatException numberFormatException) {
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
            } catch (IllegalStateException illegalStateException) {
                log.warn(
                    "Skipping an unusable approval-token signing key while verifying a token", illegalStateException);
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
    public Optional<String> toShortToken(String token) {
        if (activeSecret == null) {
            return Optional.empty();
        }

        Optional<String> innerTokenOptional = parseSignedToken(token);

        if (innerTokenOptional.isEmpty()) {
            return Optional.empty();
        }

        JobResumeId jobResumeId;

        try {
            jobResumeId = JobResumeId.parse(innerTokenOptional.get());
        } catch (IllegalArgumentException illegalArgumentException) {
            return Optional.empty();
        }

        String[] parts = token.split("\\.", -1);

        long exp = Long.parseLong(parts[1]);

        String tenantId = jobResumeId.getTenantId();

        byte[] tenantIdBytes = tenantId.getBytes(StandardCharsets.UTF_8);

        if (exp < 0 || exp > 0xFFFFFFFFL || tenantIdBytes.length > 255 || jobResumeId.getJobId() < 0) {
            return Optional.empty();
        }

        ByteBuffer byteBuffer = ByteBuffer.allocate(SHORT_TOKEN_MAXIMUM_BODY_LENGTH);

        byteBuffer.put(SHORT_TOKEN_VERSION);
        byteBuffer.putInt((int) exp);

        putVarLong(byteBuffer, jobResumeId.getJobId());

        UUID uuid = UUID.fromString(jobResumeId.getUuidAsString());

        byteBuffer.putLong(uuid.getMostSignificantBits());
        byteBuffer.putLong(uuid.getLeastSignificantBits());
        byteBuffer.put((byte) tenantIdBytes.length);
        byteBuffer.put(tenantIdBytes);

        byte[] body = Arrays.copyOf(byteBuffer.array(), byteBuffer.position());
        byte[] shortTokenBytes = Arrays.copyOf(body, body.length + SHORT_TOKEN_MAC_LENGTH);

        System.arraycopy(shortTokenMac(body, activeSecret), 0, shortTokenBytes, body.length, SHORT_TOKEN_MAC_LENGTH);

        String shortToken = toUrlSafeBase64(shortTokenBytes);

        if (shortToken.length() > SHORT_TOKEN_MAXIMUM_LENGTH) {
            return Optional.empty();
        }

        return Optional.of(shortToken);
    }

    @Override
    public Optional<String> parseShortToken(String shortToken) {
        if (activeSecret == null || shortToken == null || shortToken.length() > SHORT_TOKEN_MAXIMUM_LENGTH) {
            return Optional.empty();
        }

        byte[] shortTokenBytes = fromUrlSafeBase64(shortToken);

        if (shortTokenBytes == null || shortTokenBytes.length <= SHORT_TOKEN_MAC_LENGTH) {
            return Optional.empty();
        }

        byte[] body = Arrays.copyOf(shortTokenBytes, shortTokenBytes.length - SHORT_TOKEN_MAC_LENGTH);
        byte[] presentedMac = Arrays.copyOfRange(shortTokenBytes, body.length, shortTokenBytes.length);

        if (!isShortTokenMacValid(body, presentedMac)) {
            return Optional.empty();
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(body);

        try {
            if (byteBuffer.get() != SHORT_TOKEN_VERSION) {
                return Optional.empty();
            }

            long exp = Integer.toUnsignedLong(byteBuffer.getInt());

            Instant instant = clock.instant();

            long now = instant.getEpochSecond();

            if (now > exp + clockSkew.toSeconds()) {
                return Optional.empty();
            }

            long jobId = getVarLong(byteBuffer);
            UUID uuid = new UUID(byteBuffer.getLong(), byteBuffer.getLong());
            int tenantIdLength = Byte.toUnsignedInt(byteBuffer.get());

            if (byteBuffer.remaining() != tenantIdLength) {
                return Optional.empty();
            }

            byte[] tenantIdBytes = new byte[tenantIdLength];

            byteBuffer.get(tenantIdBytes);

            JobResumeId jobResumeId = JobResumeId.of(new String(tenantIdBytes, StandardCharsets.UTF_8), jobId, uuid);

            return Optional.of(sign(jobResumeId.toString(), exp, activeSecret));
        } catch (BufferUnderflowException | IllegalArgumentException exception) {
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

    @Override
    public boolean isSignedTokenRequired() {
        return signedTokenRequired;
    }

    private static byte @Nullable [] decodeSecret(@Nullable String secret) {
        if (secret == null || secret.isBlank()) {
            return null;
        }

        byte[] decodedSecret = EncodingUtils.base64Decode(secret);

        if (decodedSecret.length < MINIMUM_SECRET_LENGTH) {
            throw new IllegalStateException(
                "bytechef.approval.signed-token.secret must decode to at least " + MINIMUM_SECRET_LENGTH +
                    " bytes; a shorter key is trivially brute-forceable");
        }

        return decodedSecret;
    }

    private boolean isShortTokenMacValid(byte[] body, byte[] presentedMac) {
        for (byte[] secret : allSecrets) {
            try {
                if (MessageDigest.isEqual(shortTokenMac(body, secret), presentedMac)) {
                    return true;
                }
            } catch (IllegalStateException illegalStateException) {
                log.warn(
                    "Skipping an unusable approval-token signing key while verifying a short token",
                    illegalStateException);
            }
        }

        return false;
    }

    private static long getVarLong(ByteBuffer byteBuffer) {
        long value = 0;

        for (int shift = 0; shift < 64; shift += 7) {
            byte current = byteBuffer.get();

            value |= (long) (current & 0x7F) << shift;

            if ((current & 0x80) == 0) {
                return value;
            }
        }

        throw new IllegalArgumentException("Malformed job id in approval short token");
    }

    private static void putVarLong(ByteBuffer byteBuffer, long value) {
        long remaining = value;

        while ((remaining & ~0x7FL) != 0) {
            byteBuffer.put((byte) ((remaining & 0x7F) | 0x80));

            remaining >>>= 7;
        }

        byteBuffer.put((byte) remaining);
    }

    private static byte[] shortTokenMac(byte[] body, byte[] secret) {
        byte[] input = Arrays.copyOf(SHORT_TOKEN_MAC_LABEL, SHORT_TOKEN_MAC_LABEL.length + body.length);

        System.arraycopy(body, 0, input, SHORT_TOKEN_MAC_LABEL.length, body.length);

        return Arrays.copyOf(hmac(input, secret), SHORT_TOKEN_MAC_LENGTH);
    }

    private static String sign(String innerToken, long exp, byte[] secret) {
        String payload = toUrlSafeBase64(innerToken.getBytes(StandardCharsets.UTF_8));
        String signingInput = VERSION + SEPARATOR + exp + SEPARATOR + payload;
        String signature = toUrlSafeBase64(hmac(signingInput, secret));

        return signingInput + SEPARATOR + signature;
    }

    private static byte[] hmac(String input, byte[] secret) {
        return hmac(input.getBytes(StandardCharsets.UTF_8), secret);
    }

    private static byte[] hmac(byte[] input, byte[] secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);

            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));

            return mac.doFinal(input);
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IllegalStateException("HmacSHA256 not available in this JVM", noSuchAlgorithmException);
        } catch (InvalidKeyException invalidKeyException) {
            throw new IllegalStateException(
                "Configured bytechef.approval.signed-token.secret is not a valid HMAC key", invalidKeyException);
        }
    }

    private static String toUrlSafeBase64(byte[] bytes) {
        return EncodingUtils.urlEncodeBase64ToString(bytes);
    }

    private static byte @Nullable [] fromUrlSafeBase64(String value) {
        try {
            return EncodingUtils.urlDecodeBase64FromString(value);
        } catch (IllegalArgumentException illegalArgumentException) {
            return null;
        }
    }
}
