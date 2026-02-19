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

package com.bytechef.component.google.mail.trigger;

import static com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.definition.TriggerContext.Data.Scope.WORKFLOW;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FORMAT;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FORMAT_PROPERTY;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.HISTORY_ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ME;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.TOPIC_NAME;
import static com.bytechef.component.google.mail.definition.Format.FULL;
import static com.bytechef.component.google.mail.definition.Format.SIMPLE;
import static com.bytechef.component.google.mail.util.GoogleMailUtils.getSimpleMessage;
import static com.bytechef.google.commons.GoogleUtils.translateGoogleIOException;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.google.mail.definition.Format;
import com.bytechef.component.google.mail.util.GoogleMailUtils;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.History;
import com.google.api.services.gmail.model.HistoryMessageAdded;
import com.google.api.services.gmail.model.ListHistoryResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.WatchRequest;
import com.google.api.services.gmail.model.WatchResponse;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Monika Kušter
 */
public class GoogleMailNewEmailTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newEmail")
        .title("New Email")
        .description("Triggers when new mail is found in your Gmail inbox.")
        .type(TriggerType.STATIC_WEBHOOK)
        .help("", "https://docs.bytechef.io/reference/components/google-mail_v1#new-email")
        .properties(
            string(TOPIC_NAME)
                .label("Topic Name")
                .description(
                    "Must be 3-255 characters, start with a letter, and contain only the following characters: " +
                        "letters, numbers, dashes (-), periods (.), underscores (_), tildes (~), percents (%) or " +
                        "plus signs (+). Cannot start with goog.")
                .maxLength(255)
                .minLength(3)
                .required(true),
            FORMAT_PROPERTY)
        .output(GoogleMailUtils::getMessageOutput)
        .webhookEnable(GoogleMailNewEmailTrigger::webhookEnable)
        .webhookDisable(GoogleMailNewEmailTrigger::webhookDisable)
        .webhookRequest(GoogleMailNewEmailTrigger::webhookRequest);

    private GoogleMailNewEmailTrigger() {
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext context) {

        Gmail gmail = GoogleServices.getMail(connectionParameters);

        WatchRequest watchRequest = new WatchRequest()
            .setTopicName(inputParameters.getRequiredString(TOPIC_NAME))
            .setLabelIds(List.of("INBOX"));

        WatchResponse watchResponse;

        try {
            watchResponse = gmail.users()
                .watch(ME, watchRequest)
                .execute();
        } catch (IOException e) {
            throw translateGoogleIOException(e);
        }

        return new WebhookEnableOutput(Map.of(HISTORY_ID, watchResponse.getHistoryId()), null);
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        Gmail gmail = GoogleServices.getMail(connectionParameters);

        try {
            gmail.users()
                .stop(ME)
                .execute();
        } catch (IOException e) {
            throw translateGoogleIOException(e);
        }
    }

    protected static List<Object> webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers,
        HttpParameters parameters, WebhookBody body, WebhookMethod method, Parameters outputParameters,
        TriggerContext context) {

        Gmail gmail = GoogleServices.getMail(connectionParameters);

        Optional<Object> historyIdOptional = context.data(data -> data.fetch(WORKFLOW, HISTORY_ID));

        Integer triggerHistoryId = (Integer) outputParameters.get(HISTORY_ID);

        BigInteger historyId = historyIdOptional.map(o -> new BigInteger(o.toString()))
            .orElse(new BigInteger(triggerHistoryId.toString()));

        ListHistoryResponse listHistoryResponse;
        try {
            listHistoryResponse = gmail.users()
                .history()
                .list(ME)
                .setStartHistoryId(historyId)
                .execute();
        } catch (IOException e) {
            throw translateGoogleIOException(e);
        }

        List<Object> newEmails = new ArrayList<>();

        List<History> historyList = listHistoryResponse.getHistory();

        if (historyList != null && !historyList.isEmpty()) {
            History lastHistory = historyList.getLast();

            List<HistoryMessageAdded> messagesAdded = lastHistory.getMessagesAdded();

            if (messagesAdded != null && !messagesAdded.isEmpty()) {
                Format format = inputParameters.get(FORMAT, Format.class, SIMPLE);

                for (HistoryMessageAdded historyMessageAdded : messagesAdded) {
                    Message historyMessage = historyMessageAdded.getMessage();

                    Message message;
                    try {
                        message = gmail.users()
                            .messages()
                            .get(ME, historyMessage.getId())
                            .setFormat(format == SIMPLE ? FULL.getMapping() : format.getMapping())
                            .execute();
                    } catch (IOException e) {
                        throw translateGoogleIOException(e);
                    }

                    if (format.equals(SIMPLE)) {
                        newEmails.add(getSimpleMessage(message, context, gmail));
                    } else {
                        newEmails.add(message);
                    }
                }
            }
        }

        context.data(data -> data.put(WORKFLOW, HISTORY_ID, listHistoryResponse.getHistoryId()));

        return newEmails;
    }
}
