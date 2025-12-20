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

package com.bytechef.component.trello.trigger;

import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.trello.constant.TrelloConstants.CARD_OUTPUT_PROPERTY;
import static com.bytechef.component.trello.constant.TrelloConstants.ID;
import static com.bytechef.component.trello.constant.TrelloConstants.ID_BOARD;
import static com.bytechef.component.trello.constant.TrelloConstants.ID_LIST;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.OptionsFunction;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.trello.util.TrelloUtils;
import java.util.Map;
import java.util.Objects;

/**
 * @author Monika Kušter
 */
public class TrelloNewCardTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newCard")
        .title("New Card")
        .description("Triggers when a new card is created on specified board or list.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties(
            string(ID_BOARD)
                .label("Board ID")
                .options((OptionsFunction<String>) TrelloUtils::getBoardOptions)
                .required(false),
            string(ID_LIST)
                .label("List ID")
                .options((OptionsFunction<String>) TrelloUtils::getListOptions)
                .optionsLookupDependsOn(ID_BOARD)
                .required(false))
        .output(outputSchema(CARD_OUTPUT_PROPERTY))
        .webhookEnable(TrelloNewCardTrigger::dynamicWebhookEnable)
        .webhookDisable(TrelloNewCardTrigger::dynamicWebhookDisable)
        .webhookRequest(TrelloNewCardTrigger::dynamicWebhookRequest);

    private TrelloNewCardTrigger() {
    }

    protected static TriggerDefinition.WebhookEnableOutput dynamicWebhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl, String workflowExecutionId,
        TriggerContext context) {

        String idList = inputParameters.getString(ID_LIST);

        Map<String, Object> body = context
            .http(http -> http.post("/webhooks"))
            .queryParameters(
                "callbackURL", webhookUrl,
                "idModel", idList == null ? inputParameters.getRequiredString(ID_BOARD) : idList)
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return new WebhookEnableOutput(Map.of(ID, (String) body.get(ID)), null);
    }

    protected static void dynamicWebhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        context
            .http(http -> http.delete("/webhooks/" + outputParameters.getString(ID)))
            .configuration(responseType(ResponseType.JSON))
            .execute();
    }

    protected static Object dynamicWebhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters output, TriggerContext context) {

        Map<String, Map<String, Object>> content = body.getContent(new TypeReference<>() {});

        Map<String, Object> action = content.get("action");

        if (Objects.equals(action.get("type"), "createCard") && action.get("data") instanceof Map<?, ?> map &&
            map.get("card") instanceof Map<?, ?> cardMap) {

            return getCard(context, (String) cardMap.get(ID));
        }

        return null;
    }

    private static Object getCard(TriggerContext context, String cardId) {
        return context
            .http(http -> http.get("/cards/" + cardId))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
