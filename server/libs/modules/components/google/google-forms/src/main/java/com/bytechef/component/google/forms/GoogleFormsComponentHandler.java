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

package com.bytechef.component.google.forms;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.google.forms.connection.GoogleFormsConnection.CONNECTION_DEFINITION;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.google.forms.action.GoogleFormsGetFormAction;
import com.google.auto.service.AutoService;

/**
 * @author Monika Kušter
 */
@AutoService(ComponentHandler.class)
public class GoogleFormsComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("googleForms")
        .title("Google Forms")
        .description(
            "Google Forms is a web-based application that allows users to create surveys, quizzes, and forms for " +
                "data collection and analysis, with real-time collaboration and response tracking.")
        .icon("path:assets/google-forms.svg")
        .categories(ComponentCategory.SURVEYS_AND_FEEDBACK)
        .connection(CONNECTION_DEFINITION)
        .actions(GoogleFormsGetFormAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
