/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.ee.embedded.configuration.facade.AppEventFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserConnectionFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectionAdminFacade;
import com.bytechef.ee.embedded.configuration.facade.IntegrationFacade;
import com.bytechef.ee.embedded.configuration.facade.IntegrationInstanceConfigurationFacade;
import com.bytechef.ee.embedded.configuration.facade.IntegrationInstanceFacade;
import com.bytechef.ee.embedded.configuration.facade.IntegrationWorkflowFacade;
import com.bytechef.ee.embedded.configuration.service.AppEventService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.web.rest.config.EmbeddedConfigurationRestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.web.rest.config.EmbeddedConfigurationRestTestConfiguration;
import com.bytechef.ee.embedded.configuration.web.rest.mapper.ConnectionMapper;
import com.bytechef.ee.embedded.configuration.web.rest.model.ConnectionModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.TagModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.UpdateConnectionCredentialsRequestModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.UpdateConnectionRequestModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.UpdateTagsRequestModel;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = EmbeddedConfigurationRestTestConfiguration.class)
@WebMvcTest(ConnectionApiController.class)
@EmbeddedConfigurationRestConfigurationSharedMocks
class ConnectionApiControllerIntTest {

    @MockitoBean
    private AppEventFacade appEventFacade;

    @MockitoBean
    private AppEventService appEventService;

    @MockitoBean
    private ComponentConnectionFacade componentConnectionFacade;

    @MockitoBean
    private ConnectedUserProjectFacade connectedUserProjectFacade;

    @MockitoBean
    private EnvironmentService environmentService;

    @MockitoBean
    private IntegrationFacade integrationFacade;

    @MockitoBean
    private IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade;

    @MockitoBean
    private IntegrationInstanceFacade integrationInstanceFacade;

    @MockitoBean
    private IntegrationInstanceService integrationInstanceService;

    @MockitoBean
    private IntegrationService integrationService;

    @MockitoBean
    private IntegrationWorkflowFacade integrationWorkflowFacade;

    @MockitoBean
    private WorkflowFacade workflowFacade;

    @MockitoBean
    private WorkflowService workflowService;

    @Autowired
    private ConnectionAdminFacade connectionAdminFacade;

    @Autowired
    private ConnectedUserConnectionFacade connectedUserConnectionFacade;

    @Autowired
    private ConnectionMapper connectionMapper;

    @Autowired
    private MockMvc mockMvc;

    private WebTestClient webTestClient;

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @BeforeEach
    void beforeEach() {
        this.webTestClient = MockMvcWebTestClient
            .bindTo(mockMvc)
            .build();
    }

    @Test
    void testDeleteConnection() {
        this.webTestClient
            .delete()
            .uri("/internal/connections/1")
            .exchange()
            .expectStatus()
            .isNoContent();

        verify(connectionAdminFacade).deleteConnection(1L);
    }

    @Test
    void testGetConnection() {
        ConnectionDTO connectionDTO = getConnection();

        when(connectionAdminFacade.getConnection(1L))
            .thenReturn(connectionDTO);

        this.webTestClient
            .get()
            .uri("/internal/connections/1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(ConnectionModel.class)
            .isEqualTo(Validate.notNull(connectionMapper.convert(connectionDTO), "connectionModel")
                .parameters(null));
    }

    @Test
    void testGetConnectionTags() {
        when(connectionAdminFacade.getConnectionTags())
            .thenReturn(List.of(new Tag(1L, "tag1"), new Tag(2L, "tag2")));

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
    }

    @Test
    void testGetConnections() {
        ConnectionDTO connectionDTO = getConnection();

        when(connectionAdminFacade.getConnections((String) null, null, null, null))
            .thenReturn(List.of(connectionDTO));

        this.webTestClient
            .get()
            .uri("/internal/connections")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(ConnectionModel.class)
            .contains(Validate.notNull(connectionMapper.convert(connectionDTO), "connectionMapper")
                .parameters(null))
            .hasSize(1);

        when(connectionAdminFacade.getConnections("component1", null, null, null))
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

        when(connectionAdminFacade.getConnections((String) null, 1, null, null))
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

        when(connectionAdminFacade.getConnections("component1", 1, null, null))
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
    void testPostConnection() {
        ConnectionDTO connectionDTO = getConnection();
        ConnectionModel connectionModel = new ConnectionModel().componentName("componentName")
            .name("name")
            .parameters(Map.of("key1", "value1"));

        when(connectionAdminFacade.createConnection(any(), anyBoolean())).thenReturn(getConnection().id());

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
            .expectBody(Long.class)
            .isEqualTo(connectionDTO.id());

        ArgumentCaptor<ConnectionDTO> connectionArgumentCaptor = ArgumentCaptor.forClass(ConnectionDTO.class);

        verify(connectionAdminFacade).createConnection(connectionArgumentCaptor.capture(), eq(false));

        assertThat(connectionArgumentCaptor.getValue())
            .hasFieldOrPropertyWithValue("componentName", "componentName")
            .hasFieldOrPropertyWithValue("name", "name")
            .hasFieldOrPropertyWithValue("parameters", Map.of("key1", "value1"));
    }

    @Test
    void testPostConnectedUserConnectionCreatesTheConnectionAsAdmin() {
        when(connectedUserConnectionFacade.createConnectedUserConnectionAsAdmin(eq(5L), any())).thenReturn(77L);

        postConnectedUserConnection(5L, 77L);

        verify(connectedUserConnectionFacade).createConnectedUserConnectionAsAdmin(eq(5L), any());
        verify(connectedUserConnectionFacade, never()).createConnectedUserConnection(anyLong(), any());
    }

    @Test
    void testPostConnectedUserConnectionKeepsAConnectedUserCreatingTheirOwnConnection() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new EmbeddedApiKeyAuthenticationToken(
                    Environment.PRODUCTION.ordinal(), 5L, new User("external-user-5", "", List.of()), false));

        when(connectedUserConnectionFacade.createConnectedUserConnection(eq(5L), any())).thenReturn(78L);

        postConnectedUserConnection(5L, 78L);

        verify(connectedUserConnectionFacade).createConnectedUserConnection(eq(5L), any());
        verify(connectedUserConnectionFacade, never()).createConnectedUserConnectionAsAdmin(anyLong(), any());
    }

