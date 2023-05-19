
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

package com.bytechef.hermes.component.oas.task.handler;

import com.bytechef.atlas.constant.WorkflowConstants;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.commons.data.jdbc.converter.EncryptedMapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.EncryptedStringToMapWrapperConverter;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.petstore.PetstoreComponentHandler;
import com.bytechef.encryption.Encryption;
import com.bytechef.encryption.EncryptionKey;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableConnectionDefinition;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.definition.registry.component.ComponentDefinitionRegistryImpl;
import com.bytechef.hermes.component.oas.handler.OpenApiComponentActionTaskHandler;
import com.bytechef.hermes.component.util.HttpClientUtils;
import com.bytechef.hermes.component.util.HttpClientUtils.Response;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.repository.ConnectionRepository;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.constant.MetadataConstants;
import com.bytechef.hermes.data.storage.service.DataStorageService;
import com.bytechef.hermes.definition.registry.component.factory.ContextFactory;
import com.bytechef.hermes.definition.registry.component.factory.ContextFactoryImpl;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionServiceImpl;
import com.bytechef.hermes.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.hermes.file.storage.domain.FileEntry;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.test.annotation.EmbeddedSql;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

/**
 * @author Ivica Cardic
 */
@EmbeddedSql
@SpringBootTest
@WireMockTest(httpPort = 9999)
public class OpenApiComponentActionTaskHandlerIntTest {

    private static final FileStorageService FILE_STORAGE_SERVICE = new Base64FileStorageService();
    private static final PetstoreComponentHandler PETSTORE_COMPONENT_HANDLER = new PetstoreComponentHandler() {

        @Override
        public ModifiableConnectionDefinition modifyConnection(ModifiableConnectionDefinition connectionDefinition) {
            connectionDefinition.baseUri(connection -> "http://localhost:9999");

            return connectionDefinition;
        }
    };

    @Autowired
    private ConnectionRepository connectionRepository;

    private Connection connection;

    @Autowired
    private ContextFactory contextFactory;

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

        OpenApiComponentActionTaskHandler openApiComponentActionTaskHandler = createOpenApiComponentHandler(
            "deletePet");

        TaskExecution taskExecution = getTaskExecution(Map.of("petId", 1));

