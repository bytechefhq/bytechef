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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

/**
 * Handles Slack interactivity callbacks that resolve an approval in place. The Slack approval channel sends in-place
 * Approve/Discard buttons only when the connection carries the app's signing secret; this handler verifies the
 * request's {@code X-Slack-Signature} against every Slack connection carrying a signing secret in the tenant anchored
 * by the tokenized resume id, resolves the approval through {@link JobResumeFacade}, and rewrites the originating
 * message via the payload's {@code response_url}.
 *
 * <p>
 * Two callback shapes are handled. A {@code block_actions} callback fires when a button is clicked: Approve resolves
 * immediately; Discard first tries to open a comment modal ({@code views.open}, requiring the {@code views:write} scope
 * on the connection's bot token) so the reviewer can attach an optional reason, falling back to an immediate resolution
 * when no modal can be opened. A {@code view_submission} callback fires when that modal is submitted, carrying the
 * resume id and the original {@code response_url} in the view's {@code private_metadata}.
 * </p>
 *
 * <p>
 * An unverifiable request is rejected without acting — the resume id alone is a capability, but acting on an unsigned
 * interactivity callback would let anyone who saw a resume id forge resolutions attributed to Slack users.
 * </p>
 *
 * @author Ivica Cardic
 */
public class SlackInteractivityHandler {

    static final String ACTION_APPROVE = "approval_approve";
    static final String ACTION_DISCARD = "approval_discard";
    static final String CALLBACK_DISCARD_COMMENT = "approval_discard_comment";

    private static final String COMMENT_ACTION_ID = "comment";
    private static final String COMMENT_BLOCK_ID = "comment_block";
    private static final long TIMESTAMP_TOLERANCE_SECONDS = 300;
    private static final String VIEWS_OPEN_URL = "https://slack.com/api/views.open";

    private static final Logger log = LoggerFactory.getLogger(SlackInteractivityHandler.class);

    private final ConnectionService connectionService;
    private final JobResumeFacade jobResumeFacade;
    private final @Nullable ApprovalTokens approvalTokens;
    private final RestClient restClient;

    @SuppressFBWarnings("EI")
    public SlackInteractivityHandler(
        ConnectionService connectionService, JobResumeFacade jobResumeFacade,
        @Nullable ApprovalTokens approvalTokens, RestClient restClient) {

        this.connectionService = connectionService;
        this.jobResumeFacade = jobResumeFacade;
        this.approvalTokens = approvalTokens;
        this.restClient = restClient;
    }

    public enum Result {
        HANDLED, IGNORED, UNAUTHORIZED
    }

    public Result handle(String rawBody, @Nullable String timestamp, @Nullable String signature) {
        Map<String, ?> payload = parsePayload(rawBody);

        if (payload == null) {
            return Result.IGNORED;
        }

        Object type = payload.get("type");

        if (Objects.equals(type, "block_actions")) {
            return handleBlockActions(payload, rawBody, timestamp, signature);
        }

        if (Objects.equals(type, "view_submission")) {
            return handleViewSubmission(payload, rawBody, timestamp, signature);
        }

        return Result.IGNORED;
    }

