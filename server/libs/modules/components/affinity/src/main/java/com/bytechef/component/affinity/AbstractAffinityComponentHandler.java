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

package com.bytechef.component.affinity;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.affinity.action.AffinityCreateOpportunityAction;
import com.bytechef.component.affinity.action.AffinityCreateOrganizationAction;
import com.bytechef.component.affinity.action.AffinityCreatePersonAction;
import com.bytechef.component.affinity.connection.AffinityConnection;
import com.bytechef.component.definition.ComponentDefinition;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractAffinityComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("affinity")
            .title("Affinity")
            .description(
                "Affinity is a customer relationship management (CRM) platform that leverages relationship intelligence to help businesses strengthen connections and drive engagement with client and prospects."))
                    .actions(modifyActions(AffinityCreateOpportunityAction.ACTION_DEFINITION,
                        AffinityCreateOrganizationAction.ACTION_DEFINITION,
                        AffinityCreatePersonAction.ACTION_DEFINITION))
                    .connection(modifyConnection(AffinityConnection.CONNECTION_DEFINITION))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
