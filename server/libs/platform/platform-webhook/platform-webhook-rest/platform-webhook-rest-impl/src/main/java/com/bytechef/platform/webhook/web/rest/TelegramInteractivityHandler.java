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
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

/**
 * Handles Telegram {@code callback_query} webhooks that resolve an approval in place. The Telegram approval channel
 * sends inline-keyboard callback buttons only when the connection carries a webhook secret token; a button tap arrives
 * here with {@code callback_query.data = <shortId>:a|d}. The short id is resolved back to the tokenized resume id
 * through {@link ApprovalShortTokenStore}; the request is authenticated by matching the
 * {@code X-Telegram-Bot-Api-Secret-Token} header against the tenant's Telegram connections, then the approval is
 * resolved through {@link JobResumeFacade} and the originating message is rewritten via {@code editMessageText}.
 *
 * @author Ivica Cardic
 */
public class TelegramInteractivityHandler {

    private static final Logger log = LoggerFactory.getLogger(TelegramInteractivityHandler.class);

    private final ApprovalShortTokenStore approvalShortTokenStore;
    private final ConnectionService connectionService;
    private final JobResumeFacade jobResumeFacade;
    private final @Nullable ApprovalTokens approvalTokens;
    private final RestClient restClient;

    @SuppressFBWarnings("EI")
    public TelegramInteractivityHandler(
        ApprovalShortTokenStore approvalShortTokenStore, ConnectionService connectionService,
        JobResumeFacade jobResumeFacade, @Nullable ApprovalTokens approvalTokens, RestClient restClient) {

        this.approvalShortTokenStore = approvalShortTokenStore;
        this.connectionService = connectionService;
        this.jobResumeFacade = jobResumeFacade;
        this.approvalTokens = approvalTokens;
        this.restClient = restClient;
    }

    public enum Result {
        HANDLED, IGNORED, UNAUTHORIZED
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

        if (!(data instanceof String dataString) || dataString.length() < 3) {
            return Result.IGNORED;
        }

        boolean approved;

        if (dataString.endsWith(":a")) {
            approved = true;
        } else if (dataString.endsWith(":d")) {
            approved = false;
        } else {
            return Result.IGNORED;
        }

        String resumeId = approvalShortTokenStore.resolve(dataString.substring(0, dataString.length() - 2));

        if (resumeId == null) {
            // Unknown / expired short id (e.g. after a coordinator restart) — the message also carries the hosted-form
            // link, so the reviewer can still resolve there. Nothing actionable here.
            return Result.IGNORED;
        }

        String tenantId = resolveTenantId(resumeId);

        if (tenantId == null) {
            return Result.IGNORED;
        }

        Connection connection = TenantContext.callWithTenantId(
            tenantId, () -> findConnection(secretTokenHeader));

        if (connection == null) {
            log.warn("Rejected Telegram callback with an unverifiable secret token");

            return Result.UNAUTHORIZED;
        }

        String userName = extractUserName(callbackQuery);

        JobResumeOutcome outcome = TenantContext.callWithTenantId(
            tenantId,
            () -> jobResumeFacade.resumeJob(resumeId, Map.of("approved", approved), asApprovedBy(userName)));

        answerCallback(connection, callbackQuery);
        editMessage(connection, callbackQuery, outcome, approved, userName);

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
     * Finds the current tenant's Telegram connection whose webhook secret token matches the request header
     * (constant-time). Returns {@code null} when none matches.
     */
    private @Nullable Connection findConnection(@Nullable String secretTokenHeader) {
        if (secretTokenHeader == null || secretTokenHeader.isBlank()) {
            return null;
        }

        byte[] headerBytes = secretTokenHeader.getBytes(StandardCharsets.UTF_8);

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
                if (log.isDebugEnabled()) {
                    log.debug("Could not enumerate {} Telegram connections: {}", platformType, exception.getMessage());
                }
            }
        }

        return null;
    }

    private void answerCallback(Connection connection, Map<String, ?> callbackQuery) {
        Object callbackQueryId = callbackQuery.get("id");

        if (callbackQueryId == null) {
            return;
        }

        callTelegram(connection, "answerCallbackQuery", Map.of("callback_query_id", callbackQueryId));
    }

    private void editMessage(
        Connection connection, Map<String, ?> callbackQuery, JobResumeOutcome outcome, boolean approved,
        @Nullable String userName) {

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
            case GONE -> "⌛ This approval expired before it was resolved.";
            case INVALID_ID -> "⚠️ This approval is no longer available.";
        };

        callTelegram(
            connection, "editMessageText",
            Map.of("chat_id", chat.get("id"), "message_id", messageId, "text", text));
    }

    /**
     * Calls a Telegram Bot API method with the connection's bot token. Best-effort — a failure never fails the
     * resolution.
     */
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
        } catch (Exception exception) {
            log.warn("Telegram {} call failed: {}", method, exception.getMessage());
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