    private Result handleBlockActions(
        Map<String, ?> payload, String rawBody, @Nullable String timestamp, @Nullable String signature) {

        Map<String, ?> action = firstAction(payload);

        if (action == null) {
            return Result.IGNORED;
        }

        String actionId = (String) action.get("action_id");

        boolean approved;

        if (ACTION_APPROVE.equals(actionId)) {
            approved = true;
        } else if (ACTION_DISCARD.equals(actionId)) {
            approved = false;
        } else {
            return Result.IGNORED;
        }

        String resumeId = (String) action.get("value");

        Connection connection = verify(resumeId, rawBody, timestamp, signature);

        if (connection == UNVERIFIED) {
            return Result.UNAUTHORIZED;
        }

        if (connection == UNPARSEABLE) {
            return Result.IGNORED;
        }

        String responseUrl = (String) payload.get("response_url");

        // Discard-with-comment: try to open a modal so the reviewer can attach an optional reason. When no modal can
        // be opened (no trigger id, no bot token, missing scope, or the API call fails), fall back to resolving right
        // away so a discard is never silently lost.
        if (!approved) {
            String triggerId = (String) payload.get("trigger_id");

            if (triggerId != null && openDiscardCommentModal(connection, triggerId, resumeId, responseUrl)) {
                return Result.HANDLED;
            }
        }

        // The Slack user is verified by the same signature check above, so it is a trustworthy resolver identity.
        String userName = extractUserName(payload);

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(
            resumeId, Map.of("approved", approved), asApprovedBy(userName));

        rewriteMessage(responseUrl, outcome, approved, userName);

        return Result.HANDLED;
    }

    private Result handleViewSubmission(
        Map<String, ?> payload, String rawBody, @Nullable String timestamp, @Nullable String signature) {

        Map<String, ?> view = asMap(payload.get("view"));

        if (view == null || !CALLBACK_DISCARD_COMMENT.equals(view.get("callback_id"))) {
            return Result.IGNORED;
        }

        Map<String, ?> privateMetadata = parsePrivateMetadata((String) view.get("private_metadata"));

        if (privateMetadata == null) {
            return Result.IGNORED;
        }

        String resumeId = (String) privateMetadata.get("resumeId");

        Connection connection = verify(resumeId, rawBody, timestamp, signature);

        if (connection == UNVERIFIED) {
            return Result.UNAUTHORIZED;
        }

        if (connection == UNPARSEABLE) {
            return Result.IGNORED;
        }

        String comment = extractComment(view);
        String userName = extractUserName(payload);

        Map<String, Object> data = new LinkedHashMap<>();

        data.put("approved", false);

        if (comment != null && !comment.isBlank()) {
            data.put("comment", comment);
        }

        JobResumeOutcome outcome = jobResumeFacade.resumeJob(resumeId, data, asApprovedBy(userName));

        rewriteMessage((String) privateMetadata.get("responseUrl"), outcome, false, userName);

        return Result.HANDLED;
    }

    // Sentinel connections distinguishing "resume id could not be parsed" (ignore) from "signature could not be
    // verified" (unauthorized) without a nullable-plus-enum return.
    private static final Connection UNPARSEABLE = new Connection();
    private static final Connection UNVERIFIED = new Connection();

    /**
     * Parses and tenant-anchors the resume id, then verifies the request signature against that tenant's Slack
     * connections. Returns the matching {@link Connection} on success, or the {@link #UNPARSEABLE} /
     * {@link #UNVERIFIED} sentinel so the caller can map the failure to {@code IGNORED} / {@code UNAUTHORIZED}
     * respectively.
     */
    private Connection verify(
        @Nullable String resumeId, String rawBody, @Nullable String timestamp, @Nullable String signature) {

        if (resumeId == null) {
            return UNPARSEABLE;
        }

        // The button value is the form-URL tail, which is an HMAC-SIGNED token when a signer is configured (the
        // default). Unwrap it to the raw inner token before parsing the tenant/job id — JobResumeId.parse only
        // understands the inner token. The full (possibly signed) value is still handed to resumeJob, which
        // re-resolves it.
        String innerToken = approvalTokens == null
            ? resumeId
            : approvalTokens.resolveInnerToken(resumeId)
                .orElse(null);

        if (innerToken == null) {
            return UNPARSEABLE;
        }

        JobResumeId jobResumeId;

        try {
            jobResumeId = JobResumeId.parse(innerToken);
        } catch (Exception exception) {
            return UNPARSEABLE;
        }

        if (!isTimestampFresh(timestamp) || signature == null) {
            return UNVERIFIED;
        }

        Connection connection = TenantContext.callWithTenantId(
            jobResumeId.getTenantId(), () -> findVerifiedConnection(rawBody, timestamp, signature));

        if (connection == null) {
            log.warn("Rejected Slack interactivity callback with an unverifiable signature");

            return UNVERIFIED;
        }

        return connection;
    }

