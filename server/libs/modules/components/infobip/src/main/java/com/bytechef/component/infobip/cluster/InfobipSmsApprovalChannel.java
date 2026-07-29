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

package com.bytechef.component.infobip.cluster;

import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.APPROVAL_CHANNELS;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.EXPIRES_AT;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.FORM_DESCRIPTION;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.FORM_TITLE;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.INPUTS;
import static com.bytechef.component.infobip.constant.InfobipConstants.CONTENT;
import static com.bytechef.component.infobip.constant.InfobipConstants.DESTINATIONS;
import static com.bytechef.component.infobip.constant.InfobipConstants.MESSAGES;
import static com.bytechef.component.infobip.constant.InfobipConstants.SENDER;
import static com.bytechef.component.infobip.constant.InfobipConstants.TEXT;
import static com.bytechef.component.infobip.constant.InfobipConstants.TO;

import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.definition.approval.ApprovalChannelFunction;
import java.util.List;
import java.util.Map;

/**
 * Approval channel that delivers the request as a plain SMS via Infobip. Field-less approvals get one-click
 * Approve/Discard links (the hosted form pre-selects the decision via the {@code approved} query parameter and confirms
 * with a single click); approvals with form fields get the plain form link.
 *
 * @author Ivica Cardic
 */
public class InfobipSmsApprovalChannel {

    public static final ClusterElementDefinition<ApprovalChannelFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<ApprovalChannelFunction>clusterElement("sms")
            .title("SMS")
            .description("Sends an approval request SMS via Infobip.")
            .type(APPROVAL_CHANNELS)
            .properties(
                string(SENDER)
                    .label("From")
                    .description("The sender ID which can be alphanumeric or numeric (e.g., CompanyName).")
                    .required(true),
                string(TO)
                    .label("To")
                    .description("Message recipient number. Must be in international format.")
                    .maxLength(24)
                    .required(true))
            .object(() -> InfobipSmsApprovalChannel::perform);

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static Object perform(
        Parameters inputParameters, Parameters connectionParameters, String formUrl, ClusterElementContext context) {

        return context
            .http(http -> http.post("/sms/3/messages"))
            .body(
                Http.Body.of(
                    MESSAGES, List.of(
                        Map.of(
                            SENDER, inputParameters.getRequiredString(SENDER),
                            DESTINATIONS, List.of(Map.of(TO, inputParameters.getRequiredString(TO))),
                            CONTENT, Map.of(TEXT, buildMessageText(inputParameters, formUrl))))))
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
