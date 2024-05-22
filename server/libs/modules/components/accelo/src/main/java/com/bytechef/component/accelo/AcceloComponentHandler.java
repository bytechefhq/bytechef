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

import static com.bytechef.component.accelo.constant.AcceloConstants.ACCELO;
import static com.bytechef.component.definition.ComponentDSL.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.accelo.action.AcceloCreateCompanyAction;
import com.bytechef.component.accelo.action.AcceloCreateContactAction;
import com.bytechef.component.accelo.action.AcceloCreateTaskAction;
import com.bytechef.component.accelo.connection.AcceloConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 */
@AutoService(ComponentHandler.class)
public class AcceloComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(ACCELO)
        .title("Accelo")
        .description(
            "Accelo is a cloud-based platform designed to streamline operations for service businesses by " +
                "integrating project management, CRM, and billing functionalities into one unified system.")
        .icon("path:assets/accelo.svg")
        .categories(ComponentCategory.CRM, ComponentCategory.PROJECT_MANAGEMENT)
        .connection(AcceloConnection.CONNECTION_DEFINITION)
        .actions(
            AcceloCreateCompanyAction.ACTION_DEFINITION,
            AcceloCreateContactAction.ACTION_DEFINITION,
            AcceloCreateTaskAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
