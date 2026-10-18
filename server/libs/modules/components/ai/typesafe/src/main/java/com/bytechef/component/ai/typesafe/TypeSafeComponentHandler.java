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

package com.bytechef.component.ai.typesafe;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.typesafe.action.TypeSafeChoiceAction;
import com.bytechef.component.ai.typesafe.action.TypeSafeNoulAction;
import com.bytechef.component.ai.typesafe.action.TypeSafeScoreAction;
import com.bytechef.component.ai.typesafe.connection.TypeSafeConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * TypeSafe AI System One: asks typed questions about a state and returns structured answers — no text generation, no
 * JSON to parse.
 *
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class TypeSafeComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("typesafe")
        .title("TypeSafe AI")
        .version(1)
        .description(
            "TypeSafe AI answers typed questions about a piece of content with structured values instead of " +
                "generated text: a truth value (Noul), a selected label (Choice) or a position on a rubric (Score).")
        .icon("path:assets/typesafe.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(TypeSafeConnection.CONNECTION_DEFINITION)
        .actions(
            TypeSafeNoulAction.ACTION_DEFINITION,
            TypeSafeChoiceAction.ACTION_DEFINITION,
            TypeSafeScoreAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