    private static @Nullable String asApprovedBy(@Nullable String userName) {
        return userName == null ? null : "@" + userName;
    }

    private static @Nullable Map<String, ?> parsePayload(String rawBody) {
        for (String parameter : rawBody.split("&")) {
            if (!parameter.startsWith("payload=")) {
                continue;
            }

            String json = URLDecoder.decode(parameter.substring("payload=".length()), StandardCharsets.UTF_8);

            try {
                return JsonUtils.readMap(json);
            } catch (Exception exception) {
                return null;
            }
        }

        return null;
    }

    private static @Nullable Map<String, ?> parsePrivateMetadata(@Nullable String privateMetadata) {
        if (privateMetadata == null || privateMetadata.isBlank()) {
            return null;
        }

        try {
            return JsonUtils.readMap(privateMetadata);
        } catch (Exception exception) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Map<String, ?> firstAction(Map<String, ?> payload) {
        Object actions = payload.get("actions");

        if (actions instanceof List<?> actionList && !actionList.isEmpty()
            && actionList.getFirst() instanceof Map<?, ?> action) {

            return (Map<String, ?>) action;
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Map<String, ?> asMap(@Nullable Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, ?>) map : null;
    }

    @SuppressWarnings("unchecked")
    private static @Nullable String extractUserName(Map<String, ?> payload) {
        if (payload.get("user") instanceof Map<?, ?> user) {
            Map<String, ?> userMap = (Map<String, ?>) user;

            return (String) (userMap.get("username") != null ? userMap.get("username") : userMap.get("name"));
        }

        return null;
    }

    /**
     * Reads the reviewer comment out of a submitted modal's state:
     * {@code view.state.values.comment_block.comment.value}.
     */
    private static @Nullable String extractComment(Map<String, ?> view) {
        Map<String, ?> state = asMap(view.get("state"));

        if (state == null) {
            return null;
        }

        Map<String, ?> values = asMap(state.get("values"));

        if (values == null) {
            return null;
        }

        Map<String, ?> commentBlock = asMap(values.get(COMMENT_BLOCK_ID));

        if (commentBlock == null) {
            return null;
        }

        Map<String, ?> commentInput = asMap(commentBlock.get(COMMENT_ACTION_ID));

        return commentInput == null ? null : (String) commentInput.get("value");
    }

    private static boolean isTimestampFresh(@Nullable String timestamp) {
        if (timestamp == null) {
            return false;
        }

        long timestampSeconds;

        try {
            timestampSeconds = Long.parseLong(timestamp.trim());
        } catch (NumberFormatException numberFormatException) {
            return false;
        }

        Instant now = Instant.now();

        return Math.abs(now.getEpochSecond() - timestampSeconds) <= TIMESTAMP_TOLERANCE_SECONDS;
    }

    /**
     * Finds the current tenant's Slack connection whose signing secret verifies the request signature
     * ({@code v0=hex(HMAC-SHA256(secret, "v0:{timestamp}:{rawBody}"))}). Constant-time comparison; the first match
     * wins. Returns {@code null} when no connection verifies the request.
     */
    private @Nullable Connection findVerifiedConnection(String rawBody, String timestamp, String signature) {
        String baseString = "v0:" + timestamp + ":" + rawBody;

        byte[] signatureBytes = signature.getBytes(StandardCharsets.UTF_8);

        for (PlatformType platformType : PlatformType.values()) {
            try {
                for (Connection connection : connectionService.getConnections("slack", 1, platformType)) {
                    Object signingSecret = connection.getParameters()
                        .get("signingSecret");

                    if (signingSecret instanceof String signingSecretString && !signingSecretString.isBlank()) {
                        String expected = "v0=" + hmacSha256Hex(signingSecretString, baseString);

                        if (MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signatureBytes)) {
                            return connection;
                        }
                    }
                }
            } catch (Exception exception) {
                if (log.isDebugEnabled()) {
                    log.debug("Could not enumerate {} Slack connections: {}", platformType, exception.getMessage());
                }
            }
        }

        return null;
    }

    private static String hmacSha256Hex(String secret, String baseString) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

            HexFormat hexFormat = HexFormat.of();

            return hexFormat.formatHex(mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to compute the Slack request signature", exception);
        }
    }

