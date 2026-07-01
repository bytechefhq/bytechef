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

package com.bytechef.component.microsoft.outlook.cluster;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.APPROVAL_CHANNELS;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.FORM_DESCRIPTION;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.FORM_TITLE;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.INPUTS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ADDRESS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.BODY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CONTENT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CONTENT_TYPE;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.EMAIL_ADDRESS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.FROM;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.SUBJECT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.TO_RECIPIENTS;
import static com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365Utils.createRecipientList;

import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.definition.approval.ApprovalChannelFunction;
import com.bytechef.component.microsoft.outlook.constant.ContentType;
import com.bytechef.microsoft.commons.MicrosoftUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MicrosoftOutlook365ApprovalChannel {

    public static final ClusterElementDefinition<ApprovalChannelFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<ApprovalChannelFunction>clusterElement("microsoftOutlook365")
            .title("Microsoft Outlook 365")
            .description("Sends an approval request email via Microsoft Outlook 365.")
            .type(APPROVAL_CHANNELS)
            .properties(
                string(FROM)
                    .label("From")
                    .description("The email address sending the approval email.")
                    .controlType(ControlType.EMAIL)
                    .required(true),
                array(TO_RECIPIENTS)
                    .label("To Recipients")
                    .description("The To: recipients for the approval email.")
                    .items(string().controlType(ControlType.EMAIL))
                    .required(true),
                string(SUBJECT)
                    .label("Subject")
                    .description("The subject of the approval email.")
                    .required(true))
            .processErrorResponse(MicrosoftUtils::processErrorResponse)
            .object(() -> MicrosoftOutlook365ApprovalChannel::perform);

    @SuppressFBWarnings("POTENTIAL_XML_INJECTION")
    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, String formUrl, ClusterElementContext context) {

        List<Map<String, ?>> inputs = inputParameters.getList(INPUTS, new TypeReference<>() {}, List.of());

        String body;

        if (inputs.isEmpty()) {
            String formTitle = inputParameters.getString(FORM_TITLE);
            String formDescription = inputParameters.getString(FORM_DESCRIPTION);

            StringBuilder builder = new StringBuilder();

            String titleTrim = formTitle == null ? null : formTitle.trim();
            if (titleTrim != null && !titleTrim.isBlank()) {
                builder.append("<h2>")
                    .append((String) context.escaper(escaper -> escaper.escapeHtml(titleTrim)))
                    .append("</h2>");
            }

            String descTrim = formDescription == null ? null : formDescription.trim();
            if (descTrim != null && !descTrim.isBlank()) {
                builder.append("<p>")
                    .append((String) context.escaper(escaper -> escaper.escapeHtml(descTrim)))
                    .append("</p>");
            }

            builder.append("<p><a href=\"")
                .append((String) context.escaper(escaper -> escaper.escapeHtml(formUrl + "?approved=true")))
                .append("\">Approve</a> | ")
                .append("<a href=\"")
                .append((String) context.escaper(escaper -> escaper.escapeHtml(formUrl + "?approved=false")))
                .append("\">Discard</a></p>");

            body = builder.toString();
        } else {
            String safeUrl = context.escaper(escaper -> escaper.escapeHtml(formUrl == null ? "#" : formUrl));

            body = "<p>You have a new approval request. Please review and respond using the link below:</p>" +
                "<p><a href=\"" + safeUrl + "\">Open Approval Form</a></p>";
        }

        context.http(http -> http.post("/me/sendMail"))
            .body(
                Http.Body.of(
                    "message",
                    new Object[] {
                        FROM, Map.of(EMAIL_ADDRESS, Map.of(ADDRESS, inputParameters.getRequiredString(FROM))),
                        SUBJECT, inputParameters.getRequiredString(SUBJECT),
                        BODY, Map.of(CONTENT_TYPE, ContentType.HTML.name(), CONTENT, body),
                        TO_RECIPIENTS, createRecipientList(inputParameters.getRequiredList(TO_RECIPIENTS, String.class))
                    }))
            .execute();

        return null;
    }
}
