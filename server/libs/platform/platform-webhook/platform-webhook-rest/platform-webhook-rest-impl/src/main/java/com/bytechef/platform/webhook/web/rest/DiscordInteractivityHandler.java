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

package com.bytechef.platform.webhook.web.rest;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.definition.approval.ApprovalButtonPayload;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade.JobResumeOutcome;
import com.bytechef.platform.workflow.execution.service.ApprovalShortTokenService;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class DiscordInteractivityHandler {

    private static final byte[] ED25519_DER_PREFIX = HexFormat.of()
        .parseHex("302a300506032b6570032100");

    private static final int EPHEMERAL_FLAG = 64;

    private static final Logger log = LoggerFactory.getLogger(DiscordInteractivityHandler.class);

    private static final String NOT_YET_SUSPENDED_TEXT = "Still preparing — try again in a moment.";

    private final ApprovalResolutionLookup approvalResolutionLookup;
    private final ApprovalShortTokenService approvalShortTokenService;
    private final JobResumeFacade jobResumeFacade;
    private final @Nullable ApprovalTokens approvalTokens;
    private final AtomicBoolean invalidPublicKeyWarned = new AtomicBoolean();
    private final @Nullable String publicKeyHex;

    @SuppressFBWarnings("EI")
    public DiscordInteractivityHandler(
        ApprovalResolutionLookup approvalResolutionLookup, ApprovalShortTokenService approvalShortTokenService,
        JobResumeFacade jobResumeFacade, @Nullable ApprovalTokens approvalTokens, @Nullable String publicKeyHex) {

        this.approvalResolutionLookup = approvalResolutionLookup;
        this.approvalShortTokenService = approvalShortTokenService;
        this.jobResumeFacade = jobResumeFacade;
        this.approvalTokens = approvalTokens;
        this.publicKeyHex = publicKeyHex;
    }

    @SuppressFBWarnings({
        "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
    })
    public record Response(int statusCode, @Nullable Map<String, Object> body) {
    }

    public Response handle(String rawBody, @Nullable String signature, @Nullable String timestamp) {
        if (signature == null || timestamp == null || !verifySignature(signature, timestamp, rawBody)) {
            return new Response(401, null);
        }

        Map<String, ?> payload = parse(rawBody);

        if (payload == null) {
            return new Response(400, null);
        }

        Number type = payload.get("type") instanceof Number typeNumber ? typeNumber : null;

        if (type == null) {
            return new Response(400, null);
        }

        if (type.intValue() == 1) {
            return new Response(200, Map.of("type", 1));
        }

        if (type.intValue() != 3) {
            return new Response(200, Map.of("type", 6));
        }

        Map<String, ?> data = asMap(payload.get("data"));

        Object customId = data == null ? null : data.get("custom_id");

        ApprovalButtonPayload approvalButtonPayload = customId instanceof String customIdString
            ? ApprovalButtonPayload.parse(customIdString)
                .orElse(null)
            : null;

        if (approvalButtonPayload == null) {
            return new Response(200, updateMessage("This approval request is malformed."));
        }

        boolean approved = approvalButtonPayload.approved();

        String resumeId = approvalShortTokenService.fetchToken(approvalButtonPayload.shortId())
            .orElse(null);

        if (resumeId == null) {
            return new Response(200, updateMessage("⚠️ This approval is no longer available (open the form link)."));
        }

        String tenantId = resolveTenantId(resumeId);

        if (tenantId == null) {
            return new Response(200, updateMessage("⚠️ This approval is no longer available."));
        }

        String userName = extractUserName(payload);

        JobResumeOutcome outcome = TenantContext.callWithTenantId(
            tenantId,
            () -> jobResumeFacade.resumeJob(resumeId, Map.of("approved", approved), asApprovedBy(userName)));

        if (outcome == JobResumeOutcome.NOT_YET_SUSPENDED) {
            return new Response(200, ephemeralReply(NOT_YET_SUSPENDED_TEXT));
        }

        String by = userName == null ? "" : " by @" + userName;

        String text = switch (outcome) {
            case OK -> (approved ? "✅ Approved" : "🚫 Discarded") + by + ".";
            case GONE -> switch (approvalResolutionLookup.describe(resumeId)) {
                case RESOLVED -> "ℹ️ This approval was already resolved.";
                case EXPIRED -> "⌛ This approval expired before it was resolved.";
                case UNAVAILABLE -> "⚠️ This approval is no longer available.";
            };
            case INVALID_ID, JOB_FAILED, NOT_YET_SUSPENDED, STREAMING_NOT_ALLOWED ->
                "⚠️ This approval is no longer available.";
        };

        return new Response(200, updateMessage(text));
    }

    private boolean verifySignature(String signatureHex, String timestamp, String body) {
        PublicKey publicKey = resolvePublicKey();

        if (publicKey == null) {
            return false;
        }

        try {
            Signature signature = Signature.getInstance("Ed25519");

            signature.initVerify(publicKey);
            signature.update((timestamp + body).getBytes(StandardCharsets.UTF_8));

            HexFormat hexFormat = HexFormat.of();

            return signature.verify(hexFormat.parseHex(signatureHex.trim()));
        } catch (Exception exception) {
            if (log.isDebugEnabled()) {
                log.debug("Discord signature verification failed: {}", exception.getMessage());
            }

            return false;
        }
    }

    private @Nullable PublicKey resolvePublicKey() {
        if (publicKeyHex == null || publicKeyHex.isBlank()) {
            warnInvalidPublicKeyOnce("Rejecting Discord interactions: no Discord application public key is configured");

            return null;
        }

        try {
            HexFormat hexFormat = HexFormat.of();

            byte[] publicKeyBytes = hexFormat.parseHex(publicKeyHex.trim());

            byte[] der = new byte[ED25519_DER_PREFIX.length + publicKeyBytes.length];

            System.arraycopy(ED25519_DER_PREFIX, 0, der, 0, ED25519_DER_PREFIX.length);
            System.arraycopy(publicKeyBytes, 0, der, ED25519_DER_PREFIX.length, publicKeyBytes.length);

            KeyFactory keyFactory = KeyFactory.getInstance("Ed25519");

            return keyFactory.generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception exception) {
            Class<? extends Exception> exceptionClass = exception.getClass();

            warnInvalidPublicKeyOnce(
                "Rejecting Discord interactions: the configured Discord application public key is not a valid " +
                    "hex-encoded Ed25519 key (" + exceptionClass.getSimpleName() + ")");

            return null;
        }
    }

    private void warnInvalidPublicKeyOnce(String message) {
        if (invalidPublicKeyWarned.compareAndSet(false, true)) {
            log.warn(message);
        }
    }

    private @Nullable String resolveTenantId(String resumeId) {
        String innerToken = approvalTokens == null
            ? resumeId
            : approvalTokens.resolveInnerToken(resumeId)
                .orElse(null);

        if (innerToken == null) {
            return null;
        }

        try {
            return JobResumeId.parse(innerToken)
                .getTenantId();
        } catch (Exception exception) {
            return null;
        }
    }

    private static Map<String, Object> ephemeralReply(String content) {
        return Map.of("type", 4, "data", Map.of("content", content, "flags", EPHEMERAL_FLAG));
    }

    private static Map<String, Object> updateMessage(String content) {
        return Map.of("type", 7, "data", Map.of("content", content, "components", List.of()));
    }

    private static @Nullable String asApprovedBy(@Nullable String userName) {
        return userName == null || userName.isBlank() ? null : "@" + userName;
    }

    private static @Nullable Map<String, ?> parse(String rawBody) {
        try {
            return JsonUtils.readMap(rawBody);
        } catch (Exception exception) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static @Nullable String extractUserName(Map<String, ?> payload) {
        Map<String, ?> member = asMap(payload.get("member"));

        Map<String, ?> user = member != null ? asMap(member.get("user")) : asMap(payload.get("user"));

        return user == null ? null : (String) user.get("username");
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Map<String, ?> asMap(@Nullable Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, ?>) map : null;
    }
}
