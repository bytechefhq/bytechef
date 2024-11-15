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

package com.bytechef.component.example.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;

public class ExampleDummyAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("dummyAction")
        .title("Title")
        .description("Description")
        .properties()
        .output(outputSchema(string()))
        .perform(ExampleDummyAction::perform);

    private ExampleDummyAction() {
    }

    protected static String perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {
        // TODO

        return null;
    }
}
