
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

import static com.bytechef.hermes.component.constant.ComponentConstants.ADD_TO;
import static com.bytechef.hermes.component.constant.ComponentConstants.CLIENT_ID;
import static com.bytechef.hermes.component.constant.ComponentConstants.CLIENT_SECRET;
import static com.bytechef.hermes.component.constant.ComponentConstants.KEY;
import static com.bytechef.hermes.component.constant.ComponentConstants.VALUE;
import static com.bytechef.hermes.component.definition.Authorization.ApiTokenLocation;
import static com.bytechef.hermes.component.definition.Authorization.AuthorizationType;
import static com.bytechef.hermes.component.definition.ComponentDSL.authorization;
import static com.bytechef.hermes.component.definition.ComponentDSL.component;
import static com.bytechef.hermes.component.definition.ComponentDSL.connection;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

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
import com.bytechef.hermes.component.RestComponentHandler;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import java.util.List;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractPetstoreComponentHandler implements RestComponentHandler {
    private final ComponentDefinition componentDefinition = component("petstore")
        .display(
            modifyDisplay(
                display("Petstore")
                    .description(
                        "This is a sample Pet Store Server based on the OpenAPI 3.0 specification.  You can find out more about\n"
                            + "Swagger at [https://swagger.io](https://swagger.io). In the third iteration of the pet store, we've switched to the design first approach!\n"
                            + "You can now help us improve the API whether it's by making changes to the definition itself or to the code.\n"
                            + "That way, with time, we can improve the API in general, and expose some of the new features in OAS3.\n"
                            + "\n"
                            + "Some useful links:\n"
                            + "- [The Pet Store repository](https://github.com/swagger-api/swagger-petstore)\n"
                            + "- [The source API definition for the Pet Store](https://github.com/swagger-api/swagger-petstore/blob/master/src/main/resources/openapi.yaml)")))
        .actions(modifyActions(AddPetAction.ACTION_DEFINITION, UpdatePetAction.ACTION_DEFINITION,
            FindPetsByStatusAction.ACTION_DEFINITION, FindPetsByTagsAction.ACTION_DEFINITION,
            DeletePetAction.ACTION_DEFINITION, GetPetByIdAction.ACTION_DEFINITION,
            UpdatePetWithFormAction.ACTION_DEFINITION, UploadFileAction.ACTION_DEFINITION,
            GetInventoryAction.ACTION_DEFINITION, PlaceOrderAction.ACTION_DEFINITION,
            DeleteOrderAction.ACTION_DEFINITION, GetOrderByIdAction.ACTION_DEFINITION,
            CreateUserAction.ACTION_DEFINITION, CreateUsersWithListInputAction.ACTION_DEFINITION,
            LoginUserAction.ACTION_DEFINITION, LogoutUserAction.ACTION_DEFINITION, DeleteUserAction.ACTION_DEFINITION,
            GetUserByNameAction.ACTION_DEFINITION, UpdateUserAction.ACTION_DEFINITION))
        .connection(modifyConnection(
            connection()
                .baseUri(connection -> "https://petstore3.swagger.io/api/v3")
                .authorizations(authorization(
                    AuthorizationType.OAUTH2_IMPLICIT_CODE.name()
                        .toLowerCase(),
                    AuthorizationType.OAUTH2_IMPLICIT_CODE)
                        .display(
                            display("OAuth2 Implicit"))
                        .properties(
                            string(CLIENT_ID)
                                .label("Client Id")
                                .required(true),
                            string(CLIENT_SECRET)
                                .label("Client Secret")
                                .required(true))
                        .authorizationUrl(connection -> "https://petstore3.swagger.io/oauth/authorize")
                        .refreshUrl(connection -> null)
                        .scopes(connection -> List.of("write:pets", "read:pets")),
                    authorization(
                        AuthorizationType.API_KEY.name()
                            .toLowerCase(),
                        AuthorizationType.API_KEY)
                            .display(
                                display("API Key"))
                            .properties(
                                string(KEY)
                                    .label("Key")
                                    .required(true)
                                    .defaultValue("api_key")
                                    .hidden(true),
                                string(VALUE)
                                    .label("Value")
                                    .required(true),
                                string(ADD_TO)
                                    .label("Add to")
                                    .required(true)
                                    .defaultValue(ApiTokenLocation.HEADER.name())
                                    .hidden(true)))));

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
