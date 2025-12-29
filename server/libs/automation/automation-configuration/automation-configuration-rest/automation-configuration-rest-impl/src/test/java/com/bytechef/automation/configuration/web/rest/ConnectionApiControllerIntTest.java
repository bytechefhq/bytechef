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

package com.bytechef.automation.configuration.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.web.rest.config.AutomationConfigurationRestConfigurationSharedMocks;
import com.bytechef.automation.configuration.web.rest.mapper.WorkspaceConnectionMapper;
import com.bytechef.automation.configuration.web.rest.model.ConnectionModel;
import com.bytechef.automation.configuration.web.rest.model.TagModel;
import com.bytechef.automation.configuration.web.rest.model.UpdateTagsRequestModel;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tag.domain.Tag;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;

/**
 * @author Ivica Cardic
 */
@Disabled
@WebMvcTest(value = ConnectionApiController.class)
@AutomationConfigurationRestConfigurationSharedMocks
public class ConnectionApiControllerIntTest {

    @MockitoBean
    private ConnectionFacade connectionFacade;

    @MockitoBean
    private ConnectionService connectionService;

    @Autowired
    private WorkspaceConnectionMapper workspaceConnectionMapper;

    @Autowired
    private MockMvc mockMvc;

    private WebTestClient webTestClient;

    @BeforeEach
    public void beforeEach() {
        this.webTestClient = MockMvcWebTestClient
            .bindTo(mockMvc)
            .build();
    }

    @Test
    public void testDeleteConnection() {
        try {
            this.webTestClient
                .delete()
                .uri("/internal/connections/1")
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
    public void testGetConnection() {
        try {
            ConnectionDTO connectionDTO = getConnection();

            when(connectionFacade.getConnection(1L))
                .thenReturn(connectionDTO);

            this.webTestClient
                .get()
                .uri("/internal/connections/1")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ConnectionModel.class)
                .isEqualTo(Validate.notNull(workspaceConnectionMapper.convert(connectionDTO), "connectionModel")
                    .parameters(null));
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void testGetConnectionTags() {
        when(connectionFacade.getConnectionTags(PlatformType.AUTOMATION))
            .thenReturn(List.of(new Tag(1L, "tag1"), new Tag(2L, "tag2")));

        try {
            this.webTestClient
                .get()
                .uri("/internal/connections/tags")
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
    public void testGetConnections() {
        ConnectionDTO connectionDTO = getConnection();

        when(connectionFacade.getConnections(null, null, List.of(), null, null, PlatformType.AUTOMATION))
            .thenReturn(List.of(connectionDTO));

        this.webTestClient
            .get()
            .uri("/internal/connections")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(ConnectionModel.class)
            .contains(Validate.notNull(workspaceConnectionMapper.convert(connectionDTO), "connectionMapper")
                .parameters(null))
            .hasSize(1);

        when(connectionFacade.getConnections("component1", null, List.of(), null, null, PlatformType.AUTOMATION))
            .thenReturn(List.of(connectionDTO));

        this.webTestClient
            .get()
            .uri("/internal/connections?componentNames=component1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(ConnectionModel.class)
            .hasSize(1);

        when(connectionFacade.getConnections(null, 1, List.of(), null, null, PlatformType.AUTOMATION))
            .thenReturn(List.of(connectionDTO));

        this.webTestClient
            .get()
            .uri("/internal/connections?tagIds=1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(ConnectionModel.class)
            .hasSize(1);

        when(connectionFacade.getConnections("component1", 1, List.of(), null, null, PlatformType.AUTOMATION))
            .thenReturn(List.of(connectionDTO));

        this.webTestClient
            .get()
            .uri("/internal/connections?componentNames=component1&tagIds=1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }

    @Test
    public void testPostConnection() {
        ConnectionDTO connectionDTO = getConnection();
        ConnectionModel connectionModel = new ConnectionModel().componentName("componentName")
            .name("name")
            .parameters(Map.of("key1", "value1"));

        when(connectionFacade.create(any(), PlatformType.AUTOMATION))
            .thenReturn(getConnection().id());

        try {
            assert connectionDTO.id() != null;
            this.webTestClient
                .post()
                .uri("/internal/connections")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(connectionModel)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody();
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        ArgumentCaptor<ConnectionDTO> connectionArgumentCaptor = ArgumentCaptor.forClass(ConnectionDTO.class);

        verify(connectionFacade).create(connectionArgumentCaptor.capture(), PlatformType.AUTOMATION);

        assertThat(connectionArgumentCaptor.getValue())
            .hasFieldOrPropertyWithValue("componentName", "componentName")
            .hasFieldOrPropertyWithValue("name", "name")
            .hasFieldOrPropertyWithValue("parameters", Map.of("key1", "value1"));
    }

    @Test
    public void testPutConnection() {
        ConnectionModel connectionModel = new ConnectionModel().name("name2");

        try {
            this.webTestClient
                .put()
                .uri("/internal/connections/1")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(connectionModel)
                .exchange()
                .expectStatus()
                .isNoContent();
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
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

        ArgumentCaptor<List<Long>> tagsArgumentCaptor = ArgumentCaptor.forClass(List.class);

        verify(connectionService).update(anyLong(), tagsArgumentCaptor.capture());

        List<Long> capturedTagIds = tagsArgumentCaptor.getValue();

        Iterator<Long> tagIterator = capturedTagIds.iterator();

        Long capturedTagId = tagIterator.next();

        Assertions.assertEquals(2, capturedTagId);
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

    @ComponentScan(basePackages = "com.bytechef.automation.configuration.web.rest")
    @Configuration
    public static class ConnectionRestTestConfiguration {
    }
}
