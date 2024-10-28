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

package com.bytechef.component.accelo;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.accelo.action.AcceloCreateCompanyAction;
import com.bytechef.component.accelo.action.AcceloCreateContactAction;
import com.bytechef.component.accelo.connection.AcceloConnection;
import com.bytechef.component.definition.ComponentDefinition;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractAcceloComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("accelo")
            .title("Accelo")
            .description(
                "Accelo is a cloud-based platform designed to streamline operations for service businesses by integrating project management, CRM, and billing functionalities into one unified system."))
                    .actions(modifyActions(AcceloCreateCompanyAction.ACTION_DEFINITION,
                        AcceloCreateContactAction.ACTION_DEFINITION))
                    .connection(modifyConnection(AcceloConnection.CONNECTION_DEFINITION))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
