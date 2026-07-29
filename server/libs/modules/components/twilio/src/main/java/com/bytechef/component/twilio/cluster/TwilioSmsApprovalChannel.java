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

package com.bytechef.component.twilio.cluster;

import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType.FORM_URL_ENCODED;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.APPROVAL_CHANNELS;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.EXPIRES_AT;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.FORM_DESCRIPTION;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.FORM_TITLE;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.INPUTS;
import static com.bytechef.component.twilio.constant.TwilioConstants.BODY;
import static com.bytechef.component.twilio.constant.TwilioConstants.FROM;
import static com.bytechef.component.twilio.constant.TwilioConstants.TO;

import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.definition.approval.ApprovalChannelFunction;
import java.util.List;
import java.util.Map;

/**
 * Approval channel that delivers the request as a plain SMS via Twilio. Field-less approvals get one-click
 * Approve/Discard links (the hosted form pre-selects the decision via the {@code approved} query parameter and confirms
 * with a single click); approvals with form fields get the plain form link.
 *
 * @author Ivica Cardic
 */
public class TwilioSmsApprovalChannel {

    public static final ClusterElementDefinition<ApprovalChannelFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<ApprovalChannelFunction>clusterElement("sms")
            .title("SMS")
            .description("Sends an approval request SMS via Twilio.")
            .type(APPROVAL_CHANNELS)
            .properties(
                string(TO)
                    .label("To")
                    .description("The recipient phone number in international format, e.g. +15554449999.")
                    .controlType(ControlType.PHONE)
                    .exampleValue("+15554449999")
                    .required(true),
                string(FROM)
                    .label("From")
                    .description("The sender's Twilio phone number in international format, e.g. +15554449999.")
                    .controlType(ControlType.PHONE)
                    .exampleValue("+15554449999")
                    .required(true))
            .object(() -> TwilioSmsApprovalChannel::perform);

    private static Object perform(
        Parameters inputParameters, Parameters connectionParameters, String formUrl, ClusterElementContext context) {

        Map<String, String> bodyMap = Map.of(
            FROM, inputParameters.getRequiredString(FROM),
            TO, inputParameters.getRequiredString(TO),
            BODY, buildMessageText(inputParameters, formUrl));

        return context
            .http(http -> http.post("/Accounts/" + connectionParameters.getRequiredString(USERNAME) + "/Messages.json"))
            .body(Http.Body.of(bodyMap, FORM_URL_ENCODED))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    private static String buildMessageText(Parameters inputParameters, String formUrl) {
        List<Map<String, ?>> inputs = inputParameters.getList(INPUTS, new TypeReference<>() {}, List.of());

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