        HttpClientUtils.Response response = (Response) openApiComponentActionTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.statusCode());

        //

        stubFor(delete("/store/order/1").willReturn(ok()));

        openApiComponentActionTaskHandler = createOpenApiComponentHandler("deleteOrder");

        taskExecution = getTaskExecution(Map.of("orderId", 1));

        response = (Response) openApiComponentActionTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.statusCode());

        //

        stubFor(delete("/user/user1").willReturn(ok()));

        openApiComponentActionTaskHandler = createOpenApiComponentHandler("deleteUser");

        taskExecution = getTaskExecution(Map.of("username", "user1"));

        response = (Response) openApiComponentActionTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.statusCode());
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

        OpenApiComponentActionTaskHandler openApiComponentActionTaskHandler = createOpenApiComponentHandler(
            "findPetsByStatus");

        TaskExecution taskExecution = getTaskExecution(Map.of("status", "available"));

        Response response = (Response) openApiComponentActionTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.statusCode());
        JSONAssert.assertEquals(json, new JSONArray((List<?>) response.body()), true);

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

        openApiComponentActionTaskHandler = createOpenApiComponentHandler("findPetsByStatus");

        taskExecution = getTaskExecution(Map.of("status", "unknown"));

        response = (Response) openApiComponentActionTaskHandler.handle(taskExecution);

        Assertions.assertEquals(400, response.statusCode());
        Assertions.assertEquals(
            Map.of(
                "code", 400,
                "message", "Input error: query parameter `status value `unknown` is not in the allowable values " +
                    "`[available, pending, sold]`"),
            response.body());

        //

        stubFor(
            get(urlPathEqualTo("/pet/findByTags"))
                .withQueryParam("tags", equalTo("tag1"))
                .willReturn(
                    ok()
                        .withBody(json)
                        .withHeader("Content-Type", "application/json")));

        openApiComponentActionTaskHandler = createOpenApiComponentHandler("findPetsByTags");

        taskExecution = getTaskExecution(Map.of("tags", List.of("tag1")));

        response = (Response) openApiComponentActionTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.statusCode());
        JSONAssert.assertEquals(json, new JSONArray((List<?>) response.body()), true);

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

        openApiComponentActionTaskHandler = createOpenApiComponentHandler("getPetById");

        taskExecution = getTaskExecution(Map.of("petId", 7));

        response = (Response) openApiComponentActionTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.statusCode());
        JSONAssert.assertEquals(json, new JSONObject((Map<?, ?>) response.body()), true);

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

        openApiComponentActionTaskHandler = createOpenApiComponentHandler("getInventory");

        taskExecution = getTaskExecution(Collections.emptyMap());

        response = (Response) openApiComponentActionTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.statusCode());
        JSONAssert.assertEquals(json, new JSONObject((Map<?, ?>) response.body()), true);

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

        openApiComponentActionTaskHandler = createOpenApiComponentHandler("getOrderById");

        taskExecution = getTaskExecution(Map.of("orderId", 3));

        response = (Response) openApiComponentActionTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.statusCode());
        JSONAssert.assertEquals(json, new JSONObject((Map<?, ?>) response.body()), true);

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

        openApiComponentActionTaskHandler = createOpenApiComponentHandler("getUserByName");

        taskExecution = getTaskExecution(Map.of("username", "user1"));

        response = (Response) openApiComponentActionTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.statusCode());
        JSONAssert.assertEquals(json, new JSONObject((Map<?, ?>) response.body()), true);
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

        OpenApiComponentActionTaskHandler openApiComponentActionTaskHandler = createOpenApiComponentHandler("addPet");

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

        Response response = (Response) openApiComponentActionTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.statusCode());
        JSONAssert.assertEquals(json, new JSONObject((Map<?, ?>) response.body()), true);

        //

        stubFor(
            post(urlPathEqualTo("/pet/10"))
                .willReturn(
                    ok()
                        .withBody(json)
                        .withHeader("Content-Type", "application/json")));

        openApiComponentActionTaskHandler = createOpenApiComponentHandler("updatePetWithForm");

        taskExecution = getTaskExecution(Map.of("petId", 10, "name", "doggie", "status", "available"));

        response = (Response) openApiComponentActionTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertNull(response.body());

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

        openApiComponentActionTaskHandler = createOpenApiComponentHandler("uploadFile");

        FileEntry fileEntry = FILE_STORAGE_SERVICE.storeFileContent("text.txt", "This is text");

        taskExecution = getTaskExecution(Map.of("petId", 10, "fileEntry", new MockContextFileEntry(fileEntry)));

        response = (Response) openApiComponentActionTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.statusCode());
        JSONAssert.assertEquals(json, new JSONObject((Map<?, ?>) response.body()), true);

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

        openApiComponentActionTaskHandler = createOpenApiComponentHandler("placeOrder");

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

        response = (Response) openApiComponentActionTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.statusCode());
        JSONAssert.assertEquals(json, new JSONObject((Map<?, ?>) response.body()), true);

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

        openApiComponentActionTaskHandler = createOpenApiComponentHandler("createUser");

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

        response = (Response) openApiComponentActionTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.statusCode());
        JSONAssert.assertEquals(json, new JSONObject((Map<?, ?>) response.body()), true);

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

        openApiComponentActionTaskHandler = createOpenApiComponentHandler("createUsersWithListInput");

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

        response = (Response) openApiComponentActionTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.statusCode());
        JSONAssert.assertEquals(json, new JSONArray((List<?>) response.body()), true);
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

        OpenApiComponentActionTaskHandler openApiComponentActionTaskHandler = createOpenApiComponentHandler(
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

        HttpClientUtils.Response response = (Response) openApiComponentActionTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.statusCode());
        JSONAssert.assertEquals(json, new JSONObject((Map<?, ?>) response.body()), true);

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

        openApiComponentActionTaskHandler = createOpenApiComponentHandler("updateUser");

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

        response = (HttpClientUtils.Response) openApiComponentActionTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.statusCode());
        JSONAssert.assertEquals(json, new JSONObject((Map<?, ?>) response.body()), true);
    }

    private OpenApiComponentActionTaskHandler createOpenApiComponentHandler(String actionName) {
        return new OpenApiComponentActionTaskHandler(
            getActionDefinition(actionName), contextFactory, PETSTORE_COMPONENT_HANDLER);
    }

    private ActionDefinition getActionDefinition(String actionName) {
        ComponentDefinition componentDefinition = PETSTORE_COMPONENT_HANDLER.getDefinition();

        for (ActionDefinition actionDefinition : OptionalUtils.get(componentDefinition.getActions())) {
            if (Objects.equals(actionDefinition.getName(), actionName)) {
                return actionDefinition;
            }
        }

        throw new IllegalArgumentException("Action name does not exist");
    }

    private TaskExecution getTaskExecution(Map<String, Object> parameters) {
        return TaskExecution.builder()
            .metadata(
                connection.getId() == null
                    ? Map.of()
                    : Map.of(MetadataConstants.CONNECTION_IDS, Map.of("petstore", connection.getId())))
            .workflowTask(
                WorkflowTask.of(Map.of(WorkflowConstants.TYPE, "type", WorkflowConstants.PARAMETERS, parameters)))
            .build();
    }

    @ComponentScan(
        basePackages = {
            "com.bytechef.hermes.component.util",
            "com.bytechef.hermes.connection"
        })
    @EnableAutoConfiguration
    @Configuration
    public static class OpenApiComponentTaskHandlerIntTestConfiguration {

        @MockBean
        DataStorageService dataStorageService;

        @MockBean
        MessageBroker messageBroker;

        @Bean
        ConnectionDefinitionService connectionDefinitionService() {
            return new ConnectionDefinitionServiceImpl(
                new ComponentDefinitionRegistryImpl(List.of(PETSTORE_COMPONENT_HANDLER)));
        }

        @Bean
        ContextFactory contextFactory(
            ConnectionDefinitionService connectionDefinitionService, ConnectionService connectionService,
            DataStorageService dataStorageService) {

            return new ContextFactoryImpl(
                connectionDefinitionService, connectionService, dataStorageService, e -> {}, FILE_STORAGE_SERVICE);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper() {
                {
                    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

                    registerModule(new JavaTimeModule());
                    registerModule(new Jdk8Module());
                }
            };
        }

        @EnableCaching
        @TestConfiguration
        public static class CacheConfiguration {
        }

        @TestConfiguration
        public static class EncryptionIntTestConfiguration {

            @Bean
            Encryption encryption(EncryptionKey encryptionKey) {
                return new Encryption(encryptionKey);
            }

            @Bean
            EncryptionKey encryptionKey() {
                return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
            }
        }

        @EnableJdbcRepositories(basePackages = "com.bytechef.hermes.connection.repository")
        public static class ConnectionIntTestJdbcConfiguration
            extends AbstractIntTestJdbcConfiguration {

            private final Encryption encryption;
            private final ObjectMapper objectMapper;

            @SuppressFBWarnings("EI2")
            public ConnectionIntTestJdbcConfiguration(Encryption encryption, ObjectMapper objectMapper) {
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

    private static class MockContextFileEntry implements Context.FileEntry {

        private final FileEntry fileEntry;

        public MockContextFileEntry(FileEntry fileEntry) {
            this.fileEntry = fileEntry;
        }

        @Override
        public String getExtension() {
            return fileEntry.getExtension();
        }

        @Override
        public String getMimeType() {
            return fileEntry.getMimeType();
        }

        @Override
        public String getName() {
            return fileEntry.getName();
        }

        @Override
        public String getUrl() {
            return fileEntry.getUrl();
        }
    }
}
