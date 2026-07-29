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

package com.bytechef.component.mattermost.cluster;

import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.APPROVAL_CHANNELS;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.EXPIRES_AT;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.FORM_DESCRIPTION;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.FORM_TITLE;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.INPUTS;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableClusterElementDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.definition.approval.ApprovalChannelFunction;
import com.bytechef.component.mattermost.util.MattermostUtils;
import java.util.List;
import java.util.Map;

/**
 * Approval channel that posts the request to a Mattermost channel. Field-less approvals get one-click Approve/Discard
 * markdown links (the hosted form pre-selects the decision via the {@code approved} query parameter and confirms with a
 * single click); approvals with form fields get a single "Open Approval Form" link.
 *
 * @author Ivica Cardic
 */
public class MattermostApprovalChannel {

    private static final String CHANNEL_ID = "channel_id";
    private static final String MESSAGE = "message";

    public static final ModifiableClusterElementDefinition<ApprovalChannelFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<ApprovalChannelFunction>clusterElement("mattermost")
            .title("Mattermost")
            .description("Sends an approval request message to a Mattermost channel.")
            .type(APPROVAL_CHANNELS)
            .properties(
                string(CHANNEL_ID)
                    .label("Channel ID")
                    .description("The channel to send the approval request to.")
                    .options((OptionsFunction<String>) MattermostUtils::getChannelIdOptions)
                    .required(true))
            .object(() -> MattermostApprovalChannel::perform);

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static Object perform(
        Parameters inputParameters, Parameters connectionParameters, String formUrl, ClusterElementContext context) {

        List<Map<String, ?>> inputs = inputParameters.getList(INPUTS, new TypeReference<>() {}, List.of());

        Http.Body body;

        if (inputs.isEmpty() && formUrl != null && !formUrl.isBlank() && formUrl.contains("/resume/")) {
            // Post interactive buttons that resolve the approval in place. Each button's integration URL points at the
            // Mattermost interactivity endpoint and carries the tokenized resume id in its context — the same
            // capability the hosted-form link carries, so this is no weaker than the one-click links it replaces
            // (Mattermost does not sign these callbacks, so the resolver identity is not trusted).
            String publicUrl = formUrl.substring(0, formUrl.indexOf("/resume/"));
            String resumeId = formUrl.substring(formUrl.lastIndexOf('/') + 1);
            String integrationUrl = publicUrl + "/mattermost/interactivity";

            body = Http.Body.of(
                CHANNEL_ID, inputParameters.getRequiredString(CHANNEL_ID),
                MESSAGE, buildSummaryText(inputParameters),
                "props", Map.of(
                    "attachments", List.of(
                        Map.of(
                            "actions", List.of(
                                actionButton("Approve", integrationUrl, resumeId, true),
                                actionButton("Discard", integrationUrl, resumeId, false))))));
        } else {
            body = Http.Body.of(
                CHANNEL_ID, inputParameters.getRequiredString(CHANNEL_ID),
                MESSAGE, buildMessageText(inputParameters, formUrl));
        }

        return context
            .http(http -> http.post("/posts"))
            .body(body)
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    private static Map<String, Object> actionButton(
        String name, String integrationUrl, String resumeId,
        boolean approved) {

        return Map.of(
            "name", name,
            "integration", Map.of(
                "url", integrationUrl,
                "context", Map.of("resumeId", resumeId, "approved", approved)));
    }

    private static String buildSummaryText(Parameters inputParameters) {
        StringBuilder builder = new StringBuilder();

        String formTitle = inputParameters.getString(FORM_TITLE);

        if (formTitle != null && !formTitle.isBlank()) {
            builder.append("**")
                .append(escapeMarkdown(formTitle.trim()))
                .append("**\n");
        }

        String formDescription = inputParameters.getString(FORM_DESCRIPTION);

        if (formDescription != null && !formDescription.isBlank()) {
            builder.append(escapeMarkdown(formDescription.trim()))
                .append("\n");
        }

        if (builder.isEmpty()) {
            builder.append("You have a new approval request.\n");
        }

        String expiresAt = inputParameters.getString(EXPIRES_AT);

        if (expiresAt != null && !expiresAt.isBlank()) {
            builder.append("Expires: ")
                .append(expiresAt)
                .append("\n");
        }

        return builder.toString();
    }

    private static String buildMessageText(Parameters inputParameters, String formUrl) {
        List<Map<String, ?>> inputs = inputParameters.getList(INPUTS, new TypeReference<>() {}, List.of());

        StringBuilder builder = new StringBuilder(buildSummaryText(inputParameters));

        if (formUrl == null || formUrl.isBlank()) {
            builder.append("\nThe approval form link is unavailable because no public URL is configured.");
        } else if (inputs.isEmpty()) {
            builder.append("\n[Approve](")
                .append(formUrl)
                .append("?approved=true) | [Discard](")
                .append(formUrl)
                .append("?approved=false)");
        } else {
            builder.append("\n[Open Approval Form](")
                .append(formUrl)
                .append(")");
        }

        return builder.toString();
    }

    /**
     * Escapes markdown-link-forming characters so caller-supplied text — for a gated tool the description embeds the
     * AI-chosen tool arguments verbatim — cannot forge clickable links next to the real Approve/Discard links.
     */
    static String escapeMarkdown(String text) {
        return text.replace("\\", "\\\\")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace("(", "\\(")
            .replace(")", "\\)");
    }
}