    @Test
    void testGetConnectedUserConnectionsListsThemForAdminWithoutCredentials() {
        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .authorizationParameters(null)
            .componentName("componentName")
            .id(1L)
            .name("name")
            .parameters(null)
            .version(1)
            .build();

        when(connectedUserConnectionFacade.getSharedConnectionIds()).thenReturn(Set.of());
        when(connectedUserConnectionFacade.getConnectedUserConnectionsForAdmin(eq(5L), any(), anyList()))
            .thenReturn(List.of(connectionDTO));

        this.webTestClient
            .get()
            .uri("/internal/connected-users/5/components/componentName/connections")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(ConnectionModel.class)
            .value(connectionModels -> {
                assertThat(connectionModels).hasSize(1);

                ConnectionModel connectionModel = connectionModels.getFirst();

                assertThat(connectionModel.getAuthorizationParameters()).isNullOrEmpty();
                assertThat(connectionModel.getParameters()).isNullOrEmpty();
            });

        verify(connectedUserConnectionFacade).getConnectedUserConnectionsForAdmin(eq(5L), any(), anyList());
        verify(connectedUserConnectionFacade, never()).getConnectedUserConnections(anyLong(), any(), anyList());
    }

    @Test
    void testGetConnectedUserConnectionsKeepsAConnectedUserListingTheirOwnConnections() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new EmbeddedApiKeyAuthenticationToken(
                    Environment.PRODUCTION.ordinal(), 5L, new User("external-user-5", "", List.of()), false));

        when(connectedUserConnectionFacade.getSharedConnectionIds()).thenReturn(Set.of());
        when(connectedUserConnectionFacade.getConnectedUserConnections(eq(5L), any(), anyList()))
            .thenReturn(List.of(getConnection()));

        this.webTestClient
            .get()
            .uri("/internal/connected-users/5/components/componentName/connections")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(ConnectionModel.class)
            .hasSize(1);

        verify(connectedUserConnectionFacade).getConnectedUserConnections(eq(5L), any(), anyList());
        verify(connectedUserConnectionFacade, never()).getConnectedUserConnectionsForAdmin(anyLong(), any(), anyList());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testPatchConnection() {
        UpdateConnectionRequestModel updateConnectionRequestModel = new UpdateConnectionRequestModel()
            .name("name2")
            .tags(List.of(new TagModel().name("tag1")))
            .version(3);

        this.webTestClient
            .patch()
            .uri("/internal/connections/1")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updateConnectionRequestModel)
            .exchange()
            .expectStatus()
            .isNoContent();

        ArgumentCaptor<List<Tag>> tagsArgumentCaptor = ArgumentCaptor.forClass(List.class);

        verify(connectionAdminFacade).updateConnection(
            eq(1L), eq("name2"), tagsArgumentCaptor.capture(), isNull(), eq(3));

        assertThat(tagsArgumentCaptor.getValue())
            .singleElement()
            .hasFieldOrPropertyWithValue("name", "tag1");
    }

    @Test
    void testPutConnectionCredentials() {
        this.webTestClient
            .put()
            .uri("/internal/connections/1/credentials")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(new UpdateConnectionCredentialsRequestModel().parameters(Map.of("token", "secret"))
                .version(3))
            .exchange()
            .expectStatus()
            .isNoContent();

        verify(connectionAdminFacade).updateConnectionCredentials(1L, Map.of("token", "secret"), 3);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPutConnectionTags() {
        this.webTestClient
            .put()
            .uri("/internal/connections/1/tags")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(new UpdateTagsRequestModel().tags(List.of(new TagModel().name("tag1"))))
            .exchange()
            .expectStatus()
            .isNoContent();

        ArgumentCaptor<List<Tag>> tagsArgumentCaptor = ArgumentCaptor.forClass(List.class);

        verify(connectionAdminFacade).updateConnectionTags(eq(1L), tagsArgumentCaptor.capture());

        assertThat(tagsArgumentCaptor.getValue())
            .singleElement()
            .hasFieldOrPropertyWithValue("name", "tag1");
    }

    private void postConnectedUserConnection(long connectedUserId, long expectedConnectionId) {
        this.webTestClient
            .post()
            .uri("/internal/connected-users/" + connectedUserId + "/connections")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(new ConnectionModel().componentName("componentName")
                .name("name"))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(Long.class)
            .isEqualTo(expectedConnectionId);
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

}
