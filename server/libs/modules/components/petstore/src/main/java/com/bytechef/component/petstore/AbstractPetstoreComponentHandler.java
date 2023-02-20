
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
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.array;
import static com.bytechef.hermes.component.definition.ComponentDSL.authorization;
import static com.bytechef.hermes.component.definition.ComponentDSL.component;
import static com.bytechef.hermes.component.definition.ComponentDSL.connection;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.option;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static com.bytechef.hermes.component.utils.HttpClientUtils.BodyContentType;
import static com.bytechef.hermes.component.utils.HttpClientUtils.ResponseFormat;

import com.bytechef.component.petstore.schema.ApiResponseSchema;
import com.bytechef.component.petstore.schema.OrderSchema;
import com.bytechef.component.petstore.schema.PetSchema;
import com.bytechef.component.petstore.schema.UserSchema;
import com.bytechef.hermes.component.RestComponentHandler;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import java.util.List;
import java.util.Map;

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
        .actions(modifyActions(action("addPet")
            .display(
                display("Add a new pet to the store")
                    .description("Add a new pet to the store"))
            .metadata(
                Map.of(
                    "requestMethod", "POST",
                    "path", "/pet", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

                ))
            .properties(object("pet").properties(PetSchema.COMPONENT_SCHEMA)
                .label("Pet")
                .required(true)
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY)))
            .output(object(null).properties(PetSchema.COMPONENT_SCHEMA)
                .metadata(
                    Map.of(
                        "responseFormat", ResponseFormat.JSON))),
            action("updatePet")
                .display(
                    display("Update an existing pet")
                        .description("Update an existing pet by Id"))
                .metadata(
                    Map.of(
                        "requestMethod", "PUT",
                        "path", "/pet", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

                    ))
                .properties(object("pet").properties(PetSchema.COMPONENT_SCHEMA)
                    .label("Pet")
                    .required(true)
                    .metadata(
                        Map.of(
                            "type", PropertyType.BODY)))
                .output(object(null).properties(PetSchema.COMPONENT_SCHEMA)
                    .metadata(
                        Map.of(
                            "responseFormat", ResponseFormat.JSON))),
            action("findPetsByStatus")
                .display(
                    display("Finds Pets by status")
                        .description("Multiple status values can be provided with comma separated strings"))
                .metadata(
                    Map.of(
                        "requestMethod", "GET",
                        "path", "/pet/findByStatus"

                    ))
                .properties(string("status").label("Status")
                    .description("Status values that need to be considered for filter")
                    .options(option("Available", "available"), option("Pending", "pending"), option("Sold", "sold"))
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", PropertyType.QUERY)))
                .output(array("array").items(object(null).properties(PetSchema.COMPONENT_SCHEMA))
                    .metadata(
                        Map.of(
                            "responseFormat", ResponseFormat.JSON))),
            action("findPetsByTags")
                .display(
                    display("Finds Pets by tags")
                        .description(
                            "Multiple tags can be provided with comma separated strings. Use tag1, tag2, tag3 for testing."))
                .metadata(
                    Map.of(
                        "requestMethod", "GET",
                        "path", "/pet/findByTags"

                    ))
                .properties(array("tags").items(string(null))
                    .placeholder("Add")
                    .label("Tags")
                    .description("Tags to filter by")
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", PropertyType.QUERY)))
                .output(array("array").items(object(null).properties(PetSchema.COMPONENT_SCHEMA))
                    .metadata(
                        Map.of(
                            "responseFormat", ResponseFormat.JSON))),
            action("deletePet")
                .display(
                    display("Deletes a pet")
                        .description("delete a pet"))
                .metadata(
                    Map.of(
                        "requestMethod", "DELETE",
                        "path", "/pet/{petId}"

                    ))
                .properties(string("api_key").label("Api_key")
                    .description("")
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", PropertyType.HEADER)),
                    integer("petId").label("PetId")
                        .description("Pet id to delete")
                        .required(true)
                        .metadata(
                            Map.of(
                                "type", PropertyType.PATH))),
            action("getPetById")
                .display(
                    display("Find pet by ID")
                        .description("Returns a single pet"))
                .metadata(
                    Map.of(
                        "requestMethod", "GET",
                        "path", "/pet/{petId}"

                    ))
                .properties(integer("petId").label("PetId")
                    .description("ID of pet to return")
                    .required(true)
                    .metadata(
                        Map.of(
                            "type", PropertyType.PATH)))
                .output(object(null).properties(PetSchema.COMPONENT_SCHEMA)
                    .metadata(
                        Map.of(
                            "responseFormat", ResponseFormat.JSON))),
            action("updatePetWithForm")
                .display(
                    display("Updates a pet in the store with form data")
                        .description(""))
                .metadata(
                    Map.of(
                        "requestMethod", "POST",
                        "path", "/pet/{petId}"

                    ))
                .properties(integer("petId").label("PetId")
                    .description("ID of pet that needs to be updated")
                    .required(true)
                    .metadata(
                        Map.of(
                            "type", PropertyType.PATH)),
                    string("name").label("Name")
                        .description("Name of pet that needs to be updated")
                        .required(false)
                        .metadata(
                            Map.of(
                                "type", PropertyType.QUERY)),
                    string("status").label("Status")
                        .description("Status of pet that needs to be updated")
                        .required(false)
                        .metadata(
                            Map.of(
                                "type", PropertyType.QUERY))),
            action("uploadFile")
                .display(
                    display("uploads an image")
                        .description(""))
                .metadata(
                    Map.of(
                        "requestMethod", "POST",
                        "path", "/pet/{petId}/uploadImage", "bodyContentType", BodyContentType.BINARY, "mimeType",
                        "application/octet-stream"

                    ))
                .properties(integer("petId").label("PetId")
                    .description("ID of pet to update")
                    .required(true)
                    .metadata(
                        Map.of(
                            "type", PropertyType.PATH)),
                    string("additionalMetadata").label("AdditionalMetadata")
                        .description("Additional Metadata")
                        .required(false)
                        .metadata(
                            Map.of(
                                "type", PropertyType.QUERY)),
                    fileEntry("fileEntry").metadata(
                        Map.of(
                            "type", PropertyType.BODY)))
                .output(object(null).properties(ApiResponseSchema.COMPONENT_SCHEMA)
                    .metadata(
                        Map.of(
                            "responseFormat", ResponseFormat.JSON))),
            action("getInventory")
                .display(
                    display("Returns pet inventories by status")
                        .description("Returns a map of status codes to quantities"))
                .metadata(
                    Map.of(
                        "requestMethod", "GET",
                        "path", "/store/inventory"

                    ))
                .properties()
                .output(object(null).additionalProperties(integer())
                    .metadata(
                        Map.of(
                            "responseFormat", ResponseFormat.JSON))),
            action("placeOrder")
                .display(
                    display("Place an order for a pet")
                        .description("Place a new order in the store"))
                .metadata(
                    Map.of(
                        "requestMethod", "POST",
                        "path", "/store/order", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

                    ))
                .properties(object("order").properties(OrderSchema.COMPONENT_SCHEMA)
                    .label("Order")
                    .metadata(
                        Map.of(
                            "type", PropertyType.BODY)))
                .output(object(null).properties(OrderSchema.COMPONENT_SCHEMA)
                    .metadata(
                        Map.of(
                            "responseFormat", ResponseFormat.JSON))),
            action("deleteOrder")
                .display(
                    display("Delete purchase order by ID")
                        .description(
                            "For valid response try integer IDs with value < 1000. Anything above 1000 or nonintegers will generate API errors"))
                .metadata(
                    Map.of(
                        "requestMethod", "DELETE",
                        "path", "/store/order/{orderId}"

                    ))
                .properties(integer("orderId").label("OrderId")
                    .description("ID of the order that needs to be deleted")
                    .required(true)
                    .metadata(
                        Map.of(
                            "type", PropertyType.PATH))),
            action("getOrderById")
                .display(
                    display("Find purchase order by ID")
                        .description(
                            "For valid response try integer IDs with value <= 5 or > 10. Other values will generate exceptions."))
                .metadata(
                    Map.of(
                        "requestMethod", "GET",
                        "path", "/store/order/{orderId}"

                    ))
                .properties(integer("orderId").label("OrderId")
                    .description("ID of order that needs to be fetched")
                    .required(true)
                    .metadata(
                        Map.of(
                            "type", PropertyType.PATH)))
                .output(object(null).properties(OrderSchema.COMPONENT_SCHEMA)
                    .metadata(
                        Map.of(
                            "responseFormat", ResponseFormat.JSON))),
            action("createUser")
                .display(
                    display("Create user")
                        .description("This can only be done by the logged in user."))
                .metadata(
                    Map.of(
                        "requestMethod", "POST",
                        "path", "/user", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

                    ))
                .properties(object("user").properties(UserSchema.COMPONENT_SCHEMA)
                    .label("User")
                    .metadata(
                        Map.of(
                            "type", PropertyType.BODY)))
                .output(object(null).properties(UserSchema.COMPONENT_SCHEMA)
                    .metadata(
                        Map.of(
                            "responseFormat", ResponseFormat.JSON))),
            action("createUsersWithListInput")
                .display(
                    display("Creates list of users with given input array")
                        .description("Creates list of users with given input array"))
                .metadata(
                    Map.of(
                        "requestMethod", "POST",
                        "path", "/user/createWithList", "bodyContentType", BodyContentType.JSON, "mimeType",
                        "application/json"

                    ))
                .properties(array("array").items(object(null).properties(UserSchema.COMPONENT_SCHEMA))
                    .placeholder("Add")
                    .metadata(
                        Map.of(
                            "type", PropertyType.BODY)))
                .output(array("array").items(object(null).properties(UserSchema.COMPONENT_SCHEMA))
                    .metadata(
                        Map.of(
                            "responseFormat", ResponseFormat.JSON))),
            action("loginUser")
                .display(
                    display("Logs user into the system")
                        .description(""))
                .metadata(
                    Map.of(
                        "requestMethod", "GET",
                        "path", "/user/login"

                    ))
                .properties(string("username").label("Username")
                    .description("The user name for login")
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", PropertyType.QUERY)),
                    string("password").label("Password")
                        .description("The password for login in clear text")
                        .required(false)
                        .metadata(
                            Map.of(
                                "type", PropertyType.QUERY)))
                .output(string(null).metadata(
                    Map.of(
                        "responseFormat", ResponseFormat.TEXT))),
            action("logoutUser")
                .display(
                    display("Logs out current logged in user session")
                        .description(""))
                .metadata(
                    Map.of(
                        "requestMethod", "GET",
                        "path", "/user/logout"

                    ))
                .properties(),
            action("deleteUser")
                .display(
                    display("Delete user")
                        .description("This can only be done by the logged in user."))
                .metadata(
                    Map.of(
                        "requestMethod", "DELETE",
                        "path", "/user/{username}"

                    ))
                .properties(string("username").label("Username")
                    .description("The name that needs to be deleted")
                    .required(true)
                    .metadata(
                        Map.of(
                            "type", PropertyType.PATH))),
            action("getUserByName")
                .display(
                    display("Get user by user name")
                        .description(""))
                .metadata(
                    Map.of(
                        "requestMethod", "GET",
                        "path", "/user/{username}"

                    ))
                .properties(string("username").label("Username")
                    .description("The name that needs to be fetched. Use user1 for testing. ")
                    .required(true)
                    .metadata(
                        Map.of(
                            "type", PropertyType.PATH)))
                .output(object(null).properties(UserSchema.COMPONENT_SCHEMA)
                    .metadata(
                        Map.of(
                            "responseFormat", ResponseFormat.JSON))),
            action("updateUser")
                .display(
                    display("Update user")
                        .description("This can only be done by the logged in user."))
                .metadata(
                    Map.of(
                        "requestMethod", "PUT",
                        "path", "/user/{username}", "bodyContentType", BodyContentType.JSON, "mimeType",
                        "application/json"

                    ))
                .properties(string("username").label("Username")
                    .description("name that need to be deleted")
                    .required(true)
                    .metadata(
                        Map.of(
                            "type", PropertyType.PATH)),
                    object("user").properties(UserSchema.COMPONENT_SCHEMA)
                        .label("User")
                        .metadata(
                            Map.of(
                                "type", PropertyType.BODY)))
                .output(object(null).properties(UserSchema.COMPONENT_SCHEMA)
                    .metadata(
                        Map.of(
                            "responseFormat", ResponseFormat.JSON)))))
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
