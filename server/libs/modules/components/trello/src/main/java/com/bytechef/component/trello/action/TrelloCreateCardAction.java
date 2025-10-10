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
import static com.bytechef.component.trello.constant.TrelloConstants.DESC;
import static com.bytechef.component.trello.constant.TrelloConstants.ID_BOARD;
import static com.bytechef.component.trello.constant.TrelloConstants.ID_LIST;
import static com.bytechef.component.trello.constant.TrelloConstants.NAME;

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
public class TrelloCreateCardAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createCard")
        .title("Create Card")
        .description("Creates a new card.")
        .properties(
            string(ID_BOARD)
                .label("Board ID")
                .description("ID of the board.")
                .options((OptionsFunction<String>) TrelloUtils::getBoardOptions)
                .required(true),
            string(ID_LIST)
                .label("List ID")
                .description("ID of the list where the card should be created in.")
                .options((OptionsFunction<String>) TrelloUtils::getListOptions)
                .optionsLookupDependsOn(ID_BOARD)
                .required(true),
            string(NAME)
                .label("Name")
                .description("The name for the card.")
                .required(false),
            string(DESC)
                .label("Description")
                .description("The description for the card.")
                .required(false))
        .output(outputSchema(CARD_OUTPUT_PROPERTY))
        .perform(TrelloCreateCardAction::perform);

    private TrelloCreateCardAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext
            .http(http -> http.post("/cards"))
            .queryParameters(
                ID_LIST, inputParameters.getRequiredString(ID_LIST),
                NAME, inputParameters.getString(NAME),
                DESC, inputParameters.getString(DESC))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
