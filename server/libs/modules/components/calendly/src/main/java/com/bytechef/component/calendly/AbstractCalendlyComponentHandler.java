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

package com.bytechef.component.calendly;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.calendly.action.CalendlyCancelEventAction;
import com.bytechef.component.calendly.connection.CalendlyConnection;
import com.bytechef.component.definition.ComponentDefinition;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractCalendlyComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("calendly")
            .title("Calendly")
            .description(
                "Calendly is a scheduling tool that allows users to easily set up and manage appointments and meetings."))
                    .actions(modifyActions(CalendlyCancelEventAction.ACTION_DEFINITION))
                    .connection(modifyConnection(CalendlyConnection.CONNECTION_DEFINITION))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
