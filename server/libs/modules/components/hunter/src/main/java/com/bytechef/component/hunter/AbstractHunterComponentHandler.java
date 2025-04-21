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

package com.bytechef.component.hunter;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.hunter.action.HunterCombinedEnrichmentAction;
import com.bytechef.component.hunter.action.HunterCompanyEnrichmentAction;
import com.bytechef.component.hunter.action.HunterCreateLeadAction;
import com.bytechef.component.hunter.action.HunterEmailEnrichmentAction;
import com.bytechef.component.hunter.connection.HunterConnection;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractHunterComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("hunter")
            .title("Hunter")
            .description(
                "Hunter is a tool that helps users find and verify professional email addresses, enabling effective outreach and communication."))
                    .actions(modifyActions(HunterEmailEnrichmentAction.ACTION_DEFINITION,
                        HunterCompanyEnrichmentAction.ACTION_DEFINITION,
                        HunterCombinedEnrichmentAction.ACTION_DEFINITION, HunterCreateLeadAction.ACTION_DEFINITION))
                    .connection(modifyConnection(HunterConnection.CONNECTION_DEFINITION))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
