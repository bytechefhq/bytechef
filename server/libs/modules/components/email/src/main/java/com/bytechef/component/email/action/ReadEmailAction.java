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
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
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
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Store;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Igor Beslic
 */
public class ReadEmailAction {

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

        folder.open(Folder.READ_ONLY);

        Message[] messages = folder.getMessages();
        Map<String, String>[] filtered = new Map[messages.length];

        for (int i = 0; i < messages.length; i++) {
            Message message = messages[i];

            filtered[i] = new HashMap<>();

            filtered[i].put(FROM, Arrays.toString(message.getFrom()));
            filtered[i].put(TO, Arrays.toString(message.getRecipients(RecipientType.TO)));
            filtered[i].put(CC, Arrays.toString(message.getRecipients(RecipientType.CC)));
            filtered[i].put(BCC, Arrays.toString(message.getRecipients(RecipientType.BCC)));
            filtered[i].put(SUBJECT, message.getSubject());
            filtered[i].put(CONTENT, message.getContent()
                .toString());
            filtered[i].put(CONTENT_TYPE, message.getContentType());
        }

        context.log(log -> log.debug(
            "Messages read: size:{}, from:{}, to:{}",
            filtered.length, inputParameters.get("from"), inputParameters.get("to")));

        return filtered;
    }
}
