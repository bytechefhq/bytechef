
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

package com.bytechef.hermes.component.registrar;

import com.bytechef.atlas.constant.WorkflowConstants;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.commons.data.jdbc.converter.EncryptedMapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.EncryptedStringToMapWrapperConverter;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.component.petstore.PetstoreComponentHandler;
import com.bytechef.encryption.Encryption;
import com.bytechef.encryption.EncryptionKey;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableConnectionDefinition;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import com.bytechef.hermes.component.task.handler.OpenApiComponentTaskHandler;
import com.bytechef.hermes.component.util.HttpClientUtils;
import com.bytechef.hermes.component.util.HttpClientUtils.Response;
import com.bytechef.hermes.connection.constant.ConnectionConstants;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.repository.ConnectionRepository;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.definition.registry.config.WorkerDefinitionRegistryConfiguration;
import com.bytechef.hermes.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.hermes.file.storage.domain.FileEntry;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import com.bytechef.tag.service.TagService;
import com.bytechef.test.annotation.EmbeddedSql;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
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
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
@SpringBootTest(properties = {
    "spring.application.name=server-app"
})
@WireMockTest(httpPort = 9999)
public class OpenApiComponentTaskHandlerIntTest {

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

    @Autowired
    private ConnectionService connectionService;

    @MockBean
    private TagService tagService;

    private Connection connection;

    @BeforeEach
    public void beforeEach() {
        connectionRepository.deleteAll();

        connection = new Connection();

        connection.setComponentName("petshop");
        connection.setConnectionVersion(1);
        connection.setName("PetShop Connection");

        connection = connectionRepository.save(connection);
    }

    @Test
    public void testHandleDELETE() throws Exception {
        stubFor(delete("/pet/1").willReturn(ok()));

        OpenApiComponentTaskHandler openApiComponentTaskHandler = createRestComponentHandler("deletePet");

        TaskExecution taskExecution = getTaskExecution(Map.of("petId", 1));

        HttpClientUtils.Response response = (Response) openApiComponentTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.statusCode());

        //

        stubFor(delete("/store/order/1").willReturn(ok()));

        openApiComponentTaskHandler = createRestComponentHandler("deleteOrder");

        taskExecution = getTaskExecution(Map.of("orderId", 1));

        response = (Response) openApiComponentTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.statusCode());

        //

        stubFor(delete("/user/user1").willReturn(ok()));

        openApiComponentTaskHandler = createRestComponentHandler("deleteUser");

        taskExecution = getTaskExecution(Map.of("username", "user1"));

        response = (Response) openApiComponentTaskHandler.handle(taskExecution);

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

        OpenApiComponentTaskHandler openApiComponentTaskHandler = createRestComponentHandler("findPetsByStatus");

        TaskExecution taskExecution = getTaskExecution(Map.of("status", "available"));

        Response response = (Response) openApiComponentTaskHandler.handle(taskExecution);

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

        openApiComponentTaskHandler = createRestComponentHandler("findPetsByStatus");

        taskExecution = getTaskExecution(Map.of("status", "unknown"));

        response = (Response) openApiComponentTaskHandler.handle(taskExecution);

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

        openApiComponentTaskHandler = createRestComponentHandler("findPetsByTags");

        taskExecution = getTaskExecution(Map.of("tags", List.of("tag1")));

        response = (Response) openApiComponentTaskHandler.handle(taskExecution);

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

        openApiComponentTaskHandler = createRestComponentHandler("getPetById");

        taskExecution = getTaskExecution(Map.of("petId", 7));

        response = (Response) openApiComponentTaskHandler.handle(taskExecution);

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

        openApiComponentTaskHandler = createRestComponentHandler("getInventory");

        taskExecution = getTaskExecution(Collections.emptyMap());

        response = (Response) openApiComponentTaskHandler.handle(taskExecution);

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

        openApiComponentTaskHandler = createRestComponentHandler("getOrderById");

        taskExecution = getTaskExecution(Map.of("orderId", 3));

        response = (Response) openApiComponentTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.statusCode());
        JSONAssert.assertEquals(json, new JSONObject((Map<?, ?>) response.body()), true);

        //

        stubFor(
            get(
                urlPathEqualTo("/user/login"))
                    .willReturn(
                        ok()
                            .withBody("Logged in user session: 7284289668658063360")
                            .withHeader("Content-Type", "text/plain")));

        openApiComponentTaskHandler = createRestComponentHandler("loginUser");

        taskExecution = getTaskExecution(Map.of());

        response = (Response) openApiComponentTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("Logged in user session: 7284289668658063360", response.body());

        //

        stubFor(get(urlPathEqualTo("/user/logout")).willReturn(ok()));

        openApiComponentTaskHandler = createRestComponentHandler("logoutUser");

        taskExecution = getTaskExecution(Map.of());

        response = (Response) openApiComponentTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertNull(response.body());

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

        openApiComponentTaskHandler = createRestComponentHandler("getUserByName");

        taskExecution = getTaskExecution(Map.of("username", "user1"));

        response = (Response) openApiComponentTaskHandler.handle(taskExecution);

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

        OpenApiComponentTaskHandler openApiComponentTaskHandler = createRestComponentHandler("addPet");

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

        Assertions.assertEquals(200, response.statusCode());
        JSONAssert.assertEquals(json, new JSONObject((Map<?, ?>) response.body()), true);

        //

