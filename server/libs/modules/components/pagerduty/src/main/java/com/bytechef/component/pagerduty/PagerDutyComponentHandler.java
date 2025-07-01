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

package com.bytechef.component.pagerduty;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.pagerduty.action.PagerDutyCreateIncidentAction;
import com.bytechef.component.pagerduty.action.PagerDutyCreateIncidentNoteAction;
import com.bytechef.component.pagerduty.action.PagerDutyUpdateIncidentAction;
import com.bytechef.component.pagerduty.connection.PagerDutyConnection;
import com.bytechef.component.pagerduty.trigger.PagerDutyNewOrUpdatedIncidentTrigger;
import com.google.auto.service.AutoService;

/**
 * @author Nikolina Spehar
 */
@AutoService(ComponentHandler.class)
public class PagerDutyComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("pagerDuty")
        .title("PagerDuty")
        .description(
            "With PagerDuty, you can get real-time alerts, manage on-call schedules, and automate parts of your " +
                "incident response process.")
        .icon("path:assets/pagerduty.svg")
        .categories(ComponentCategory.PROJECT_MANAGEMENT)
        .customAction(true)
        .connection(PagerDutyConnection.CONNECTION_DEFINITION)
        .actions(
            PagerDutyCreateIncidentAction.ACTION_DEFINITION,
            PagerDutyCreateIncidentNoteAction.ACTION_DEFINITION,
            PagerDutyUpdateIncidentAction.ACTION_DEFINITION)
        .clusterElements(
            tool(PagerDutyCreateIncidentAction.ACTION_DEFINITION),
            tool(PagerDutyCreateIncidentNoteAction.ACTION_DEFINITION),
            tool(PagerDutyUpdateIncidentAction.ACTION_DEFINITION))
        .triggers(PagerDutyNewOrUpdatedIncidentTrigger.TRIGGER_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
