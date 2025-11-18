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

package com.bytechef.platform.component.oas.handler;

import static com.github.tomakehurst.wiremock.client.WireMock.badRequest;
import static com.github.tomakehurst.wiremock.client.WireMock.binaryEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.exception.TaskExecutionException;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.component.config.ComponentRegistryConfiguration;
import com.bytechef.platform.component.config.ComponentRegistryConfigurationSharedMocks;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.context.FileEntryImpl;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.constant.ModeType;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import wiremock.com.fasterxml.jackson.databind.node.JsonNodeFactory;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ComponentRegistryConfiguration.class)
@ComponentRegistryConfigurationSharedMocks
public class OpenApiComponentTaskHandlerIntTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig()
            .port(9999)
            .http2PlainDisabled(true))
        .configureStaticDsl(true)
        .build();

    @Autowired
    private ConnectionRepository connectionRepository;

    private Connection connection;

    @Autowired
    private ActionDefinitionFacade actionDefinitionFacade;

    @BeforeEach
    public void beforeEach() {
        connectionRepository.deleteAll();

        connection = new Connection();

        connection.setAuthorizationType(AuthorizationType.API_KEY);
        connection.setComponentName("petstore");
        connection.setName("PetShop Connection");

        connection = connectionRepository.save(connection);
    }

    @Test
    public void testHandleDELETE() throws Exception {
        stubFor(delete("/pet/1").willReturn(ok()));

        OpenApiComponentTaskHandler openApiComponentTaskHandler = createOpenApiComponentHandler(
            "deletePet");

        TaskExecution taskExecution = getTaskExecution(Map.of("petId", 1));

        Object result = openApiComponentTaskHandler.handle(taskExecution);

        Assertions.assertNull(result);

        //

        stubFor(delete("/store/order/1").willReturn(ok()));

        openApiComponentTaskHandler = createOpenApiComponentHandler("deleteOrder");

        taskExecution = getTaskExecution(Map.of("orderId", 1));

        result = openApiComponentTaskHandler.handle(taskExecution);

        Assertions.assertNull(result);

        //

        stubFor(delete("/user/user1").willReturn(ok()));

        openApiComponentTaskHandler = createOpenApiComponentHandler("deleteUser");

        taskExecution = getTaskExecution(Map.of("username", "user1"));

        result = openApiComponentTaskHandler.handle(taskExecution);

        Assertions.assertNull(result);
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testHandleGET() throws Exception {
        String json = """
            [
              {
                "id": 4,
                "category": {
                  "id": 1,
                  "name": "Dogs"
                },
                "name": "Dog 1",
                "photoUrls": [
                  "url1",
                  "url2"
                ],
                "tags": [
                  {
                    "id": 1,
                    "name": "tag1"
                  },
                  {
                    "id": 2,
                    "name": "tag2"
                  }
                ],
                "status": "available"
              },
              {
                "id": 7,
                "category": {
                  "id": 4,
                  "name": "Lions"
                },
                "name": "Lion 1",
                "photoUrls": [
                  "url1",
                  "url2"
                ],
                "tags": [
                  {
                    "id": 1,
                    "name": "tag1"
                  },
                  {
                    "id": 2,
                    "name": "tag2"
                  }
                ],
                "status": "available"
              }
            ]
            """;

        stubFor(
            get(urlPathEqualTo("/pet/findByStatus"))
                .withQueryParam("status", equalTo("available"))
                .willReturn(
                    ok()
                        .withBody(json)
                        .withHeader("Content-Type", "application/json")));

        OpenApiComponentTaskHandler openApiComponentTaskHandler = createOpenApiComponentHandler(
            "findPetsByStatus");

        TaskExecution taskExecution = getTaskExecution(Map.of("status", "available"));

        List<?> list = (List<?>) openApiComponentTaskHandler.handle(taskExecution);

        JSONAssert.assertEquals(json, new JSONArray(list), true);

        //

        stubFor(
            get(urlPathEqualTo("/pet/findByTags"))
                .withQueryParam("tags", equalTo("tag1"))
                .willReturn(
                    ok()
                        .withBody(json)
                        .withHeader("Content-Type", "application/json")));

        openApiComponentTaskHandler = createOpenApiComponentHandler("findPetsByTags");

        taskExecution = getTaskExecution(Map.of("tags", List.of("tag1")));

        list = (List<?>) openApiComponentTaskHandler.handle(taskExecution);

        JSONAssert.assertEquals(json, new JSONArray(list), true);

        //

        json = """
            {
                    "id": 7,
                    "category": {
                      "id": 4,
                      "name": "Lions"
                    },
                    "name": "Lion 1",
                    "photoUrls": [
                      "url1",
                      "url2"
                    ],
                    "tags": [
                      {
                        "id": 1,
                        "name": "tag1"
                      },
                      {
                        "id": 2,
                        "name": "tag2"
                      }
                    ],
                    "status": "available"
                  }
            """;

        stubFor(
            get(
                urlPathEqualTo("/pet/7")).willReturn(
                    ok()
                        .withBody(json)
                        .withHeader("Content-Type", "application/json")));

        openApiComponentTaskHandler = createOpenApiComponentHandler("getPetById");

        taskExecution = getTaskExecution(Map.of("petId", 7));

        Map<?, ?> map = (Map<?, ?>) openApiComponentTaskHandler.handle(taskExecution);

        JSONAssert.assertEquals(json, new JSONObject(map), true);

        //

        json = """
            {
              "approved": 921,
              "delivered": 50
            }
            """;

        stubFor(
            get(
                urlPathEqualTo("/store/inventory")).willReturn(
                    ok()
                        .withBody(json)
                        .withHeader("Content-Type", "application/json")));

        openApiComponentTaskHandler = createOpenApiComponentHandler("getInventory");

        taskExecution = getTaskExecution(Collections.emptyMap());

        map = (Map<?, ?>) openApiComponentTaskHandler.handle(taskExecution);

        JSONAssert.assertEquals(json, new JSONObject(map), true);

        //

        json = """
            {
              "id": 10,
              "petId": 198772,
              "quantity": 7,
              "shipDate": "2023-02-02T11:09:23.082Z",
              "status": "approved",
              "complete": true
            }
            """;

        stubFor(
            get(
                urlPathEqualTo("/store/order/3")).willReturn(
                    ok()
                        .withBody(json)
                        .withHeader("Content-Type", "application/json")));

        openApiComponentTaskHandler = createOpenApiComponentHandler("getOrderById");

        taskExecution = getTaskExecution(Map.of("orderId", 3));

        map = (Map<?, ?>) openApiComponentTaskHandler.handle(taskExecution);

        JSONAssert.assertEquals(json, new JSONObject(map), true);

        //

//        stubFor(
//            get(
//                urlPathEqualTo("/user/login"))
//                    .willReturn(
//                        ok()
//                            .withBody("Logged in user session: 7284289668658063360")
//                            .withHeader("Content-Type", "text/plain")));
//
//        openApiComponentActionTaskHandler = createOpenApiComponentHandler("loginUser");
//
//        taskExecution = getTaskExecution(Map.of());
//
//        response = (Response) openApiComponentActionTaskHandler.handle(taskExecution);
//
//        Assertions.assertEquals(200, response.getStatusCode());
//        Assertions.assertEquals("Logged in user session: 7284289668658063360", response.getBody());

        //

//        stubFor(get(urlPathEqualTo("/user/logout")).willReturn(ok()));
//
//        openApiComponentActionTaskHandler = createOpenApiComponentHandler("logoutUser");
//
//        taskExecution = getTaskExecution(Map.of());
//
//        response = (Response) openApiComponentActionTaskHandler.handle(taskExecution);
//
//        Assertions.assertEquals(200, response.getStatusCode());
//        Assertions.assertNull(response.getBody());

        //

        json = """
            {
              "id": 1,
              "username": "user1",
              "firstName": "first name 1",
              "lastName": "last name 1",
              "email": "email1@test.com",
              "password": "XXXXXXXXXXX",
              "phone": "123-456-7890",
              "userStatus": 1
            }
            """;

        stubFor(
            get(
                urlPathEqualTo("/user/user1")).willReturn(
                    ok()
                        .withBody(json)
                        .withHeader("Content-Type", "application/json")));

        openApiComponentTaskHandler = createOpenApiComponentHandler("getUserByName");

        taskExecution = getTaskExecution(Map.of("username", "user1"));

        map = (Map<?, ?>) openApiComponentTaskHandler.handle(taskExecution);

        JSONAssert.assertEquals(json, new JSONObject(map), true);
    }

    @Test
    public void testHandleGETStatus400() {
        OpenApiComponentTaskHandler openApiComponentTaskHandler;
        TaskExecution taskExecution;

        //

        stubFor(
            get(urlPathEqualTo("/pet/findByStatus"))
                .withQueryParam("status", equalTo("unknown"))
                .willReturn(
                    badRequest()
                        .withJsonBody(
                            JsonNodeFactory.instance.pojoNode(
                                Map.of(
                                    "code", 400,
                                    "message", "Input error: query parameter `status value `unknown` is not in " +
                                        "the allowable values `[available, pending, sold]`")))
                        .withHeader("Content-Type", "application/json")));

        openApiComponentTaskHandler = createOpenApiComponentHandler("findPetsByStatus");

        taskExecution = getTaskExecution(Map.of("status", "unknown"));

        assertThrows(TaskExecutionException.class, () -> openApiComponentTaskHandler.handle(taskExecution));
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void testHandlePOST() throws Exception {
        String json = """
            {
              "id": 10,
              "name": "doggie",
              "category": {
                "id": 1
              },
              "photoUrls": [
                "string"
              ],
              "status": "available",
              "tags": [
                {
                  "name": "tag1"
                }
              ]
            }
            """;

        stubFor(
            post(urlPathEqualTo("/pet"))
                .withRequestBody(equalToJson(json))
                .willReturn(
                    ok()
                        .withBody(json)
                        .withHeader("Content-Type", "application/json")));

        OpenApiComponentTaskHandler openApiComponentTaskHandler = createOpenApiComponentHandler("addPet");

        TaskExecution taskExecution = getTaskExecution(
            new LinkedHashMap<>() {
                {
                    put("id", 10);
                    put("name", "doggie");
                    put("category", Map.of("id", 1));
                    put("photoUrls", List.of("string"));
                    put("status", "available");
                    put("tags", List.of(Map.of("name", "tag1")));
                }
            });

        Map<?, ?> map = (Map<?, ?>) openApiComponentTaskHandler.handle(taskExecution);

        JSONAssert.assertEquals(json, new JSONObject(map), true);

        //

        stubFor(
            post(urlPathEqualTo("/pet/10"))
                .willReturn(
                    ok()
                        .withBody(json)
                        .withHeader("Content-Type", "application/json")));

        openApiComponentTaskHandler = createOpenApiComponentHandler("updatePetWithForm");

        taskExecution = getTaskExecution(Map.of("petId", 10, "name", "doggie", "status", "available"));

        Object result = openApiComponentTaskHandler.handle(taskExecution);

        Assertions.assertNull(result);

        //

        json = """
            {
              "id": 10,
              "category": {
                "id": 1,
                "name": "Dogs"
              },
              "name": "doggie",
              "photoUrls": [
                "string",
                "/tmp/inflector8740325056085860509.tmp"
              ],
              "tags": [
                {
                  "id": 0,
                  "name": "string"
                }
              ],
              "status": "available"
            }
            """;

        stubFor(
            post(urlPathEqualTo("/pet/10/uploadImage"))
                .withRequestBody(binaryEqualTo("This is text".getBytes(StandardCharsets.UTF_8)))
                .withHeader("Content-Type", equalTo("application/octet-stream"))
                .willReturn(
                    ok()
                        .withBody(json)
                        .withHeader("Content-Type", "application/json")));

        openApiComponentTaskHandler = createOpenApiComponentHandler("uploadFile");

        FileEntry fileEntry = ComponentRegistryConfiguration.FILES_FILE_STORAGE.storeFileContent(
            "text.txt", "This is text");

        taskExecution = getTaskExecution(Map.of("petId", 10, "fileEntry", new FileEntryImpl(fileEntry)));

        map = (Map<?, ?>) openApiComponentTaskHandler.handle(taskExecution);

        JSONAssert.assertEquals(json, new JSONObject(map), true);

        // next: place order

        json = """
            {
              "id": 10,
              "petId": 198772,
              "quantity": 7,
              "shipDate": "2023-01-23T07:45:00",
              "status": "approved",
              "complete": true
            }
            """;

        stubFor(
            post(urlPathEqualTo("/store/order"))
                .withRequestBody(equalToJson(json))
                .willReturn(
                    ok()
                        .withBody(json)
                        .withHeader("Content-Type", "application/json")));

        openApiComponentTaskHandler = createOpenApiComponentHandler("placeOrder");

        taskExecution = getTaskExecution(
            new LinkedHashMap<>() {
                {
                    put("id", 10);
                    put("petId", 198772);
                    put("quantity", 7);
                    put("shipDate", LocalDateTime.of(2023, 1, 23, 7, 45));
                    put("status", "approved");
                    put("complete", true);
                }
            });

        map = (Map<?, ?>) openApiComponentTaskHandler.handle(taskExecution);

        JSONAssert.assertEquals(json, new JSONObject(map), true);

        //

        json = """
            {
              "id": 10,
              "username": "user1",
              "firstName": "John",
              "lastName": "James",
              "email": "john@email.com",
              "password": "12345",
              "phone": "12345",
              "userStatus": 1
            }
            """;

        stubFor(
            post(urlPathEqualTo("/user"))
                .withRequestBody(equalToJson(json))
                .willReturn(
                    ok()
                        .withBody(json)
                        .withHeader("Content-Type", "application/json")));

        openApiComponentTaskHandler = createOpenApiComponentHandler("createUser");

        taskExecution = getTaskExecution(
            new LinkedHashMap<>() {
                {
                    put("id", 10);
                    put("username", "user1");
                    put("firstName", "John");
                    put("lastName", "James");
                    put("email", "john@email.com");
                    put("password", "12345");
                    put("phone", "12345");
                    put("userStatus", 1);
                }
            });

        map = (Map<?, ?>) openApiComponentTaskHandler.handle(taskExecution);

        JSONAssert.assertEquals(json, new JSONObject(map), true);

        //

        json = """
            [
                {
                  "id": 10,
                  "username": "user1",
                  "firstName": "John",
                  "lastName": "James",
                  "email": "john@email.com",
                  "password": "12345",
                  "phone": "12345",
                  "userStatus": 1
                }
            ]
            """;

        stubFor(
            post(urlPathEqualTo("/user/createWithList"))
                .withRequestBody(equalToJson(json))
                .willReturn(
                    ok()
                        .withBody(json)
                        .withHeader("Content-Type", "application/json")));

        openApiComponentTaskHandler = createOpenApiComponentHandler("createUsersWithListInput");

        taskExecution = getTaskExecution(
            Map.of(
                "__items", List.of(
                    new LinkedHashMap<String, Object>() {
                        {
                            put("id", 10);
                            put("username", "user1");
                            put("firstName", "John");
                            put("lastName", "James");
                            put("email", "john@email.com");
                            put("password", "12345");
                            put("phone", "12345");
                            put("userStatus", 1);
                        }
                    })));

        List<?> list = (List<?>) openApiComponentTaskHandler.handle(taskExecution);

        JSONAssert.assertEquals(json, new JSONArray(list), true);
    }

    @Test
    public void testHandlePUT() throws Exception {
        String json = """
            {
              "id": 10,
              "name": "doggie",
              "category": {
                "id": 1
              },
              "photoUrls": [
                "string"
              ],
              "status": "available",
              "tags": [
                {
                  "name": "tag1"
                }
              ]
            }
            """;

        stubFor(
            put(urlPathEqualTo("/pet"))
                .withRequestBody(equalToJson(json))
                .willReturn(
                    ok()
                        .withBody(json)
                        .withHeader("Content-Type", "application/json")));

        OpenApiComponentTaskHandler openApiComponentTaskHandler = createOpenApiComponentHandler(
            "updatePet");

        TaskExecution taskExecution = getTaskExecution(
            new LinkedHashMap<>() {
                {
                    put("id", 10);
                    put("name", "doggie");
                    put("category", Map.of("id", 1));
                    put("photoUrls", List.of("string"));
                    put("status", "available");
                    put("tags", List.of(Map.of("name", "tag1")));
                }
            });

        Map<?, ?> result = (Map<?, ?>) openApiComponentTaskHandler.handle(taskExecution);

        JSONAssert.assertEquals(json, new JSONObject(result), true);

        //

        json = """
            {
              "id": 10,
              "username": "user1",
              "firstName": "John",
              "lastName": "James",
              "email": "john@email.com",
              "password": "12345",
              "phone": "12345",
              "userStatus": 1
            }
            """;

        stubFor(
            put(urlPathEqualTo("/user/user1"))
                .withRequestBody(equalToJson(json))
                .willReturn(
                    ok()
                        .withBody(json)
                        .withHeader("Content-Type", "application/json")));

        openApiComponentTaskHandler = createOpenApiComponentHandler("updateUser");

        taskExecution = getTaskExecution(
            new LinkedHashMap<>() {
                {
                    put("id", 10);
                    put("username", "user1");
                    put("firstName", "John");
                    put("lastName", "James");
                    put("email", "john@email.com");
                    put("password", "12345");
                    put("phone", "12345");
                    put("userStatus", 1);
                }
            });

        Map<?, ?> map = (Map<?, ?>) openApiComponentTaskHandler.handle(taskExecution);

        JSONAssert.assertEquals(json, new JSONObject(map), true);
    }

    private OpenApiComponentTaskHandler createOpenApiComponentHandler(String actionName) {
        return new OpenApiComponentTaskHandler(
            actionName, actionDefinitionFacade, ComponentRegistryConfiguration.PETSTORE_COMPONENT_HANDLER);
    }

    private TaskExecution getTaskExecution(Map<String, Object> parameters) {
        return TaskExecution.builder()
            .id(1L)
            .jobId(1L)
            .metadata(
                Map.of(
                    MetadataConstants.CONNECTION_IDS, Map.of("petstore", Validate.notNull(connection.getId(), "id")),
                    MetadataConstants.TYPE, ModeType.AUTOMATION))
            .workflowTask(
                new WorkflowTask(
                    Map.of(
                        WorkflowConstants.NAME, "name",
                        WorkflowConstants.TYPE, "type",
                        WorkflowConstants.PARAMETERS, parameters)))
            .build();
    }

}
