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

package com.bytechef.component.trello;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.trello.action.TrelloCreateBoardAction;
import com.bytechef.component.trello.action.TrelloCreateCardAction;
import com.bytechef.component.trello.action.TrelloGetCardAction;
import com.bytechef.component.trello.connection.TrelloConnection;
import com.bytechef.component.trello.trigger.TrelloNewCardTrigger;
import com.google.auto.service.AutoService;

/**
 * @author Monika Kušter
 */
@AutoService(ComponentHandler.class)
public class TrelloComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("trello")
        .title("Trello")
        .description(
            "Trello is a project management tool that uses boards, lists, and cards to help users organize tasks " +
                "and collaborate with teams.")
        .customAction(true)
        .icon("path:assets/trello.svg")
        .categories(ComponentCategory.PRODUCTIVITY_AND_COLLABORATION)
        .connection(TrelloConnection.CONNECTION_DEFINITION)
        .actions(
            TrelloCreateBoardAction.ACTION_DEFINITION,
            TrelloCreateCardAction.ACTION_DEFINITION,
            TrelloGetCardAction.ACTION_DEFINITION)
        .triggers(TrelloNewCardTrigger.TRIGGER_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
