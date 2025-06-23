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

package com.bytechef.component.email.action;

import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.email.constant.EmailConstants.HOST;
import static com.bytechef.component.email.constant.EmailConstants.PORT;
import static com.bytechef.component.email.constant.EmailConstants.PROTOCOL;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.email.EmailProtocol;
import com.bytechef.component.email.commons.EmailUtils;
import jakarta.mail.Authenticator;
import jakarta.mail.BodyPart;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.Part;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.FlagTerm;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Igor Beslic
 */
public class ReadEmailAction {

    private static final String HAS_ATTACHMENTS = "hasAttachments";
    private static final String FROM = "from";
    private static final String CC = "cc";
    private static final String BCC = "bcc";

    private static final String TO = "to";
    private static final String CONTENT = "content";
    private static final String CONTENT_TYPE = "contentType";
    private static final String SUBJECT = "subject";

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("get")
        .title("Get Mail")
        .description("Get emails from inbox.")
        .properties(
            integer(PORT)
                .label("Port")
                .description("Defines the port to connect to the email server.")
                .required(true)
                .defaultValue(25),
            string(PROTOCOL)
                .controlType(Property.ControlType.SELECT)
                .defaultValue(EmailProtocol.imap.name())
                .label("Protocol")
                .description(
                    "Protocol defines communication procedure. IMAP allows receiving emails. POP3 is older protocol for receiving emails.")
                .options(
                    option(EmailProtocol.imap.name(), EmailProtocol.imap.name(), "IMAP is used to receive email"),
                    option(EmailProtocol.pop3.name(), EmailProtocol.pop3.name(), "POP3 is used to receive email"))
                .required(true),
            string(FROM)
                .label("From Email")
                .description("From who the email was sent.")
                .required(true),
            string(SUBJECT)
                .label("Subject contains")
                .description(
                    "Filters email messages where subject contains this keyword. Character matching is case insensitive.")
                .required(true))
        .output(
            outputSchema(
                array()
                    .items(
                        object()
                            .properties(
                                string(CC), string(CONTENT), string(CONTENT_TYPE), string(FROM), bool(HAS_ATTACHMENTS),
                                string(SUBJECT)))
                    .description("The email message.")))
        .perform(ReadEmailAction::perform);

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context)
        throws MessagingException, IOException {

        Session session;
        EmailProtocol emailProtocol = EmailProtocol.valueOf(inputParameters.getRequiredString(PROTOCOL));
        int port = inputParameters.getRequiredInteger(PORT);

        if (connectionParameters.containsKey(USERNAME)) {
            session =
                Session.getInstance(EmailUtils.getMailSessionProperties(port, emailProtocol, connectionParameters),
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
                EmailUtils.getMailSessionProperties(port, emailProtocol, connectionParameters));
        }

        Store protocolStore = session.getStore(emailProtocol.name());
        protocolStore.connect(
            connectionParameters.getRequiredString(HOST), connectionParameters.getRequiredString(USERNAME),
            connectionParameters.getRequiredString(PASSWORD));

        Folder folder = protocolStore.getFolder("INBOX");

        folder.open(Folder.READ_WRITE);

        Message[] messages = folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

        List<Map<String, Object>> filtered = new ArrayList<>();

        String[] fromCriterias = asCriteriaArray(inputParameters.getString(FROM));
        String[] subjectCriterias = asCriteriaArray(inputParameters.getString(SUBJECT));

        for (Message message : messages) {
            if (mismatchesCriteria(message.getFrom()[0].toString(), fromCriterias)) {
                continue;
            }

            if (mismatchesCriteria(message.getSubject(), subjectCriterias)) {
                continue;
            }

            Map<String, Object> itemMap = new HashMap<>();

            itemMap.put(FROM, context.json(json -> json.write(message.getFrom())));
            itemMap.put(TO, context.json(json -> json.write(message.getRecipients(RecipientType.TO))));
            itemMap.put(CC, context.json(json -> json.write(message.getRecipients(RecipientType.CC))));
            itemMap.put(BCC, context.json(json -> json.write(message.getRecipients(RecipientType.BCC))));
            itemMap.put(SUBJECT, message.getSubject());
            itemMap.put(CONTENT, getContent(message));
            itemMap.put(CONTENT_TYPE, message.getContentType());
            itemMap.put("hasAttachments", Boolean.FALSE);

            filtered.add(itemMap);
        }

        context.log(log -> log.debug(
            "Messages read: size:{}, from:{}, to:{}",
            filtered.size(), inputParameters.get("from"), inputParameters.get("to")));

        return filtered.toArray(new Map[0]);
    }

    private static String[] asCriteriaArray(String value) {
        if (value == null || value.isEmpty()) {
            return new String[0];
        }

        String[] criteriaTerms = value.split(",");

        if (criteriaTerms.length == 0) {
            return new String[0];
        }

        String[] sanitizedCriteriaTerms = new String[criteriaTerms.length];

        for (int i = 0; i < criteriaTerms.length; i++) {
            sanitizedCriteriaTerms[i] = criteriaTerms[i].trim();
        }

        return sanitizedCriteriaTerms;
    }

    private static boolean mismatchesCriteria(String sample, String[] criterias) {
        if (criterias.length == 0) {
            return false;
        }

        String sanitizedSample = sample.toLowerCase();

        for (String criteria : criterias) {
            if (sanitizedSample.contains(criteria.toLowerCase())) {

                return false;
            }
        }

        return true;
    }

    private static String getContent(Message message) throws IOException, MessagingException {
        Object content = message.getContent();

        if (content instanceof String) {
            return (String) content;
        } else if (content instanceof MimeMultipart) {
            return getMultipartContent((MimeMultipart) content);
        }

        return "Unknown content type: " + content.getClass();

    }

    private static String getMultipartContent(MimeMultipart multipart) throws IOException, MessagingException {
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);

            if (bodyPart.isMimeType("text/plain")) {
                stringBuilder.append(bodyPart.getContent());
                stringBuilder.append("\n");
            } else if (bodyPart.isMimeType("text/html")) {
                stringBuilder.append(bodyPart.getContent());
                stringBuilder.append("\n");
            } else if (bodyPart.getDisposition() != null && bodyPart.getDisposition()
                .equalsIgnoreCase(Part.ATTACHMENT)) {
                System.out.println("Attachment: " + bodyPart.getFileName());
                try (InputStream is = bodyPart.getInputStream()) {
                    System.out.println(
                        "Attachment content: " + new String(is.readAllBytes(), Charset.forName("UTF-8")));
                }
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                stringBuilder.append(getMultipartContent((MimeMultipart) bodyPart.getContent()));
            }
        }

        return stringBuilder.toString();
    }

}
