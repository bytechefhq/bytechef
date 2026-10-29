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
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade.JobResumeOutcome;
import com.bytechef.platform.workflow.execution.service.ApprovalShortTokenService;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * @author Ivica Cardic
 */
public class TelegramInteractivityHandler {

    private static final Logger log = LoggerFactory.getLogger(TelegramInteractivityHandler.class);

    private static final Connection UNAVAILABLE = new Connection();

    private static final String NO_LONGER_AVAILABLE_TEXT = "This approval is no longer available.";
    private static final String NOT_YET_SUSPENDED_TEXT = "Still preparing — try again in a moment.";

    private final ApprovalResolutionLookup approvalResolutionLookup;
    private final ApprovalShortTokenService approvalShortTokenService;
    private final ConnectionService connectionService;
    private final JobResumeFacade jobResumeFacade;
    private final @Nullable ApprovalTokens approvalTokens;
    private final RestClient restClient;

    @SuppressFBWarnings("EI")
    public TelegramInteractivityHandler(
        ApprovalResolutionLookup approvalResolutionLookup, ApprovalShortTokenService approvalShortTokenService,
        ConnectionService connectionService, JobResumeFacade jobResumeFacade, @Nullable ApprovalTokens approvalTokens,
        RestClient restClient) {

        this.approvalResolutionLookup = approvalResolutionLookup;
        this.approvalShortTokenService = approvalShortTokenService;
        this.connectionService = connectionService;
        this.jobResumeFacade = jobResumeFacade;
        this.approvalTokens = approvalTokens;
        this.restClient = restClient;
    }

    public enum Result {
        HANDLED, IGNORED, UNAUTHORIZED, UNAVAILABLE
    }

    public Result handle(String rawBody, @Nullable String secretTokenHeader) {
        Map<String, ?> payload = parse(rawBody);

        if (payload == null) {
            return Result.IGNORED;
        }

        Map<String, ?> callbackQuery = asMap(payload.get("callback_query"));

        if (callbackQuery == null) {
            return Result.IGNORED;
        }

        Object data = callbackQuery.get("data");

        ApprovalButtonPayload approvalButtonPayload = data instanceof String dataString
            ? ApprovalButtonPayload.parse(dataString)
                .orElse(null)
            : null;

        if (approvalButtonPayload == null) {
            return Result.IGNORED;
        }

        boolean approved = approvalButtonPayload.approved();

        String resumeId = approvalShortTokenService.fetchToken(approvalButtonPayload.shortId())
            .orElse(null);

        if (resumeId == null) {
            log.info(
                "Telegram approval callback for an unknown or expired short id {}", approvalButtonPayload.shortId());

            answerNoLongerAvailable(callbackQuery, secretTokenHeader);

            return Result.IGNORED;
        }

        String tenantId = resolveTenantId(resumeId);

        if (tenantId == null) {
            log.warn(
                "Telegram approval callback for short id {} carries an unverifiable resume id",
                approvalButtonPayload.shortId());

            answerNoLongerAvailable(callbackQuery, secretTokenHeader);

            return Result.IGNORED;
        }

        Connection connection = TenantContext.callWithTenantId(
            tenantId, () -> findConnection(secretTokenHeader));

        if (connection == UNAVAILABLE) {
            return Result.UNAVAILABLE;
        }

        if (connection == null) {
            log.warn("Rejected Telegram callback with an unverifiable secret token");

            return Result.UNAUTHORIZED;
        }

        String userName = extractUserName(callbackQuery);

        JobResumeOutcome outcome = TenantContext.callWithTenantId(
            tenantId,
            () -> jobResumeFacade.resumeJob(resumeId, Map.of("approved", approved), asApprovedBy(userName)));

        if (outcome == JobResumeOutcome.NOT_YET_SUSPENDED) {
            answerCallback(connection, callbackQuery, NOT_YET_SUSPENDED_TEXT);

            return Result.HANDLED;
        }

        answerCallback(connection, callbackQuery, null);
        editMessage(connection, callbackQuery, resumeId, outcome, approved, userName);

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

    private @Nullable Connection findConnection(@Nullable String secretTokenHeader) {
        if (secretTokenHeader == null || secretTokenHeader.isBlank()) {
            return null;
        }

        byte[] headerBytes = secretTokenHeader.getBytes(StandardCharsets.UTF_8);
        boolean enumerationFailed = false;

        for (PlatformType platformType : PlatformType.values()) {
            try {
                for (Connection connection : connectionService.getConnections("telegram", 1, platformType)) {
                    Object secretToken = connection.getParameters()
                        .get("webhookSecretToken");

                    if (secretToken instanceof String secretTokenString && !secretTokenString.isBlank()
                        && MessageDigest.isEqual(secretTokenString.getBytes(StandardCharsets.UTF_8), headerBytes)) {

                        return connection;
                    }
                }
            } catch (Exception exception) {
                log.warn("Could not enumerate {} Telegram connections", platformType, exception);

                enumerationFailed = true;
            }
        }

        return enumerationFailed ? UNAVAILABLE : null;
    }

    private void answerNoLongerAvailable(Map<String, ?> callbackQuery, @Nullable String secretTokenHeader) {
        Connection connection = findConnection(secretTokenHeader);

        if (connection == null || connection == UNAVAILABLE) {
            return;
        }

        answerCallback(connection, callbackQuery, NO_LONGER_AVAILABLE_TEXT);
    }

    private void answerCallback(Connection connection, Map<String, ?> callbackQuery, @Nullable String text) {
        Object callbackQueryId = callbackQuery.get("id");

        if (callbackQueryId == null) {
            return;
        }

        Map<String, Object> body = text == null
            ? Map.of("callback_query_id", callbackQueryId)
            : Map.of("callback_query_id", callbackQueryId, "text", text);

        callTelegram(connection, "answerCallbackQuery", body);
    }

    private void editMessage(
        Connection connection, Map<String, ?> callbackQuery, String resumeId, JobResumeOutcome outcome,
        boolean approved, @Nullable String userName) {

        Map<String, ?> message = asMap(callbackQuery.get("message"));

        if (message == null) {
            return;
        }

        Map<String, ?> chat = asMap(message.get("chat"));

        Object messageId = message.get("message_id");

        if (chat == null || chat.get("id") == null || messageId == null) {
            return;
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

        callTelegram(
            connection, "editMessageText",
            Map.of("chat_id", chat.get("id"), "message_id", messageId, "text", text));
    }

    private void callTelegram(Connection connection, String method, Map<String, Object> body) {
        Object botToken = connection.getParameters()
            .get("botToken");

        if (!(botToken instanceof String botTokenString) || botTokenString.isBlank()) {
            return;
        }

        try {
            restClient.post()
                .uri("https://api.telegram.org/bot" + botTokenString + "/" + method)
                .body(body)
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientResponseException restClientResponseException) {
            log.warn(
                "Telegram {} call failed with status {}", method, restClientResponseException.getStatusCode());
        } catch (Exception exception) {
            Class<? extends Exception> exceptionClass = exception.getClass();

            log.warn("Telegram {} call failed: {}", method, exceptionClass.getName());
        }
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
    private static @Nullable String extractUserName(Map<String, ?> callbackQuery) {
        Map<String, ?> from = asMap(callbackQuery.get("from"));

        if (from == null) {
            return null;
        }

        Object username = from.get("username") != null ? from.get("username") : from.get("first_name");

        return username instanceof String usernameString ? usernameString : null;
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Map<String, ?> asMap(@Nullable Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, ?>) map : null;
    }
}
