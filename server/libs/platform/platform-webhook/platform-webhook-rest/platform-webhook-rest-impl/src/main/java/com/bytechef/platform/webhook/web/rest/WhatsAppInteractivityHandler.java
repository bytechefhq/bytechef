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
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * @author Ivica Cardic
 */
public class WhatsAppInteractivityHandler {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppInteractivityHandler.class);

    private static final String GRAPH_API_URL = "https://graph.facebook.com/v17.0/";

    private static final String ALREADY_RESOLVED_TEXT = "ℹ️ This approval was already resolved.";
    private static final String EXPIRED_TEXT = "⌛ This approval expired before it was resolved.";
    private static final String NO_LONGER_AVAILABLE_TEXT = "⚠️ This approval is no longer available.";
    private static final String NOT_YET_SUSPENDED_TEXT = "Still preparing — try again in a moment.";

    private static final Connection UNAVAILABLE = new Connection();

    private final ApprovalResolutionLookup approvalResolutionLookup;
    private final ConnectionService connectionService;
    private final JobResumeFacade jobResumeFacade;
    private final @Nullable ApprovalTokens approvalTokens;
    private final RestClient restClient;

    @SuppressFBWarnings("EI")
    public WhatsAppInteractivityHandler(
        ApprovalResolutionLookup approvalResolutionLookup, ConnectionService connectionService,
        JobResumeFacade jobResumeFacade, @Nullable ApprovalTokens approvalTokens, RestClient restClient) {

        this.approvalResolutionLookup = approvalResolutionLookup;
        this.connectionService = connectionService;
        this.jobResumeFacade = jobResumeFacade;
        this.approvalTokens = approvalTokens;
        this.restClient = restClient;
    }

    public enum Result {
        HANDLED, IGNORED, UNAUTHORIZED, UNAVAILABLE
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

        Connection connection = signature == null ? null : findVerifiedConnection(tenantId, rawBody, signature);

        if (connection == UNAVAILABLE) {
            return Result.UNAVAILABLE;
        }

        if (connection == null) {
            log.warn("Rejected WhatsApp interactivity callback with an unverifiable signature");

            return Result.UNAUTHORIZED;
        }

        String userName = extractContactName(payload);

        JobResumeOutcome outcome = TenantContext.callWithTenantId(
            tenantId, () -> jobResumeFacade.resumeJob(resumeId, Map.of("approved", approved), asApprovedBy(userName)));

        if (outcome != JobResumeOutcome.OK) {
            log.warn("WhatsApp in-place approval resolution returned {}", outcome);

            reply(connection, payload, toReplyText(resumeId, outcome));
        }

        return Result.HANDLED;
    }

    private String toReplyText(String resumeId, JobResumeOutcome outcome) {
        return switch (outcome) {
            case GONE -> switch (approvalResolutionLookup.describe(resumeId)) {
                case RESOLVED -> ALREADY_RESOLVED_TEXT;
                case EXPIRED -> EXPIRED_TEXT;
                case UNAVAILABLE -> NO_LONGER_AVAILABLE_TEXT;
            };
            case NOT_YET_SUSPENDED -> NOT_YET_SUSPENDED_TEXT;
            case OK, INVALID_ID, JOB_FAILED, STREAMING_NOT_ALLOWED -> NO_LONGER_AVAILABLE_TEXT;
        };
    }

    private void reply(Connection connection, Map<String, ?> payload, String text) {
        Map<String, ?> parameters = connection.getParameters();

        Object accessToken = parameters.get("systemUserAccessToken");
        Object phoneNumberId = parameters.get("phoneNumberId");
        String recipient = extractSender(payload);

        if (!(accessToken instanceof String accessTokenString) || accessTokenString.isBlank() ||
            !(phoneNumberId instanceof String phoneNumberIdString) || phoneNumberIdString.isBlank() ||
            recipient == null) {

            log.warn("Could not reply to the WhatsApp approver: the connection or the callback lacks reply details");

            return;
        }

        try {
            restClient.post()
                .uri(GRAPH_API_URL + phoneNumberIdString + "/messages")
                .header("Authorization", "Bearer " + accessTokenString)
                .body(
                    Map.of(
                        "messaging_product", "whatsapp", "recipient_type", "individual", "to", recipient,
                        "type", "text", "text", Map.of("body", text)))
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientResponseException restClientResponseException) {
            log.warn(
                "Could not reply to the WhatsApp approver; Graph answered {}",
                restClientResponseException.getStatusCode());
        } catch (Exception exception) {
            Class<? extends Exception> exceptionClass = exception.getClass();

            log.warn("Could not reply to the WhatsApp approver: {}", exceptionClass.getName());
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

    private @Nullable Connection findVerifiedConnection(String tenantId, String rawBody, String signature) {
        return TenantContext.callWithTenantId(tenantId, () -> {
            byte[] signatureBytes = signature.getBytes(StandardCharsets.UTF_8);
            boolean enumerationFailed = false;

            for (PlatformType platformType : PlatformType.values()) {
                try {
                    for (Connection connection : connectionService.getConnections("whatsApp", 1, platformType)) {
                        Object appSecret = connection.getParameters()
                            .get("appSecret");

                        if (appSecret instanceof String appSecretString && !appSecretString.isBlank()) {
                            String expected = "sha256=" + hmacSha256Hex(appSecretString, rawBody);

                            if (MessageDigest.isEqual(
                                expected.getBytes(StandardCharsets.UTF_8), signatureBytes)) {

                                return connection;
                            }
                        }
                    }
                } catch (Exception exception) {
                    log.warn("Could not enumerate {} WhatsApp connections", platformType, exception);

                    enumerationFailed = true;
                }
            }

            return enumerationFailed ? UNAVAILABLE : null;
        });
    }

    private static String hmacSha256Hex(String secret, String rawBody) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

            HexFormat hexFormat = HexFormat.of();

            return hexFormat.formatHex(mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8)));
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

    private static @Nullable String extractSender(Map<String, ?> payload) {
        Map<String, ?> value = firstChangeValue(payload);

        Map<String, ?> message = value == null ? null : firstOfList(value.get("messages"));

        Object from = message == null ? null : message.get("from");

        return from instanceof String fromString && !fromString.isBlank() ? fromString : null;
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
