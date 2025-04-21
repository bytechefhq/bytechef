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

package com.bytechef.component.bolna;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.bolna.action.BolnaMakePhoneCallAction;
import com.bytechef.component.bolna.connection.BolnaConnection;
import com.bytechef.component.bolna.trigger.BolnaCallCompletionReportTrigger;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Marija Horvat
 */
@AutoService(ComponentHandler.class)
public class BolnaComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("bolna")
        .title("Bolna")
        .description(
            "Bolna AI is an open-source platform that enables businesses to create and deploy voice-driven " +
                "conversational agents")
        .icon("path:assets/bolna.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(BolnaConnection.CONNECTION_DEFINITION)
        .actions(BolnaMakePhoneCallAction.ACTION_DEFINITION)
        .triggers(BolnaCallCompletionReportTrigger.TRIGGER_DEFINITION)
        .clusterElements(tool(BolnaMakePhoneCallAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
