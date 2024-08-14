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

package com.bytechef.component.trello.constant;

import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ComponentDSL.ModifiableObjectProperty;

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
            string(ID),
            string(DESC),
            string(ID_BOARD),
            string(ID_LIST),
            string(NAME));

    private TrelloConstants() {
    }
}
