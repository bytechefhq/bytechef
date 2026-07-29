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
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade.JobResumeOutcome;
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
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles Discord interaction webhooks that resolve an approval in place. The Discord approval channel sends
 * interaction buttons only when the connection carries the app public key; a button tap arrives here as a
 * {@code MESSAGE_COMPONENT} (type 3) interaction whose {@code custom_id} is {@code <shortId>:a|d}. The short id is
 * resolved back to the tokenized resume id through {@link ApprovalShortTokenStore}. Every request is authenticated by
 * the Ed25519 signature ({@code X-Signature-Ed25519}/{@code -Timestamp}) verified against the configured app public
 * key, including the {@code PING} (type 1) handshake Discord uses to register the Interactions Endpoint URL.
 *
 * @author Ivica Cardic
 */
public class DiscordInteractivityHandler {

    // DER SubjectPublicKeyInfo prefix for an Ed25519 (id-Ed25519, OID 1.3.101.112) 32-byte raw public key.
    private static final byte[] ED25519_DER_PREFIX = HexFormat.of()
        .parseHex("302a300506032b6570032100");

    private static final Logger log = LoggerFactory.getLogger(DiscordInteractivityHandler.class);

    private final ApprovalShortTokenStore approvalShortTokenStore;
    private final JobResumeFacade jobResumeFacade;
    private final @Nullable ApprovalTokens approvalTokens;
    private final @Nullable String publicKeyHex;

    @SuppressFBWarnings("EI")
    public DiscordInteractivityHandler(
        ApprovalShortTokenStore approvalShortTokenStore, JobResumeFacade jobResumeFacade,
        @Nullable ApprovalTokens approvalTokens, @Nullable String publicKeyHex) {

        this.approvalShortTokenStore = approvalShortTokenStore;
        this.jobResumeFacade = jobResumeFacade;
        this.approvalTokens = approvalTokens;
        this.publicKeyHex = publicKeyHex;
    }

    /**
     * @param statusCode HTTP status to return
     * @param body       interaction response body (null → empty)
     */
    // Every call site builds body from Map.of(...), which is already immutable; SpotBugs cannot see that through the
    // static Map type, so it still flags the accessor/constructor as exposing a mutable reference. Same suppression
    // as other ByteChef records carrying Maps (e.g. Violation.ClassifiedViolation).
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

        // PING → PONG (both type 1): the endpoint-registration handshake.
        if (type.intValue() == 1) {
            return new Response(200, Map.of("type", 1));
        }

        // MESSAGE_COMPONENT (type 3): a button was clicked.
        if (type.intValue() != 3) {
            return new Response(200, Map.of("type", 6));
        }

        Map<String, ?> data = asMap(payload.get("data"));

        Object customId = data == null ? null : data.get("custom_id");

        if (!(customId instanceof String customIdString) || customIdString.length() < 3) {
            return new Response(200, updateMessage("This approval request is malformed."));
        }

        boolean approved;

        if (customIdString.endsWith(":a")) {
            approved = true;
        } else if (customIdString.endsWith(":d")) {
            approved = false;
        } else {
            return new Response(200, updateMessage("This approval request is malformed."));
        }

        String resumeId = approvalShortTokenStore.resolve(customIdString.substring(0, customIdString.length() - 2));

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

        String by = userName == null ? "" : " by @" + userName;

        String text = switch (outcome) {
            case OK -> (approved ? "✅ Approved" : "🚫 Discarded") + by + ".";
            case GONE -> "⌛ This approval expired before it was resolved.";
            case INVALID_ID -> "⚠️ This approval is no longer available.";
        };

        return new Response(200, updateMessage(text));
    }

    private boolean verifySignature(String signatureHex, String timestamp, String body) {
        if (publicKeyHex == null || publicKeyHex.isBlank()) {
            return false;
        }

        try {
            byte[] publicKeyBytes = HexFormat.of()
                .parseHex(publicKeyHex.trim());

            byte[] der = new byte[ED25519_DER_PREFIX.length + publicKeyBytes.length];

            System.arraycopy(ED25519_DER_PREFIX, 0, der, 0, ED25519_DER_PREFIX.length);
            System.arraycopy(publicKeyBytes, 0, der, ED25519_DER_PREFIX.length, publicKeyBytes.length);

            PublicKey publicKey = KeyFactory.getInstance("Ed25519")
                .generatePublic(new X509EncodedKeySpec(der));

            Signature signature = Signature.getInstance("Ed25519");

            signature.initVerify(publicKey);
            signature.update((timestamp + body).getBytes(StandardCharsets.UTF_8));

            return signature.verify(HexFormat.of()
                .parseHex(signatureHex.trim()));
        } catch (Exception exception) {
            if (log.isDebugEnabled()) {
                log.debug("Discord signature verification failed: {}", exception.getMessage());
            }

            return false;
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

    private static Map<String, Object> updateMessage(String content) {
        // type 7 = UPDATE_MESSAGE: rewrites the message and drops the buttons.
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
