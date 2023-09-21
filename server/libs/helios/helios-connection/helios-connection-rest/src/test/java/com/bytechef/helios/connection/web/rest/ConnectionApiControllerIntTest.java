
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

package com.bytechef.helios.connection.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.helios.connection.web.rest.mapper.ConnectionMapper;
import com.bytechef.helios.connection.web.rest.model.ConnectionModel;
import com.bytechef.helios.connection.web.rest.model.TagModel;
import com.bytechef.helios.connection.web.rest.model.UpdateTagsRequestModel;
import com.bytechef.helios.connection.dto.ConnectionDTO;
import com.bytechef.helios.connection.facade.ConnectionFacade;
import com.bytechef.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;

/**
 * @author Ivica Cardic
 */
@Disabled
@WebMvcTest(value = ConnectionApiController.class)
public class ConnectionApiControllerIntTest {

    @MockBean
    private ConnectionFacade connectionFacade;

    @Autowired
    private ConnectionMapper connectionMapper;

    @Autowired
    private MockMvc mockMvc;

    private WebTestClient webTestClient;

    @BeforeEach
    public void setup() {
        this.webTestClient = MockMvcWebTestClient
            .bindTo(mockMvc)
            .build();
    }

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

        ArgumentCaptor<Long> argument = ArgumentCaptor.forClass(Long.class);

        verify(connectionFacade).delete(argument.capture());

        Assertions.assertEquals(1L, argument.getValue());
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testGetConnection() {
        try {
            ConnectionDTO connectionDTO = getConnection();

            when(connectionFacade.getConnection(1L)).thenReturn(connectionDTO);

            this.webTestClient
                .get()
                .uri("/connections/1")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ConnectionModel.class)
                .isEqualTo(connectionMapper.convert(connectionDTO)
                    .parameters(null));
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void testGetConnectionTags() {
        when(connectionFacade.getConnectionTags()).thenReturn(List.of(new Tag(1L, "tag1"), new Tag(2L, "tag2")));

        try {
            this.webTestClient
                .get()
                .uri("/connections/tags")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.[0].id")
                .isEqualTo(1)
                .jsonPath("$.[1].id")
                .isEqualTo(2)
                .jsonPath("$.[0].name")
                .isEqualTo("tag1")
                .jsonPath("$.[1].name")
                .isEqualTo("tag2");
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testGetConnections() {
        ConnectionDTO connectionDTO = getConnection();

        when(connectionFacade.getConnections((String) null, null)).thenReturn(List.of(connectionDTO));

        this.webTestClient
            .get()
            .uri("/connections")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(ConnectionModel.class)
            .contains(connectionMapper.convert(connectionDTO)
                .parameters(null))
            .hasSize(1);

        when(connectionFacade.getConnections("component1", null)).thenReturn(List.of(connectionDTO));

        this.webTestClient
            .get()
            .uri("/connections?componentNames=component1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(ConnectionModel.class)
            .hasSize(1);

        when(connectionFacade.getConnections(null, 1)).thenReturn(List.of(connectionDTO));

        this.webTestClient
            .get()
            .uri("/connections?tagIds=1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(ConnectionModel.class)
            .hasSize(1);

        when(connectionFacade.getConnections("component1", 1)).thenReturn(List.of(connectionDTO));

        this.webTestClient
            .get()
            .uri("/connections?componentNames=component1&tagIds=1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testPostConnection() {
        ConnectionDTO connectionDTO = getConnection();
        ConnectionModel connectionModel = new ConnectionModel().componentName("componentName")
            .name("name")
            .parameters(Map.of("key1", "value1"));

        when(connectionFacade.create(any())).thenReturn(getConnection());

        try {
            assert connectionDTO.id() != null;
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
                .isEqualTo(connectionDTO.id())
                .jsonPath("$.name")
                .isEqualTo(connectionDTO.name())
                .jsonPath("$.parameters")
                .isMap()
                .jsonPath("$.parameters.key1")
                .isEqualTo("value1");
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        ArgumentCaptor<ConnectionDTO> connectionArgumentCaptor = ArgumentCaptor.forClass(ConnectionDTO.class);

        verify(connectionFacade).create(connectionArgumentCaptor.capture());

        org.assertj.core.api.Assertions.assertThat(connectionArgumentCaptor.getValue())
            .hasFieldOrPropertyWithValue("componentName", "componentName")
            .hasFieldOrPropertyWithValue("name", "name")
            .hasFieldOrPropertyWithValue("parameters", Map.of("key1", "value1"));
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testPutConnection() {
        ConnectionDTO connection = ConnectionDTO.builder()
            .componentName("componentName")
            .id(1L)
            .name("name2")
            .parameters(Map.of("key1", "value1"))
            .version(1)
            .build();
        ConnectionModel connectionModel = new ConnectionModel().name("name2");

        when(connectionFacade.update(any(ConnectionDTO.class))).thenReturn(connection);

        try {
            this.webTestClient
                .put()
                .uri("/connections/1")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(connectionModel)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.id")
                .isEqualTo(connection.id())
                .jsonPath("$.name")
                .isEqualTo("name2");
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    @SuppressFBWarnings("NP")
    public void testPutConnectionTags() {
        try {
            this.webTestClient
                .put()
                .uri("/project-connections/1/tags")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateTagsRequestModel().tags(List.of(new TagModel().name("tag1"))))
                .exchange()
                .expectStatus()
                .is2xxSuccessful();
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        ArgumentCaptor<List<Tag>> tagsArgumentCaptor = ArgumentCaptor.forClass(List.class);

        verify(connectionFacade).update(anyLong(), tagsArgumentCaptor.capture());

        List<Tag> capturedTags = tagsArgumentCaptor.getValue();

        Iterator<Tag> tagIterator = capturedTags.iterator();

        Tag capturedTag = tagIterator.next();

        Assertions.assertEquals("tag1", capturedTag.getName());
    }

    private static ConnectionDTO getConnection() {
        return ConnectionDTO.builder()
            .componentName("componentName")
            .id(1L)
            .name("name")
            .parameters(Map.of("key1", "value1"))
            .version(1)
            .build();
    }

    @ComponentScan(basePackages = "com.bytechef.helios.connection.web.rest")
    @Configuration
    public static class ConnectionRestTestConfiguration {
    }
}
