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

package com.bytechef.component.telegram.cluster;

import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.APPROVAL_CHANNELS;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.EXPIRES_AT;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.FORM_DESCRIPTION;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.FORM_TITLE;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.INPUTS;
import static com.bytechef.component.telegram.constant.TelegramConstants.CHAT_ID;
import static com.bytechef.component.telegram.constant.TelegramConstants.TEXT;
import static com.bytechef.component.telegram.constant.TelegramConstants.WEBHOOK_SECRET_TOKEN;

import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableClusterElementDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.definition.approval.ApprovalChannelFunction;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Approval channel that sends the request to a Telegram chat through the connected bot. Field-less approvals get
 * one-click Approve/Discard inline-keyboard buttons (the hosted form pre-selects the decision via the {@code approved}
 * query parameter and confirms with a single click); approvals with form fields get a single "Open Approval Form"
 * button.
 *
 * @author Ivica Cardic
 */
public class TelegramApprovalChannel {

    private static final Logger log = LoggerFactory.getLogger(TelegramApprovalChannel.class);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    public static final ModifiableClusterElementDefinition<ApprovalChannelFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<ApprovalChannelFunction>clusterElement("telegram")
            .title("Telegram")
            .description("Sends an approval request message to a Telegram chat.")
            .type(APPROVAL_CHANNELS)
            .properties(
                string(CHAT_ID)
                    .label("Chat ID")
                    .description(
                        "Unique identifier for the target chat or username of the target channel. Your bot has to " +
                            "be a member of that chat or group.")
                    .required(true))
            .object(() -> TelegramApprovalChannel::perform);

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static Object perform(
        Parameters inputParameters, Parameters connectionParameters, String formUrl, ClusterElementContext context) {

        List<Map<String, ?>> inputs = inputParameters.getList(INPUTS, new TypeReference<>() {}, List.of());

        String text = buildSummaryText(inputParameters);

        Http.Body body;

        if (formUrl == null || formUrl.isBlank()) {
            body = Http.Body.of(
                CHAT_ID, inputParameters.getRequiredString(CHAT_ID),
                TEXT, text + "\nThe approval form link is unavailable because no public URL is configured.");
        } else {
            String webhookSecretToken = connectionParameters.getString(WEBHOOK_SECRET_TOKEN);

            boolean inPlace = webhookSecretToken != null && !webhookSecretToken.isBlank();

            String shortId = inputs.isEmpty() && inPlace && formUrl.contains("/resume/")
                ? mintShortId(formUrl)
                : null;

            List<List<Map<String, String>>> inlineKeyboard;

            if (shortId != null) {
                // In-place callback buttons: the tap resolves the approval through /telegram/interactivity without
                // leaving Telegram. callback_data is capped at 64 bytes, so it carries a short id (minted server-side)
                // rather than the token; the hosted-form link stays in the text as a durable fallback in case the
                // short id is lost (e.g. a coordinator restart).
                inlineKeyboard = List.of(
                    List.of(
                        callbackButton("Approve", shortId + ":a"),
                        callbackButton("Discard", shortId + ":d")));

                text = text + "\nApprove or Discard below, or open the form: " + formUrl;
            } else if (inputs.isEmpty()) {
                inlineKeyboard = List.of(
                    List.of(
                        urlButton("Approve", formUrl + "?approved=true"),
                        urlButton("Discard", formUrl + "?approved=false")));
            } else {
                inlineKeyboard = List.of(List.of(urlButton("Open Approval Form", formUrl)));
            }

            body = Http.Body.of(
                CHAT_ID, inputParameters.getRequiredString(CHAT_ID),
                TEXT, text,
                "reply_markup", Map.of("inline_keyboard", inlineKeyboard));
        }

        return context
            .http(http -> http.post("/sendMessage"))
            .body(body)
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    /**
     * Mints a short id for the resume token (the {@code formUrl} tail) via the ByteChef short-token endpoint, derived
     * from the public URL in {@code formUrl}. Returns {@code null} on any failure so the caller falls back to the
     * hosted-form URL buttons — in-place delivery is never allowed to fail the approval.
     */
    private static @Nullable String mintShortId(String formUrl) {
        String publicUrl = formUrl.substring(0, formUrl.indexOf("/resume/"));
        String token = formUrl.substring(formUrl.lastIndexOf('/') + 1);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(publicUrl + "/approval/short-token"))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(token, StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String shortId = response.body()
                    .trim();

                return shortId.isBlank() ? null : shortId;
            }
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();
        } catch (Exception exception) {
            log.warn("Could not mint a Telegram approval short id; falling back to form links: {}",
                exception.getMessage());
        }

        return null;
    }

    private static Map<String, String> urlButton(String label, String url) {
        return Map.of(TEXT, label, "url", url);
    }

    private static Map<String, String> callbackButton(String label, String callbackData) {
        return Map.of(TEXT, label, "callback_data", callbackData);
    }

    private static String buildSummaryText(Parameters inputParameters) {
        StringBuilder builder = new StringBuilder();

        String formTitle = inputParameters.getString(FORM_TITLE);

        if (formTitle != null && !formTitle.isBlank()) {
            builder.append(formTitle.trim());
        }

        String formDescription = inputParameters.getString(FORM_DESCRIPTION);

        if (formDescription != null && !formDescription.isBlank()) {
            if (!builder.isEmpty()) {
                builder.append("\n");
            }

            builder.append(formDescription.trim());
        }

        if (builder.isEmpty()) {
            builder.append("You have a new approval request.");
        }

        String expiresAt = inputParameters.getString(EXPIRES_AT);

        if (expiresAt != null && !expiresAt.isBlank()) {
            builder.append("\nExpires: ")
                .append(expiresAt);
        }

        return builder.toString();
    }
}
