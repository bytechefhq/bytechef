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

package com.bytechef.component.email.cluster;

import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.APPROVAL_CHANNELS;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.EXPIRES_AT;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.FORM_DESCRIPTION;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.FORM_TITLE;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.INPUTS;
import static com.bytechef.component.email.constant.EmailConstants.PORT;

import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableClusterElementDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.definition.approval.ApprovalChannelFunction;
import com.bytechef.component.email.EmailProtocol;
import com.bytechef.component.email.commons.EmailUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.util.List;
import java.util.Map;

/**
 * Approval channel that delivers the request as an HTML email over the connected SMTP server. Field-less approvals get
 * one-click Approve/Discard links (the hosted form pre-selects the decision via the {@code approved} query parameter
 * and confirms with a single click); approvals with form fields get an "Open Approval Form" link.
 *
 * @author Ivica Cardic
 */
public class EmailApprovalChannel {

    private static final String FROM = "from";
    private static final String TO = "to";
    private static final String SUBJECT = "subject";

    public static final ModifiableClusterElementDefinition<ApprovalChannelFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<ApprovalChannelFunction>clusterElement("email")
            .title("Email")
            .description("Sends an approval request email via the connected SMTP server.")
            .type(APPROVAL_CHANNELS)
            .properties(
                integer(PORT)
                    .label("Port")
                    .description("Defines the port to connect to the email server.")
                    .defaultValue(25)
                    .required(true),
                string(FROM)
                    .label("From Email")
                    .description("From who to send the approval email.")
                    .required(true),
                array(TO)
                    .label("To Email")
                    .description("Who to send the approval email to.")
                    .items(string())
                    .required(true),
                string(SUBJECT)
                    .label("Subject")
                    .description("Subject of the approval email.")
                    .required(true))
            .object(() -> EmailApprovalChannel::perform);

    private static Object perform(
        Parameters inputParameters, Parameters connectionParameters, String formUrl, ClusterElementContext context)
        throws MessagingException {

        int port = inputParameters.getRequiredInteger(PORT);

        Session session;

        if (connectionParameters.containsKey(USERNAME)) {
            session = Session.getInstance(
                EmailUtils.getMailSessionProperties(port, EmailProtocol.smtp, connectionParameters),
                new Authenticator() {

                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                            connectionParameters.getRequiredString(USERNAME),
                            connectionParameters.getRequiredString(PASSWORD));
                    }
                });
        } else {
            session = Session.getInstance(
                EmailUtils.getMailSessionProperties(port, EmailProtocol.smtp, connectionParameters));
        }

        Message message = new MimeMessage(session);

        message.setFrom(new InternetAddress(inputParameters.getRequiredString(FROM)));
        message.setRecipients(
            RecipientType.TO,
            InternetAddress.parse(String.join(",", inputParameters.getRequiredList(TO, String.class))));
        message.setSubject(inputParameters.getRequiredString(SUBJECT));

        MimeBodyPart mimeBodyPart = new MimeBodyPart();

        mimeBodyPart.setContent(buildHtmlBody(inputParameters, formUrl, context), "text/html; charset=utf-8");

        Multipart multipart = new MimeMultipart();

        multipart.addBodyPart(mimeBodyPart);

        message.setContent(multipart);

        Transport.send(message);

        return null;
    }

    // Every interpolated value below is routed through context.escaper(...).escapeHtml(...) (backed by Apache
    // Commons StringEscapeUtils.escapeHtml4), which is real HTML4 escaping. SpotBugs' taint analysis cannot trace
    // sanitization through the generic ContextFunction/lambda indirection used to reach the Escaper, so it still
    // flags these appends as POTENTIAL_XML_INJECTION even though the content is safe. Same false positive, same
    // fix, as GoogleMailApprovalChannel#perform and MicrosoftOutlook365ApprovalChannel#perform.
    @SuppressFBWarnings("POTENTIAL_XML_INJECTION")
    private static String buildHtmlBody(Parameters inputParameters, String formUrl, ClusterElementContext context) {
        List<Map<String, ?>> inputs = inputParameters.getList(INPUTS, new TypeReference<>() {}, List.of());

        StringBuilder builder = new StringBuilder();

        String formTitle = inputParameters.getString(FORM_TITLE);

        if (formTitle != null && !formTitle.isBlank()) {
            builder.append("<h2>")
                .append((String) context.escaper(escaper -> escaper.escapeHtml(formTitle.trim())))
                .append("</h2>");
        }

        String formDescription = inputParameters.getString(FORM_DESCRIPTION);

        if (formDescription != null && !formDescription.isBlank()) {
            builder.append("<p>")
                .append((String) context.escaper(escaper -> escaper.escapeHtml(formDescription.trim())))
                .append("</p>");
        }

        if (builder.isEmpty()) {
            builder.append("<p>You have a new approval request.</p>");
        }

        String expiresAt = inputParameters.getString(EXPIRES_AT);

        if (expiresAt != null && !expiresAt.isBlank()) {
            builder.append("<p>Expires: ")
                .append((String) context.escaper(escaper -> escaper.escapeHtml(expiresAt)))
                .append("</p>");
        }

        if (formUrl == null || formUrl.isBlank()) {
            builder.append("<p>The approval form link is unavailable because no public URL is configured.</p>");
        } else if (inputs.isEmpty()) {
            builder.append("<p><a href=\"")
                .append((String) context.escaper(escaper -> escaper.escapeHtml(formUrl + "?approved=true")))
                .append("\">Approve</a> | <a href=\"")
                .append((String) context.escaper(escaper -> escaper.escapeHtml(formUrl + "?approved=false")))
                .append("\">Discard</a></p>");
        } else {
            builder.append("<p><a href=\"")
                .append((String) context.escaper(escaper -> escaper.escapeHtml(formUrl)))
                .append("\">Open Approval Form</a></p>");
        }

        return builder.toString();
    }
}
