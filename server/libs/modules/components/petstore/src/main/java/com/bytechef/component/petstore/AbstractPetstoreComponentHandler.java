
/*
 * Copyright 2021 <your company/name>.
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

import static com.bytechef.hermes.component.definition.ComponentDSL.component;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;

import com.bytechef.component.petstore.action.AddPetAction;
import com.bytechef.component.petstore.action.CreateUserAction;
import com.bytechef.component.petstore.action.CreateUsersWithListInputAction;
import com.bytechef.component.petstore.action.DeleteOrderAction;
import com.bytechef.component.petstore.action.DeletePetAction;
import com.bytechef.component.petstore.action.DeleteUserAction;
import com.bytechef.component.petstore.action.FindPetsByStatusAction;
import com.bytechef.component.petstore.action.FindPetsByTagsAction;
import com.bytechef.component.petstore.action.GetInventoryAction;
import com.bytechef.component.petstore.action.GetOrderByIdAction;
import com.bytechef.component.petstore.action.GetPetByIdAction;
import com.bytechef.component.petstore.action.GetUserByNameAction;
import com.bytechef.component.petstore.action.LoginUserAction;
import com.bytechef.component.petstore.action.LogoutUserAction;
import com.bytechef.component.petstore.action.PlaceOrderAction;
import com.bytechef.component.petstore.action.UpdatePetAction;
import com.bytechef.component.petstore.action.UpdatePetWithFormAction;
import com.bytechef.component.petstore.action.UpdateUserAction;
import com.bytechef.component.petstore.action.UploadFileAction;
import com.bytechef.component.petstore.connection.PetstoreConnection;
import com.bytechef.hermes.component.OpenApiComponentHandler;
import com.bytechef.hermes.component.definition.ComponentDefinition;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractPetstoreComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = component("petstore")
        .display(
            modifyDisplay(
                display("Petstore")
                    .description("This is a sample Pet Store Server based on the OpenAPI 3.0 specification.")))
        .actions(modifyActions(AddPetAction.ACTION_DEFINITION, UpdatePetAction.ACTION_DEFINITION,
            FindPetsByStatusAction.ACTION_DEFINITION, FindPetsByTagsAction.ACTION_DEFINITION,
            DeletePetAction.ACTION_DEFINITION, GetPetByIdAction.ACTION_DEFINITION,
            UpdatePetWithFormAction.ACTION_DEFINITION, UploadFileAction.ACTION_DEFINITION,
            GetInventoryAction.ACTION_DEFINITION, PlaceOrderAction.ACTION_DEFINITION,
            DeleteOrderAction.ACTION_DEFINITION, GetOrderByIdAction.ACTION_DEFINITION,
            CreateUserAction.ACTION_DEFINITION, CreateUsersWithListInputAction.ACTION_DEFINITION,
            LoginUserAction.ACTION_DEFINITION, LogoutUserAction.ACTION_DEFINITION, DeleteUserAction.ACTION_DEFINITION,
            GetUserByNameAction.ACTION_DEFINITION, UpdateUserAction.ACTION_DEFINITION))
        .connection(modifyConnection(PetstoreConnection.CONNECTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
