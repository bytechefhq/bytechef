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

package com.bytechef.component.petstore;

import static com.bytechef.component.definition.ComponentDSL.component;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.petstore.action.PetstoreAddPetAction;
import com.bytechef.component.petstore.action.PetstoreCreateUserAction;
import com.bytechef.component.petstore.action.PetstoreCreateUsersWithListInputAction;
import com.bytechef.component.petstore.action.PetstoreDeleteOrderAction;
import com.bytechef.component.petstore.action.PetstoreDeletePetAction;
import com.bytechef.component.petstore.action.PetstoreDeleteUserAction;
import com.bytechef.component.petstore.action.PetstoreFindPetsByStatusAction;
import com.bytechef.component.petstore.action.PetstoreFindPetsByTagsAction;
import com.bytechef.component.petstore.action.PetstoreGetInventoryAction;
import com.bytechef.component.petstore.action.PetstoreGetOrderByIdAction;
import com.bytechef.component.petstore.action.PetstoreGetPetByIdAction;
import com.bytechef.component.petstore.action.PetstoreGetUserByNameAction;
import com.bytechef.component.petstore.action.PetstorePlaceOrderAction;
import com.bytechef.component.petstore.action.PetstoreUpdatePetAction;
import com.bytechef.component.petstore.action.PetstoreUpdatePetWithFormAction;
import com.bytechef.component.petstore.action.PetstoreUpdateUserAction;
import com.bytechef.component.petstore.action.PetstoreUploadFileAction;
import com.bytechef.component.petstore.connection.PetstoreConnection;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractPetstoreComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("petstore")
            .title("Petstore")
            .description("This is a sample Pet Store Server based on the OpenAPI 3.0 specification."))
                .actions(modifyActions(PetstoreAddPetAction.ACTION_DEFINITION,
                    PetstoreUpdatePetAction.ACTION_DEFINITION, PetstoreFindPetsByStatusAction.ACTION_DEFINITION,
                    PetstoreFindPetsByTagsAction.ACTION_DEFINITION, PetstoreDeletePetAction.ACTION_DEFINITION,
                    PetstoreGetPetByIdAction.ACTION_DEFINITION, PetstoreUpdatePetWithFormAction.ACTION_DEFINITION,
                    PetstoreUploadFileAction.ACTION_DEFINITION, PetstoreGetInventoryAction.ACTION_DEFINITION,
                    PetstorePlaceOrderAction.ACTION_DEFINITION, PetstoreDeleteOrderAction.ACTION_DEFINITION,
                    PetstoreGetOrderByIdAction.ACTION_DEFINITION, PetstoreCreateUserAction.ACTION_DEFINITION,
                    PetstoreCreateUsersWithListInputAction.ACTION_DEFINITION,
                    PetstoreDeleteUserAction.ACTION_DEFINITION, PetstoreGetUserByNameAction.ACTION_DEFINITION,
                    PetstoreUpdateUserAction.ACTION_DEFINITION))
                .connection(modifyConnection(PetstoreConnection.CONNECTION_DEFINITION))
                .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
