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

package com.bytechef.component.googledrive.action;

import static com.bytechef.component.googledrive.constant.ExampleConstants.DUMMY;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.Parameters;

/**
 * @author Mario Cvjetojevic
 */
public final class ExampleDummyAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(DUMMY)
        .title("Title")
        .description("Description")
        .properties()
        .outputSchema(string())
        .perform(ExampleDummyAction::perform);

    private ExampleDummyAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return null;
    }
}