    /**
     * Opens the discard-comment modal through {@code views.open} using the verifying connection's bot token. Returns
     * {@code true} only when Slack acknowledges the open ({@code ok: true}); any missing token, missing scope, or
     * transport error returns {@code false} so the caller falls back to an immediate resolution.
     */
    private boolean openDiscardCommentModal(
        Connection connection, String triggerId, @Nullable String resumeId, @Nullable String responseUrl) {

        Object botToken = connection.getParameters()
            .get("access_token");

        if (!(botToken instanceof String botTokenString) || botTokenString.isBlank()) {
            return false;
        }

        Map<String, Object> privateMetadata = new LinkedHashMap<>();

        privateMetadata.put("resumeId", resumeId);
        privateMetadata.put("responseUrl", responseUrl == null ? "" : responseUrl);

        Map<String, Object> requestBody = Map.of(
            "trigger_id", triggerId, "view", buildDiscardCommentView(JsonUtils.write(privateMetadata)));

        try {
            Map<String, ?> response = restClient.post()
                .uri(VIEWS_OPEN_URL)
                .header("Authorization", "Bearer " + botTokenString)
                .body(requestBody)
                .retrieve()
                .body(Map.class);

            return response != null && Boolean.TRUE.equals(response.get("ok"));
        } catch (Exception exception) {
            log.warn("Could not open the Slack discard-comment modal: {}", exception.getMessage());

            return false;
        }
    }

    private static Map<String, Object> buildDiscardCommentView(String privateMetadata) {
        return Map.of(
            "type", "modal",
            "callback_id", CALLBACK_DISCARD_COMMENT,
            "private_metadata", privateMetadata,
            "title", plainText("Discard approval"),
            "submit", plainText("Discard"),
            "close", plainText("Cancel"),
            "blocks", List.of(
                Map.of(
                    "type", "input",
                    "block_id", COMMENT_BLOCK_ID,
                    "optional", true,
                    "label", plainText("Comment (optional)"),
                    "element", Map.of(
                        "type", "plain_text_input",
                        "action_id", COMMENT_ACTION_ID,
                        "multiline", true))));
    }

    private static Map<String, Object> plainText(String text) {
        return Map.of("type", "plain_text", "text", text);
    }

    /**
     * Rewrites the originating Slack message through the {@code response_url} so the channel shows the outcome and the
     * buttons stop being actionable. Best-effort — a rewrite failure never fails the resolution.
     */
    private void rewriteMessage(
        @Nullable String responseUrl, JobResumeOutcome outcome, boolean approved, @Nullable String userName) {

        if (responseUrl == null || responseUrl.isBlank()) {
            return;
        }

        String by = userName == null ? "" : " by @" + userName;

        String text = switch (outcome) {
            case OK -> (approved ? ":white_check_mark: Approved" : ":no_entry_sign: Discarded") + by + ".";
            case GONE -> ":hourglass: This approval expired before it was resolved.";
            case INVALID_ID -> ":warning: This approval is no longer available.";
        };

        try {
            restClient.post()
                .uri(responseUrl)
                .body(Map.of("replace_original", true, "text", text))
                .retrieve()
                .toBodilessEntity();
        } catch (Exception exception) {
            log.warn("Could not rewrite the Slack approval message: {}", exception.getMessage());
        }
    }
}
