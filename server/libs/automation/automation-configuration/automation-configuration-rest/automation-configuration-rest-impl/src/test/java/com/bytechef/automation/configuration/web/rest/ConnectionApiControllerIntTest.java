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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.facade.ProjectCategoryFacade;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectTagFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.web.rest.config.AutomationConfigurationRestConfigurationSharedMocks;
import com.bytechef.automation.configuration.web.rest.config.AutomationConfigurationRestTestConfiguration;
import com.bytechef.automation.configuration.web.rest.model.ConnectionModel;
import com.bytechef.automation.configuration.web.rest.model.UpdateConnectionRequestModel;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = AutomationConfigurationRestTestConfiguration.class)
@WebMvcTest(value = ConnectionApiController.class)
@AutomationConfigurationRestConfigurationSharedMocks
@MockitoBean(types = {
    CategoryService.class, ProjectCategoryFacade.class, ProjectDeploymentFacade.class, ProjectFacade.class,
    ProjectService.class, ProjectTagFacade.class, ProjectWorkflowFacade.class, WorkflowService.class,
    WorkspaceFacade.class
})
class ConnectionApiControllerIntTest {

    @Autowired
    private ConnectionFacade connectionFacade;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkspaceConnectionFacade workspaceConnectionFacade;

    private WebTestClient webTestClient;

    @BeforeEach
    void beforeEach() {
        webTestClient = MockMvcWebTestClient.bindTo(mockMvc)
            .build();
    }

    @Test
    void testCreateConnection() {
        when(workspaceConnectionFacade.create(eq(5L), any())).thenReturn(1L);

        webTestClient.post()
            .uri("/internal/connections")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                new ConnectionModel().componentName("componentName")
                    .name("name")
                    .parameters(Map.of("key1", "value1"))
                    .workspaceId(5L))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(Long.class)
            .isEqualTo(1L);

        ArgumentCaptor<ConnectionDTO> connectionDTOArgumentCaptor = ArgumentCaptor.forClass(ConnectionDTO.class);

        verify(workspaceConnectionFacade).create(eq(5L), connectionDTOArgumentCaptor.capture());

        assertThat(connectionDTOArgumentCaptor.getValue())
            .hasFieldOrPropertyWithValue("componentName", "componentName")
            .hasFieldOrPropertyWithValue("name", "name")
            .hasFieldOrPropertyWithValue("parameters", Map.of("key1", "value1"));
    }

    @Test
    void testDeleteConnection() {
        webTestClient.delete()
            .uri("/internal/connections/1")
            .exchange()
            .expectStatus()
            .isNoContent();

        verify(workspaceConnectionFacade).delete(1L);
    }

    @Test
    void testGetConnection() {
        when(workspaceConnectionFacade.getConnection(1L)).thenReturn(createConnectionDTO());

        webTestClient.get()
            .uri("/internal/connections/1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.id")
            .isEqualTo(1)
            .jsonPath("$.name")
            .isEqualTo("name")
            .jsonPath("$.parameters")
            .doesNotExist();
    }

    @Test
    void testGetWorkspaceConnections() {
        when(workspaceConnectionFacade.getConnections(5L, "componentName", null, null, null))
            .thenReturn(List.of(createConnectionDTO()));

        webTestClient.get()
            .uri("/internal/workspaces/5/connections?componentName=componentName")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(ConnectionModel.class)
            .hasSize(1);
    }

    @Test
    void testUpdateConnectionWithoutParametersKeepsTheConnectionProperties() {
        webTestClient.patch()
            .uri("/internal/connections/1")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(new UpdateConnectionRequestModel().name("renamed")
                .tags(List.of())
                .version(3))
            .exchange()
            .expectStatus()
            .isNoContent();

        verify(workspaceConnectionFacade).update(1L, "renamed", List.of(), 3);
        verify(workspaceConnectionFacade, never()).update(anyLong(), anyString(), anyMap(), anyList(), anyInt());
        verify(connectionFacade, never()).update(anyLong(), anyString(), anyList(), anyInt());
    }

    @Test
    void testUpdateConnectionWithParametersReplacesTheConnectionProperties() {
        webTestClient.patch()
            .uri("/internal/connections/1")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(new UpdateConnectionRequestModel().name("renamed")
                .parameters(Map.of("baseUri", "https://new.example.com"))
                .tags(List.of())
                .version(3))
            .exchange()
            .expectStatus()
            .isNoContent();

        verify(workspaceConnectionFacade).update(
            1L, "renamed", Map.of("baseUri", "https://new.example.com"), List.of(), 3);
        verify(workspaceConnectionFacade, never()).update(anyLong(), anyString(), anyList(), anyInt());
        verify(connectionFacade, never()).update(anyLong(), anyString(), anyMap(), anyList(), anyInt());
    }

    private static ConnectionDTO createConnectionDTO() {
        return ConnectionDTO.builder()
            .componentName("componentName")
            .id(1L)
            .name("name")
            .parameters(Map.of("key1", "value1"))
            .status(ConnectionStatus.ACTIVE)
            .version(1)
            .build();
    }
}
