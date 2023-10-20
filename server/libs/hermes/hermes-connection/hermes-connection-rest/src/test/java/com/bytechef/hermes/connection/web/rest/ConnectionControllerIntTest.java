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

package com.bytechef.hermes.connection.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.encryption.EncryptionKey;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.connection.web.rest.config.ConnectionRestTestConfiguration;
import com.bytechef.hermes.connection.web.rest.model.ConnectionModel;
import com.bytechef.hermes.connection.web.rest.model.ConnectionUpdateModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = ConnectionRestTestConfiguration.class)
@WebFluxTest(value = ConnectionController.class)
public class ConnectionControllerIntTest {

    @MockBean
    private ConnectionService connectionService;

    @MockBean
    private EncryptionKey encryptionKey;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void testDeleteConnection() {
        try {
            this.webTestClient
                    .delete()
                    .uri("/connections/1")
                    .exchange()
                    .expectStatus()
                    .isOk();
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);

        verify(connectionService).delete(argument.capture());

        Assertions.assertEquals("1", argument.getValue());
    }

    @Test
    public void testGetConnection() {
        try {
            Connection connection = getConnection();

            when(connectionService.getConnection(anyString())).thenReturn(connection);

            this.webTestClient
                    .get()
                    .uri("/connections/1")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody(ConnectionModel.class);
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void testGetConnections() {
        Connection connection = getConnection();

        when(connectionService.getConnections()).thenReturn(List.of(connection));

        try {
            this.webTestClient
                    .get()
                    .uri("/connections")
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBodyList(ConnectionModel.class);
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testPostConnection() {
        Connection connection = getConnection();
        ConnectionModel connectionModel =
                new ConnectionModel().name("name").label("label").parameters(Map.of("key1", "value1"));

        when(connectionService.add(any())).thenReturn(connection);

        try {
            assert connection.getId() != null;
            this.webTestClient
                    .post()
                    .uri("/connections")
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(connectionModel)
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody()
                    .jsonPath("$.id")
                    .isEqualTo(connection.getId())
                    .jsonPath("$.label")
                    .isEqualTo(connection.getLabel())
                    .jsonPath("$.name")
                    .isEqualTo(connection.getName())
                    .jsonPath("$.parameters")
                    .isMap()
                    .jsonPath("$.parameters.key1")
                    .isEqualTo("value1");
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testPutConnection() {
        Connection connection = getConnection();
        ConnectionUpdateModel connectionUpdateModel = new ConnectionUpdateModel().name("name2");

        connection.setLabel("label2");

        when(connectionService.update(anyString(), anyString())).thenReturn(connection);

        try {
            this.webTestClient
                    .put()
                    .uri("/connections/1")
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(connectionUpdateModel)
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody()
                    .jsonPath("$.id")
                    .isEqualTo(connection.getId())
                    .jsonPath("$.label")
                    .isEqualTo("label2");
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    private static Connection getConnection() {
        Connection connection = new Connection();

        connection.setId("1");
        connection.setLabel("label");
        connection.setName("name");
        connection.setParameters(Map.of("key1", "value1"));

        return connection;
    }
}
