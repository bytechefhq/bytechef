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

package com.bytechef.component.google.mail.cluster;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.APPROVAL_CHANNELS;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.EXPIRES_AT;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.FORM_DESCRIPTION;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.FORM_TITLE;
import static com.bytechef.component.definition.approval.ApprovalChannelFunction.INPUTS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.EMAIL_PROPERTY;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ME;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.SUBJECT;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.TO;

import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.definition.approval.ApprovalChannelFunction;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.codec.binary.Base64;

/**
 * @author Ivica Cardic
 */
public class GoogleMailApprovalChannel {

    public static final ClusterElementDefinition<ApprovalChannelFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<ApprovalChannelFunction>clusterElement("googleMail")
            .title("Gmail")
            .description("Sends an approval request email via Gmail.")
            .type(APPROVAL_CHANNELS)
            .properties(
                array(TO)
                    .label("To")
                    .description("Recipients email addresses.")
                    .items(EMAIL_PROPERTY)
                    .required(true),
                string(SUBJECT)
                    .label("Subject")
                    .description("Subject of the approval email.")
                    .required(true))
            .object(() -> GoogleMailApprovalChannel::perform);

    @SuppressFBWarnings("POTENTIAL_XML_INJECTION")
    private static Object perform(
        Parameters inputParameters, Parameters connectionParameters, String formUrl, ClusterElementContext context)
        throws IOException, MessagingException {

        Gmail gmail = GoogleServices.getMail(connectionParameters);

        List<String> toAddresses = inputParameters.getRequiredList(TO, String.class);
        // A MIME Subject header is plain text (jakarta.mail applies its own RFC 2047 encoding), so it must NOT be
        // HTML-escaped — escaping corrupts subjects containing &, <, or >.
        String subject = inputParameters.getRequiredString(SUBJECT);

        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties(), null));

        mimeMessage.setRecipients(
            jakarta.mail.Message.RecipientType.TO, InternetAddress.parse(String.join(",", toAddresses)));
        mimeMessage.setSubject(subject, StandardCharsets.UTF_8.name());

        List<Map<String, ?>> inputs = inputParameters.getList(INPUTS, new TypeReference<>() {}, List.of());

        StringBuilder builder = new StringBuilder();

        String formTitle = inputParameters.getString(FORM_TITLE);
        String titleTrim = formTitle == null ? null : formTitle.trim();

        if (titleTrim != null && !titleTrim.isBlank()) {
            builder.append("<h2>")
                .append((String) context.escaper(escaper -> escaper.escapeHtml(titleTrim)))
                .append("</h2>");
        }

        String formDescription = inputParameters.getString(FORM_DESCRIPTION);
        String descTrim = formDescription == null ? null : formDescription.trim();

        if (descTrim != null && !descTrim.isBlank()) {
            builder.append("<p>")
                .append((String) context.escaper(escaper -> escaper.escapeHtml(descTrim)))
                .append("</p>");
        }

        String expiresAt = inputParameters.getString(EXPIRES_AT);

        if (expiresAt != null && !expiresAt.isBlank()) {
            builder.append("<p>Expires: ")
                .append((String) context.escaper(escaper -> escaper.escapeHtml(expiresAt)))
                .append("</p>");
        }

        if (inputs.isEmpty()) {
            builder.append("<p><a href=\"")
                .append((String) context.escaper(escaper -> escaper.escapeHtml(formUrl + "?approved=true")))
                .append("\">Approve</a> | ")
                .append("<a href=\"")
                .append((String) context.escaper(escaper -> escaper.escapeHtml(formUrl + "?approved=false")))
                .append("\">Discard</a></p>");
        } else {
            String safeUrl = context.escaper(escaper -> escaper.escapeHtml(formUrl == null ? "#" : formUrl));

            builder.append("<p>Please review and respond using the link below:</p>")
                .append("<p><a href=\"")
                .append(safeUrl)
                .append("\">Open Approval Form</a></p>");
        }

        String body = builder.toString();

        MimeBodyPart mimeBodyPart = new MimeBodyPart();

        mimeBodyPart.setText(body, StandardCharsets.UTF_8.name(), "html");

        MimeMultipart multipart = new MimeMultipart();

        multipart.addBodyPart(mimeBodyPart);

        mimeMessage.setContent(multipart);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        mimeMessage.writeTo(buffer);

        Message message = new Message();

        message.setRaw(Base64.encodeBase64URLSafeString(buffer.toByteArray()));

        return gmail.users()
            .messages()
            .send(ME, message)
            .execute();
    }
}
