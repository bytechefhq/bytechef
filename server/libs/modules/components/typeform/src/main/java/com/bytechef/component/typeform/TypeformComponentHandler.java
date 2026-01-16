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

package com.bytechef.component.typeform;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.typeform.action.TypeformCreateEmptyFormAction;
import com.bytechef.component.typeform.connection.TypeformConnection;
import com.bytechef.component.typeform.trigger.TypeformNewSubmissionTrigger;
import com.google.auto.service.AutoService;

/**
 * @author Monika Kušter
 */
@AutoService(ComponentHandler.class)
public class TypeformComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("typeform")
        .title("Typeform")
        .description(
            "Typeform is an online survey and form-building tool that enables users to create interactive and " +
                "engaging forms for collecting data and feedback.")
        .icon("path:assets/typeform.svg")
        .categories(ComponentCategory.SURVEYS_AND_FEEDBACK)
        .connection(TypeformConnection.CONNECTION_DEFINITION)
        .actions(TypeformCreateEmptyFormAction.ACTION_DEFINITION)
        .triggers(TypeformNewSubmissionTrigger.TRIGGER_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