//        restComponentTaskHandler = new RestComponentTaskHandler(
//            getActionDefinition("updatePetWithForm"), getConnectionDefinition(), connectionService,
//            PETSTORE_COMPONENT_HANDLER, null, null);
//
//        taskExecution = getTaskExecution(Map.of("petId", 10, "name", "doggie", "status", "available"));
//
//        httpResponseEntry = (HttpResponseEntry) restComponentTaskHandler.handle(taskExecution);
//
//        Assertions.assertEquals(200, httpResponseEntry.statusCode());
//        Assertions.assertNull(httpResponseEntry.body());

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

        openApiComponentTaskHandler = createRestComponentHandler("uploadFile");

        FileEntry fileEntry = FILE_STORAGE_SERVICE.storeFileContent("text.txt", "This is text");

        taskExecution = getTaskExecution(Map.of("petId", 10, "fileEntry", fileEntry.toComponentFileEntry()));

        response = (Response) openApiComponentTaskHandler.handle(taskExecution);

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

        openApiComponentTaskHandler = createRestComponentHandler("placeOrder");

        taskExecution = getTaskExecution(
            Map.of(
                "order", new LinkedHashMap<String, Object>() {
                    {
                        put("id", 10);
                        put("petId", 198772);
                        put("quantity", 7);
                        put("shipDate", LocalDateTime.of(2023, 01, 23, 7, 45));
                        put("status", "approved");
                        put("complete", true);
                    }
                }));

        response = (Response) openApiComponentTaskHandler.handle(taskExecution);

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

        openApiComponentTaskHandler = createRestComponentHandler("createUser");

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

        openApiComponentTaskHandler = createRestComponentHandler("createUsersWithListInput");

        taskExecution = getTaskExecution(
            Map.of(
                "array", List.of(
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

        OpenApiComponentTaskHandler openApiComponentTaskHandler = createRestComponentHandler("updatePet");

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

        HttpClientUtils.Response response = (Response) openApiComponentTaskHandler.handle(taskExecution);

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

        openApiComponentTaskHandler = createRestComponentHandler("updateUser");

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

        response = (HttpClientUtils.Response) openApiComponentTaskHandler.handle(taskExecution);

        Assertions.assertEquals(200, response.statusCode());
        JSONAssert.assertEquals(json, new JSONObject((Map<?, ?>) response.body()), true);
    }

    private OpenApiComponentTaskHandler createRestComponentHandler(String actionName) {
        return new OpenApiComponentTaskHandler(
            getActionDefinition(actionName), getConnectionDefinition(), connectionService,
            PETSTORE_COMPONENT_HANDLER, null, FILE_STORAGE_SERVICE);
    }

    private ConnectionDefinition getConnectionDefinition() {
        ComponentDefinition componentDefinition = PETSTORE_COMPONENT_HANDLER.getDefinition();

        return componentDefinition.getConnection();
    }

    private ActionDefinition getActionDefinition(String actionName) {
        ComponentDefinition componentDefinition = PETSTORE_COMPONENT_HANDLER.getDefinition();

        for (ActionDefinition actionDefinition : componentDefinition.getActions()) {
            if (Objects.equals(actionDefinition.getName(), actionName)) {
                return actionDefinition;
            }
        }

        throw new IllegalArgumentException("Action name does not exist");
    }

    private TaskExecution getTaskExecution(Map<String, Object> parameters) {
        return new TaskExecution(
            connection.getId() == null
                ? new WorkflowTask()
                : new WorkflowTask(
                    Map.of(
                        WorkflowConstants.PARAMETERS,
                        Stream
                            .concat(
                                CollectionUtils.stream(Map.of(ConnectionConstants.CONNECTION_ID, connection.getId())),
                                CollectionUtils.stream(parameters.entrySet()))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))));
    }

    @ComponentScan(
        basePackages = {
            "com.bytechef.hermes.connection"
        })
    @EnableAutoConfiguration
    @Import(WorkerDefinitionRegistryConfiguration.class)
    @Configuration
    public static class OpenApiComponentTaskHandlerIntTestConfiguration {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
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
}
