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

package com.bytechef.component.slack.cluster;

import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.APPROVAL_CHANNELS;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.EXPIRES_AT;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.FORM_DESCRIPTION;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.FORM_TITLE;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.INPUTS;
import static com.bytechef.component.slack.constant.SlackConstants.CHANNEL;
import static com.bytechef.component.slack.constant.SlackConstants.SIGNING_SECRET;
import static com.bytechef.component.slack.constant.SlackConstants.TEXT;
import static com.bytechef.component.slack.constant.SlackConstants.TYPE;
import static com.bytechef.component.slack.util.SlackSendMessageUtils.sendMessage;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.definition.approval.ApprovalChannelFunction;
import com.bytechef.component.slack.util.SlackUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class SlackApprovalChannel {

    public static final ClusterElementDefinition<ApprovalChannelFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<ApprovalChannelFunction>clusterElement("slack")
            .title("Slack")
            .description("Sends an approval request message via Slack.")
            .type(APPROVAL_CHANNELS)
            .properties(
                string(CHANNEL)
                    .label("Channel")
                    .description("Channel, private group, or IM channel to send the approval request to.")
                    .options((OptionsFunction<String>) SlackUtils::getChannelIdOptions)
                    .required(true))
            .object(() -> SlackApprovalChannel::perform);

    private static Object perform(
        Parameters inputParameters, Parameters connectionParameters, String formUrl, ClusterElementContext context) {

        String channel = inputParameters.getRequiredString(CHANNEL);

        List<Map<String, ?>> inputs = inputParameters.getList(INPUTS, new TypeReference<>() {}, List.of());

        String signingSecret = connectionParameters.getString(SIGNING_SECRET);

        boolean inPlace = signingSecret != null && !signingSecret.isBlank();

        String text;
        List<Map<String, Object>> elements;

        if (inputs.isEmpty() && inPlace) {
            // The connection carries the Slack app's signing secret, so the app's interactivity Request URL can be
            // verified server-side: send in-place block_actions buttons that resolve the approval without leaving
            // Slack (see SlackInteractivityController). The button value is the tokenized resume id — the same
            // capability the hosted-form URL carries.
            String resumeId = formUrl.substring(formUrl.lastIndexOf('/') + 1);

            text = buildSummaryText(inputParameters);
            elements = List.of(
                Map.of(
                    TYPE, "button", TEXT, Map.of(TYPE, "plain_text", TEXT, "Approve"),
                    "style", "primary", "action_id", "approval_approve", "value", resumeId),
                Map.of(
                    TYPE, "button", TEXT, Map.of(TYPE, "plain_text", TEXT, "Discard"),
                    "style", "danger", "action_id", "approval_discard", "value", resumeId));
        } else if (inputs.isEmpty()) {
            text = buildSummaryText(inputParameters);
            elements = List.of(
                Map.of(
                    TYPE, "button", TEXT, Map.of(TYPE, "plain_text", TEXT, "Approve"),
                    "style", "primary", "url", formUrl + "?approved=true"),
                Map.of(
                    TYPE, "button", TEXT, Map.of(TYPE, "plain_text", TEXT, "Discard"),
                    "style", "danger", "url", formUrl + "?approved=false"));
        } else {
            text = buildSummaryText(inputParameters);
            elements = List.of(
                Map.of(
                    TYPE, "button", TEXT, Map.of(TYPE, "plain_text", TEXT, "Open Approval Form"),
                    "style", "primary", "url", formUrl));
        }

        List<Map<String, Object>> blocks = List.of(
            Map.of(
                TYPE, "section", TEXT,
                Map.of(TYPE, "mrkdwn", TEXT, text)),
            Map.of(
                TYPE, "actions", "block_id", "actions", "elements", elements));

        return sendMessage(channel, text, null, blocks, context);
    }

    private static String buildSummaryText(Parameters inputParameters) {
        String formTitle = inputParameters.getString(FORM_TITLE);
        String formDescription = inputParameters.getString(FORM_DESCRIPTION);

        StringBuilder builder = new StringBuilder();

        if (formTitle != null && !formTitle.isBlank()) {
            builder.append("*")
                .append(escapeMrkdwn(formTitle))
                .append("*");
        }

        if (formDescription != null && !formDescription.isBlank()) {
            if (!builder.isEmpty()) {
                builder.append("\n");
            }

            builder.append(escapeMrkdwn(formDescription));
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

    /**
     * Escapes Slack mrkdwn control characters so caller-supplied text — for a gated tool the description embeds the
     * AI-chosen tool arguments verbatim — cannot forge {@code <url|text>} links next to the real Approve/Discard
     * buttons.
     */
    static String escapeMrkdwn(String text) {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }
}
