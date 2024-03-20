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

package com.bytechef.platform.component.registry.oas.handler;

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

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.file.storage.TaskFileStorageImpl;
import com.bytechef.commons.data.jdbc.converter.EncryptedMapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.EncryptedStringToMapWrapperConverter;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDSL.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.petstore.PetstoreComponentHandler;
import com.bytechef.data.storage.service.DataStorageService;
import com.bytechef.encryption.Encryption;
import com.bytechef.encryption.EncryptionKey;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.registry.config.JacksonConfiguration;
import com.bytechef.platform.component.registry.definition.FileEntryImpl;
import com.bytechef.platform.component.registry.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.registry.oas.handler.loader.OpenApiComponentHandlerLoader;
import com.bytechef.platform.configuration.facade.WorkflowConnectionFacade;
import com.bytechef.platform.configuration.instance.accessor.InstanceAccessorRegistry;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.constant.Type;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.tag.service.TagService;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
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
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import wiremock.com.fasterxml.jackson.databind.node.JsonNodeFactory;

/**
 * @author Ivica Cardic
 */
@SpringBootTest
@Import({
    JacksonConfiguration.class, PostgreSQLContainerConfiguration.class
})
@WireMockTest(httpPort = 9999)
public class OpenApiComponentTaskHandlerIntTest {

    private static final FileStorageService FILE_STORAGE_SERVICE = new Base64FileStorageService();
    private static final PetstoreComponentHandler PETSTORE_COMPONENT_HANDLER = new PetstoreComponentHandler() {

        @Override
        public ModifiableConnectionDefinition modifyConnection(
            ModifiableConnectionDefinition modifiableConnectionDefinition) {

            modifiableConnectionDefinition.baseUri((connectionParameters, context) -> "http://localhost:9999");

            return modifiableConnectionDefinition;
        }

        @Override
        public ModifiableActionDefinition modifyAction(ModifiableActionDefinition modifiableActionDefinition) {
            return modifiableActionDefinition.perform(
                OpenApiComponentHandlerLoader.PERFORM_FUNCTION_FUNCTION.apply(modifiableActionDefinition));
        }
    };

    @Autowired
    private ConnectionRepository connectionRepository;

    private Connection connection;

    @Autowired
    private ActionDefinitionFacade actionDefinitionFacade;

    @BeforeEach
    public void beforeEach() {
        connectionRepository.deleteAll();

        connection = new Connection();

        connection.setAuthorizationName("api_key");
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

        Response response = (Response) openApiComponentTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.getStatusCode());

        //

        stubFor(delete("/store/order/1").willReturn(ok()));

        openApiComponentTaskHandler = createOpenApiComponentHandler("deleteOrder");

        taskExecution = getTaskExecution(Map.of("orderId", 1));

