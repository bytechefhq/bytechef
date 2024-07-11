/*
 * Copyright 2023-present ByteChef Inc.
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

import static com.bytechef.component.definition.ComponentDSL.ModifiableTriggerDefinition;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.ComponentDSL.trigger;
import static com.bytechef.component.definition.TriggerContext.Data.Scope.WORKFLOW;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.HISTORY_ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ME;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.MESSAGE_PROPERTY;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.NEW_EMAIL;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.TOPIC_NAME;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.exception.ProviderException;
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
 * @author Monika Domiter
 */
public class GoogleMailNewEmailTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger(NEW_EMAIL)
        .title("New Email")
        .description("Triggers when new mail is found in your Gmail inbox.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties(
            string(TOPIC_NAME)
                .label("Topic name")
                .required(true))
        .outputSchema(
            array()
                .items(MESSAGE_PROPERTY))
        .dynamicWebhookEnable(GoogleMailNewEmailTrigger::dynamicWebhookEnable)
        .dynamicWebhookDisable(GoogleMailNewEmailTrigger::dynamicWebhookDisable)
        .dynamicWebhookRequest(GoogleMailNewEmailTrigger::dynamicWebhookRequest);

    private GoogleMailNewEmailTrigger() {
    }

    protected static DynamicWebhookEnableOutput dynamicWebhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, Context context) {

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
            throw new ProviderException("Failed to start Gmail webhook", e);
        }

        return new DynamicWebhookEnableOutput(Map.of(HISTORY_ID, watchResponse.getHistoryId()), null);
    }

    protected static void dynamicWebhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, Context context) {

        Gmail gmail = GoogleServices.getMail(connectionParameters);

        try {
            gmail.users()
                .stop(ME)
                .execute();
        } catch (IOException e) {
            throw new ProviderException("Failed to stop Gmail webhook", e);
        }
    }

    protected static List<Message> dynamicWebhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers,
        HttpParameters parameters, WebhookBody body, WebhookMethod method, DynamicWebhookEnableOutput output,
        TriggerContext context) throws IOException {

        Gmail gmail = GoogleServices.getMail(connectionParameters);

        Optional<Object> historyIdOptional = context.data(data -> data.fetchValue(WORKFLOW, HISTORY_ID));

        Map<String, ?> outputParameters = output.parameters();

        Integer triggerHistoryId = (Integer) outputParameters.get(HISTORY_ID);

        BigInteger historyId = historyIdOptional.map(o -> new BigInteger(o.toString()))
            .orElse(new BigInteger(triggerHistoryId.toString()));

        ListHistoryResponse listHistoryResponse = gmail.users()
            .history()
            .list(ME)
            .setStartHistoryId(historyId)
            .execute();

        List<Message> newEmails = new ArrayList<>();

        List<History> historyList = listHistoryResponse.getHistory();

        if (historyList != null && !historyList.isEmpty()) {
            History lastHistory = historyList.getLast();

            List<HistoryMessageAdded> messagesAdded = lastHistory.getMessagesAdded();

            if (messagesAdded != null && !messagesAdded.isEmpty()) {
                for (HistoryMessageAdded historyMessageAdded : messagesAdded) {
                    Message historyMessage = historyMessageAdded.getMessage();

                    Message message = gmail.users()
                        .messages()
                        .get(ME, historyMessage.getId())
                        .execute();

                    newEmails.add(message);
                }
            }
        }

        context.data(data -> data.setValue(WORKFLOW, HISTORY_ID, listHistoryResponse.getHistoryId()));

        return newEmails;
    }
}
