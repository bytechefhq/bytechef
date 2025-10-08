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

package com.bytechef.component.trello.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.trello.constant.TrelloConstants.CARD_OUTPUT_PROPERTY;
import static com.bytechef.component.trello.constant.TrelloConstants.ID;
import static com.bytechef.component.trello.constant.TrelloConstants.ID_BOARD;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.trello.util.TrelloUtils;

/**
 * @author Monika Kušter
 */
public class TrelloGetCardAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getCard")
        .title("Get Card")
        .description("Gets a card details.")
        .properties(
            string(ID_BOARD)
                .label("Board ID")
                .description("ID of the board where card is located.")
                .options((OptionsFunction<String>) TrelloUtils::getBoardOptions)
                .required(true),
            string(ID)
                .label("Card ID")
                .options((OptionsFunction<String>) TrelloUtils::getCardOptions)
                .optionsLookupDependsOn(ID_BOARD)
                .required(true))
        .output(outputSchema(CARD_OUTPUT_PROPERTY))
        .perform(TrelloGetCardAction::perform);

    private TrelloGetCardAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext
            .http(http -> http.get("/cards/" + inputParameters.getRequiredString(ID)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