        response = (Response) openApiComponentTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.getStatusCode());

        //

        stubFor(delete("/user/user1").willReturn(ok()));

        openApiComponentTaskHandler = createOpenApiComponentHandler("deleteUser");

        taskExecution = getTaskExecution(Map.of("username", "user1"));

        response = (Response) openApiComponentTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.getStatusCode());
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

        Response response = (Response) openApiComponentTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.getStatusCode());
        JSONAssert.assertEquals(json, new JSONArray(response.getBody(new TypeReference<List<?>>() {})), true);

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

        response = (Response) openApiComponentTaskHandler.handle(taskExecution);

        Assertions.assertEquals(400, response.getStatusCode());
        Assertions.assertEquals(
            Map.of(
                "code", 400,
                "message", "Input error: query parameter `status value `unknown` is not in the allowable values " +
                    "`[available, pending, sold]`"),
            response.getBody(new TypeReference<Map<String, Object>>() {}));

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

        response = (Response) openApiComponentTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.getStatusCode());
        JSONAssert.assertEquals(json, new JSONArray(response.getBody(new TypeReference<List<?>>() {})), true);

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

        response = (Response) openApiComponentTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.getStatusCode());
        JSONAssert.assertEquals(json, new JSONObject(response.getBody(new TypeReference<Map<?, ?>>() {})), true);

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

        response = (Response) openApiComponentTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.getStatusCode());
        JSONAssert.assertEquals(json, new JSONObject(response.getBody(Map.class)), true);

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

        response = (Response) openApiComponentTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.getStatusCode());
        JSONAssert.assertEquals(json, new JSONObject(response.getBody(new TypeReference<Map<?, ?>>() {})), true);

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

        response = (Response) openApiComponentTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.getStatusCode());
        JSONAssert.assertEquals(json, new JSONObject(response.getBody(new TypeReference<Map<?, ?>>() {})), true);
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
            Map.of(
                "pet", new LinkedHashMap<String, Object>() {
                    {
                        put("id", 10);
                        put("name", "doggie");
                        put("category", Map.of("id", 1));
                        put("photoUrls", List.of("string"));
                        put("status", "available");
                        put("tags", List.of(Map.of("name", "tag1")));
                    }
                }));

        Response response = (Response) openApiComponentTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.getStatusCode());
        JSONAssert.assertEquals(json, new JSONObject(response.getBody(new TypeReference<Map<?, ?>>() {})), true);

        //

        stubFor(
            post(urlPathEqualTo("/pet/10"))
                .willReturn(
                    ok()
                        .withBody(json)
                        .withHeader("Content-Type", "application/json")));

        openApiComponentTaskHandler = createOpenApiComponentHandler("updatePetWithForm");

        taskExecution = getTaskExecution(Map.of("petId", 10, "name", "doggie", "status", "available"));

        response = (Response) openApiComponentTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertNull(response.getBody());

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

        FileEntry fileEntry = FILE_STORAGE_SERVICE.storeFileContent("data", "text.txt", "This is text");

        taskExecution = getTaskExecution(Map.of("petId", 10, "fileEntry", new FileEntryImpl(fileEntry)));

        response = (Response) openApiComponentTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.getStatusCode());
        JSONAssert.assertEquals(json, new JSONObject(response.getBody(new TypeReference<Map<?, ?>>() {})), true);

        //

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
            Map.of(
                "order", new LinkedHashMap<String, Object>() {
                    {
                        put("id", 10);
                        put("petId", 198772);
                        put("quantity", 7);
                        put("shipDate", LocalDateTime.of(2023, 1, 23, 7, 45));
                        put("status", "approved");
                        put("complete", true);
                    }
                }));

        response = (Response) openApiComponentTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.getStatusCode());
        JSONAssert.assertEquals(json, new JSONObject(response.getBody(new TypeReference<Map<?, ?>>() {})), true);

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
            Map.of(
                "user", new LinkedHashMap<String, Object>() {
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
                }));

        response = (Response) openApiComponentTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.getStatusCode());
        JSONAssert.assertEquals(json, new JSONObject(response.getBody(new TypeReference<Map<?, ?>>() {})), true);

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

        response = (Response) openApiComponentTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.getStatusCode());
        JSONAssert.assertEquals(json, new JSONArray(response.getBody(new TypeReference<List<?>>() {})), true);
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
            Map.of(
                "pet", new LinkedHashMap<String, Object>() {
                    {
                        put("id", 10);
                        put("name", "doggie");
                        put("category", Map.of("id", 1));
                        put("photoUrls", List.of("string"));
                        put("status", "available");
                        put("tags", List.of(Map.of("name", "tag1")));
                    }
                }));

        Response response = (Response) openApiComponentTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.getStatusCode());
        JSONAssert.assertEquals(json, new JSONObject(response.getBody(new TypeReference<Map<?, ?>>() {})), true);

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
            Map.of(
                "username", "user1",
                "user", new LinkedHashMap<String, Object>() {
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
                }));

        response = (Response) openApiComponentTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.getStatusCode());
        JSONAssert.assertEquals(json, new JSONObject(response.getBody(new TypeReference<Map<?, ?>>() {})), true);
    }

    private OpenApiComponentTaskHandler createOpenApiComponentHandler(String actionName) {
        return new OpenApiComponentTaskHandler(
            actionName, actionDefinitionFacade, PETSTORE_COMPONENT_HANDLER);
    }

    private TaskExecution getTaskExecution(Map<String, Object> parameters) {
        return TaskExecution.builder()
            .id(1L)
            .jobId(1L)
            .metadata(
                Map.of(
                    MetadataConstants.CONNECTION_IDS, Map.of("petstore", Validate.notNull(connection.getId(), "id")),
                    MetadataConstants.TYPE, Type.AUTOMATION))
            .workflowTask(
                new WorkflowTask(
                    Map.of(
                        WorkflowConstants.NAME, "name",
                        WorkflowConstants.TYPE, "type",
                        WorkflowConstants.PARAMETERS, parameters)))
            .build();
    }

    @ComponentScan(
        basePackages = {
            "com.bytechef.encryption", "com.bytechef.platform.component", "com.bytechef.platform.connection",
            "com.bytechef.liquibase.config"
        })
    @EnableAutoConfiguration
    @Configuration
    public static class OpenApiComponentTaskHandlerIntTestConfiguration {

        @MockBean
        DataStorageService dataStorageService;

        @MockBean
        InstanceAccessorRegistry instanceAccessorRegistry;

        @MockBean
        MessageBroker messageBroker;

        @MockBean
        OAuth2Service oAuth2Service;

        @MockBean
        TagService tagService;

        @MockBean
        WorkflowService workflowService;

        @MockBean
        WorkflowConnectionFacade workflowConnectionFacade;

        @Bean
        List<ComponentHandler> componentHandlers() {
            return List.of(PETSTORE_COMPONENT_HANDLER);
        }

        @Bean
        EncryptionKey encryptionKey() {
            return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
        }

        @Bean
        FileStorageService fileStorageService() {
            return FILE_STORAGE_SERVICE;
        }

        @MockBean
        WorkflowTestConfigurationService workflowTestConfigurationService;

        @Bean
        TaskFileStorage workflowFileStorage() {
            return new TaskFileStorageImpl(new Base64FileStorageService());
        }

        @EnableJdbcRepositories(basePackages = "com.bytechef.platform.connection.repository")
        public static class ConnectionIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

            private final Encryption encryption;
            private final ObjectMapper objectMapper;

            @SuppressFBWarnings("EI2")
            public ConnectionIntTestJdbcConfiguration(
                Encryption encryption, @Qualifier("objectMapper") ObjectMapper objectMapper) {

                this.encryption = encryption;
                this.objectMapper = objectMapper;
            }

            @Override
            protected List<?> userConverters() {
                return Arrays.asList(
                    new EncryptedMapWrapperToStringConverter(encryption, objectMapper),
                    new EncryptedStringToMapWrapperConverter(encryption, objectMapper));
            }
        }
    }
}
