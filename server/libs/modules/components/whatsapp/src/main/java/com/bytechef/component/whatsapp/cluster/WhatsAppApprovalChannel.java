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

package com.bytechef.component.whatsapp.cluster;

import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.APPROVAL_CHANNELS;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.EXPIRES_AT;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.FORM_DESCRIPTION;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.FORM_TITLE;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.INPUTS;
import static com.bytechef.component.whatsapp.constant.WhatsAppConstants.APP_SECRET;
import static com.bytechef.component.whatsapp.constant.WhatsAppConstants.BODY;
import static com.bytechef.component.whatsapp.constant.WhatsAppConstants.MESSAGING_PRODUCT;
import static com.bytechef.component.whatsapp.constant.WhatsAppConstants.PHONE_NUMBER_ID;
import static com.bytechef.component.whatsapp.constant.WhatsAppConstants.RECEIVE_USER;
import static com.bytechef.component.whatsapp.constant.WhatsAppConstants.RECIPIENT_TYPE;
import static com.bytechef.component.whatsapp.constant.WhatsAppConstants.TEXT;
import static com.bytechef.component.whatsapp.constant.WhatsAppConstants.TYPE;
import static com.bytechef.component.whatsapp.constant.WhatsAppConstants.WHATS_APP;

import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.definition.approval.ApprovalChannelFunction;
import java.util.List;
import java.util.Map;

/**
 * Approval channel that delivers the request as a WhatsApp text message. Field-less approvals get one-click
 * Approve/Discard links (the hosted form auto-resolves via the {@code approved} query parameter); approvals with form
 * fields get the plain form link. Free-form WhatsApp messages only reach recipients inside an open 24-hour
 * customer-service window — outside it the delivery is silently dropped by Meta, so pair this channel with a fallback
 * for cold outreach.
 *
 * @author Ivica Cardic
 */
public class WhatsAppApprovalChannel {

    public static final ClusterElementDefinition<ApprovalChannelFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<ApprovalChannelFunction>clusterElement(WHATS_APP)
            .title("WhatsApp")
            .description("Sends an approval request message via WhatsApp.")
            .type(APPROVAL_CHANNELS)
            .properties(
                string(RECEIVE_USER)
                    .label("Send Message To")
                    .description("Phone number to send the approval request to. It must start with \"+\" sign.")
                    .required(true))
            .object(() -> WhatsAppApprovalChannel::perform);

    private static Object perform(
        Parameters inputParameters, Parameters connectionParameters, String formUrl, ClusterElementContext context) {

        List<Map<String, ?>> inputs = inputParameters.getList(INPUTS, new TypeReference<>() {}, List.of());

        String appSecret = connectionParameters.getString(APP_SECRET);

        boolean inPlace = appSecret != null && !appSecret.isBlank();

        Body body;

        if (inputs.isEmpty() && inPlace && formUrl != null && !formUrl.isBlank()) {
            // The connection carries the Meta app secret, so the app's webhook can be verified server-side: send
            // in-place reply buttons that resolve the approval without leaving WhatsApp (see
            // WhatsAppInteractivityController). The button ids carry the tokenized resume id (the form-URL tail —
            // WhatsApp allows 256 chars, so the signed token fits) prefixed with the decision, and must be unique
            // within the message.
            String resumeId = formUrl.substring(formUrl.lastIndexOf('/') + 1);

            body = Body.of(
                MESSAGING_PRODUCT, "whatsapp",
                RECIPIENT_TYPE, "individual",
                RECEIVE_USER, inputParameters.getRequiredString(RECEIVE_USER),
                TYPE, "interactive",
                "interactive", Map.of(
                    TYPE, "button",
                    BODY, Map.of(TEXT, buildSummaryText(inputParameters)),
                    "action", Map.of(
                        "buttons", List.of(
                            replyButton("a:" + resumeId, "Approve"),
                            replyButton("d:" + resumeId, "Discard")))));
        } else {
            body = Body.of(
                MESSAGING_PRODUCT, "whatsapp",
                RECIPIENT_TYPE, "individual",
                RECEIVE_USER, inputParameters.getRequiredString(RECEIVE_USER),
                TYPE, "text",
                TEXT, Map.of(BODY, buildMessageText(inputParameters, formUrl)));
        }

        return context
            .http(http -> http.post("/" + connectionParameters.getString(PHONE_NUMBER_ID) + "/messages"))
            .body(body)
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    private static Map<String, Object> replyButton(String id, String title) {
        return Map.of(TYPE, "reply", "reply", Map.of("id", id, "title", title));
    }

    private static String buildSummaryText(Parameters inputParameters) {
        StringBuilder builder = new StringBuilder();

        String formTitle = inputParameters.getString(FORM_TITLE);

        if (formTitle != null && !formTitle.isBlank()) {
            builder.append(formTitle.trim())
                .append("\n");
        }

        String formDescription = inputParameters.getString(FORM_DESCRIPTION);

        if (formDescription != null && !formDescription.isBlank()) {
            builder.append(formDescription.trim())
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
            builder.append("\nApprove: ")
                .append(formUrl)
                .append("?approved=true")
                .append("\nDiscard: ")
                .append(formUrl)
                .append("?approved=false");
        } else {
            builder.append("\nReview and respond: ")
                .append(formUrl);
        }

        return builder.toString();
    }
}
