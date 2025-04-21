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

package com.bytechef.component.trello.constant;

import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Monika Kušter
 */
public class TrelloConstants {

    public static final String DESC = "desc";
    public static final String ID = "id";
    public static final String ID_BOARD = "idBoard";
    public static final String ID_LIST = "idList";
    public static final String NAME = "name";

    public static final ModifiableObjectProperty CARD_OUTPUT_PROPERTY = object()
        .properties(
            string(ID)
                .description("ID of the card."),
            string(DESC)
                .description("Description of the card."),
            string(ID_BOARD)
                .description("ID of the board the card belongs to."),
            string(ID_LIST)
                .description("ID of the list the card belongs to."),
            string(NAME)
                .description("Name of the card."));

    private TrelloConstants() {
    }
}
