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

package com.bytechef.component.greenhouse;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.greenhouse.action.GreenhouseGetApplicationAction;
import com.bytechef.component.greenhouse.action.GreenhouseGetCandidateAction;
import com.bytechef.component.greenhouse.action.GreenhouseGetJobAction;
import com.bytechef.component.greenhouse.action.GreenhouseListApplicationScorecardsAction;
import com.bytechef.component.greenhouse.connection.GreenhouseConnection;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class GreenhouseComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("greenhouse")
        .title("Greenhouse")
        .description(
            "Greenhouse is an applicant tracking system and hiring platform that helps companies manage job " +
                "openings, candidates, applications and structured interview feedback.")
        .icon("path:assets/greenhouse.svg")
        .categories(ComponentCategory.HRIS)
        .connection(GreenhouseConnection.CONNECTION_DEFINITION)
        .actions(
            GreenhouseGetApplicationAction.ACTION_DEFINITION,
            GreenhouseGetCandidateAction.ACTION_DEFINITION,
            GreenhouseGetJobAction.ACTION_DEFINITION,
            GreenhouseListApplicationScorecardsAction.ACTION_DEFINITION)
        .version(1)
        .customAction(true)
        .customActionHelp("", "https://harvestdocs.greenhouse.io/");

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
