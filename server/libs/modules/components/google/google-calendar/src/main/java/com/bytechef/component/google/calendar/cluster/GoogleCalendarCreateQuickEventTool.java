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

package com.bytechef.component.google.calendar.cluster;

import static com.bytechef.component.definition.ai.agent.ToolFunction.TOOLS;
import static com.bytechef.component.google.calendar.action.GoogleCalendarCreateQuickEventAction.OUTPUT_SCHEMA;
import static com.bytechef.component.google.calendar.action.GoogleCalendarCreateQuickEventAction.PROPERTIES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CREATE_QUICK_EVENT;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CREATE_QUICK_EVENT_DESCRIPTION;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CREATE_QUICK_EVENT_TITLE;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ai.agent.SingleConnectionToolFunction;
import com.bytechef.component.google.calendar.action.GoogleCalendarCreateQuickEventAction;

/**
 * @author Monika Kušter
 */
public class GoogleCalendarCreateQuickEventTool {

    public static final ClusterElementDefinition<SingleConnectionToolFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<SingleConnectionToolFunction>clusterElement(CREATE_QUICK_EVENT)
            .title(CREATE_QUICK_EVENT_TITLE)
            .description(CREATE_QUICK_EVENT_DESCRIPTION)
            .type(TOOLS)
            .properties(PROPERTIES)
            .output(OUTPUT_SCHEMA)
            .object(() -> GoogleCalendarCreateQuickEventAction::perform);
}
