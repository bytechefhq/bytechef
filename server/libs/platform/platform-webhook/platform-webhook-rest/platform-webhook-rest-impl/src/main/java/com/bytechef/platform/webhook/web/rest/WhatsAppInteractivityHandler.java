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
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade.JobResumeOutcome;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles WhatsApp Cloud API inbound webhooks that resolve an approval in place. The WhatsApp approval channel sends
 * in-place Approve/Discard reply buttons only when the connection carries the Meta app secret; a button tap arrives
 * here as an {@code interactive.button_reply} inbound message whose id is the decision-prefixed tokenized resume id
 * ({@code a:<token>} / {@code d:<token>}). This handler verifies the request's {@code X-Hub-Signature-256} HMAC against
 * every WhatsApp connection carrying an app secret in the tenant anchored by the resume id, resolves the approval
 * through {@link JobResumeFacade}, and echoes nothing (Meta expects only a 200).
 *
 * <p>
 * An unverifiable request is rejected without acting — the resume id alone is a capability, so acting on an unsigned
 * inbound webhook would let anyone who saw a resume id forge resolutions.
 * </p>
 *
 * @author Ivica Cardic
 */
public class WhatsAppInteractivityHandler {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppInteractivityHandler.class);

    private final ConnectionService connectionService;
    private final JobResumeFacade jobResumeFacade;
    private final @Nullable ApprovalTokens approvalTokens;

    @SuppressFBWarnings("EI")
    public WhatsAppInteractivityHandler(
        ConnectionService connectionService, JobResumeFacade jobResumeFacade, @Nullable ApprovalTokens approvalTokens) {

        this.connectionService = connectionService;
        this.jobResumeFacade = jobResumeFacade;
        this.approvalTokens = approvalTokens;
    }

    public enum Result {
        HANDLED, IGNORED, UNAUTHORIZED
    }

    public Result handle(String rawBody, @Nullable String signature) {
        Map<String, ?> payload = parse(rawBody);

        if (payload == null) {
            return Result.IGNORED;
        }

        Map<String, ?> buttonReply = extractButtonReply(payload);

        if (buttonReply == null) {
            return Result.IGNORED;
        }

        Object buttonId = buttonReply.get("id");

        if (!(buttonId instanceof String buttonIdString) || buttonIdString.length() < 3) {
            return Result.IGNORED;
        }

        boolean approved;

        if (buttonIdString.startsWith("a:")) {
            approved = true;
        } else if (buttonIdString.startsWith("d:")) {
            approved = false;
        } else {
            return Result.IGNORED;
        }

        String resumeId = buttonIdString.substring(2);

        String tenantId = resolveTenantId(resumeId);

        if (tenantId == null) {
            return Result.IGNORED;
        }

        if (signature == null || !verifySignature(tenantId, rawBody, signature)) {
            log.warn("Rejected WhatsApp interactivity callback with an unverifiable signature");

            return Result.UNAUTHORIZED;
        }

        String userName = extractContactName(payload);

        JobResumeOutcome outcome = TenantContext.callWithTenantId(
            tenantId, () -> jobResumeFacade.resumeJob(resumeId, Map.of("approved", approved), asApprovedBy(userName)));

        if (outcome != JobResumeOutcome.OK) {
            log.info("WhatsApp in-place approval resolution returned {}", outcome);
        }

        return Result.HANDLED;
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

    /**
     * Verifies {@code X-Hub-Signature-256: sha256=hex(HMAC-SHA256(appSecret, rawBody))} against the tenant's WhatsApp
     * connections carrying an app secret. Constant-time comparison; the first match wins.
     */
    private boolean verifySignature(String tenantId, String rawBody, String signature) {
        return Boolean.TRUE.equals(TenantContext.callWithTenantId(tenantId, () -> {
            byte[] signatureBytes = signature.getBytes(StandardCharsets.UTF_8);

            for (PlatformType platformType : PlatformType.values()) {
                try {
                    for (Connection connection : connectionService.getConnections("whatsApp", 1, platformType)) {
                        Object appSecret = connection.getParameters()
                            .get("appSecret");

                        if (appSecret instanceof String appSecretString && !appSecretString.isBlank()) {
                            String expected = "sha256=" + hmacSha256Hex(appSecretString, rawBody);

                            if (MessageDigest.isEqual(
                                expected.getBytes(StandardCharsets.UTF_8), signatureBytes)) {

                                return true;
                            }
                        }
                    }
                } catch (Exception exception) {
                    if (log.isDebugEnabled()) {
                        log.debug(
                            "Could not enumerate {} WhatsApp connections: {}", platformType, exception.getMessage());
                    }
                }
            }

            return false;
        }));
    }

    private static String hmacSha256Hex(String secret, String rawBody) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

            return java.util.HexFormat.of()
                .formatHex(mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to compute the WhatsApp request signature", exception);
        }
    }

    private static @Nullable String asApprovedBy(@Nullable String userName) {
        return userName == null || userName.isBlank() ? null : userName;
    }

    private static @Nullable Map<String, ?> parse(String rawBody) {
        try {
            return JsonUtils.readMap(rawBody);
        } catch (Exception exception) {
            return null;
        }
    }

    /**
     * Navigates {@code entry[0].changes[0].value.messages[0].interactive.button_reply}.
     */
    private static @Nullable Map<String, ?> extractButtonReply(Map<String, ?> payload) {
        Map<String, ?> value = firstChangeValue(payload);

        if (value == null) {
            return null;
        }

        Map<String, ?> message = firstOfList(value.get("messages"));

        if (message == null) {
            return null;
        }

        Map<String, ?> interactive = asMap(message.get("interactive"));

        return interactive == null ? null : asMap(interactive.get("button_reply"));
    }

    private static @Nullable String extractContactName(Map<String, ?> payload) {
        Map<String, ?> value = firstChangeValue(payload);

        if (value == null) {
            return null;
        }

        Map<String, ?> contact = firstOfList(value.get("contacts"));

        if (contact == null) {
            return null;
        }

        Map<String, ?> profile = asMap(contact.get("profile"));

        return profile == null ? null : (String) profile.get("name");
    }

    private static @Nullable Map<String, ?> firstChangeValue(Map<String, ?> payload) {
        Map<String, ?> entry = firstOfList(payload.get("entry"));

        if (entry == null) {
            return null;
        }

        Map<String, ?> change = firstOfList(entry.get("changes"));

        return change == null ? null : asMap(change.get("value"));
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Map<String, ?> firstOfList(@Nullable Object value) {
        if (value instanceof List<?> list && !list.isEmpty() && list.getFirst() instanceof Map<?, ?> map) {
            return (Map<String, ?>) map;
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Map<String, ?> asMap(@Nullable Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, ?>) map : null;
    }
}
