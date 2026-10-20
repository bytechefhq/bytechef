/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint.HttpMethod;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionDTO;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionEndpointDTO;
import com.bytechef.ee.automation.apiplatform.configuration.exception.ApiCollectionErrorType;
import com.bytechef.ee.automation.apiplatform.configuration.facade.ApiCollectionFacade;
import com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.config.ApiCollectionPublicRestTestConfiguration;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = ApiCollectionPublicRestTestConfiguration.class)
@TestPropertySource(properties = "bytechef.edition=ee")
@WebMvcTest(ApiCollectionEndpointPublicApiController.class)
@WithMockUser
class ApiCollectionEndpointPublicApiControllerIntTest {

    private static final Instant CREATED_DATE = Instant.parse("2026-01-01T00:00:00Z");

    @MockitoBean
    private ApiCollectionFacade apiCollectionFacade;

    @MockitoBean(answers = Answers.CALLS_REAL_METHODS)
    private EnvironmentService environmentService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetApiCollectionEndpointsListsTheEndpointsOfTheCollection() throws Exception {
        when(apiCollectionFacade.getApiCollection(100L)).thenReturn(apiCollection(Environment.PRODUCTION));
        when(apiCollectionFacade.getApiCollectionEndpoints(100L))
            .thenReturn(
                List.of(
                    endpoint(7L, "create-order",
                        List.of(new ProjectDeploymentWorkflowConnection(61L, "slack", "node_1")),
                        Map.of("region", "eu"))));

        mockMvc.perform(get("/v1/api-collections/100/endpoints"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(7))
            .andExpect(jsonPath("$[0].apiCollectionId").value(100))
            .andExpect(jsonPath("$[0].name").value("create-order"))
            .andExpect(jsonPath("$[0].httpMethod").value("POST"))
            .andExpect(jsonPath("$[0].path").value("orders"))
            .andExpect(jsonPath("$[0].workflowUuid").value("uuid-1"))
            .andExpect(jsonPath("$[0].enabled").value(true))
            .andExpect(jsonPath("$[0].connections[0].workflowNodeName").value("node_1"))
            .andExpect(jsonPath("$[0].connections[0].key").value("slack"))
            .andExpect(jsonPath("$[0].connections[0].connectionId").value(61))
            .andExpect(jsonPath("$[0].inputs.region").value("eu"));
    }

    @Test
    void testCreateEndpointWithConnectionsPassesSlots() throws Exception {
        when(apiCollectionFacade.getApiCollection(100L)).thenReturn(apiCollection(Environment.PRODUCTION));
        when(apiCollectionFacade.createApiCollectionEndpoint(any(ApiCollectionEndpointDTO.class), any(), any()))
            .thenReturn(
                endpoint(7L, "create-order", List.of(new ProjectDeploymentWorkflowConnection(61L, "slack", "node_1")),
                    Map.of("region", "eu")));

        mockMvc.perform(
            post("/v1/api-collections/100/endpoints")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "create-order", "httpMethod": "POST", "path": "orders", "workflowUuid": "uuid-1",
                        "connections": [{"workflowNodeName": "node_1", "key": "slack", "connectionId": 61}],
                        "inputs": {"region": "eu"}
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(7))
            .andExpect(jsonPath("$.connections[0].connectionId").value(61));

        ArgumentCaptor<ApiCollectionEndpointDTO> endpointCaptor = ArgumentCaptor.forClass(
            ApiCollectionEndpointDTO.class);
        ArgumentCaptor<List<ProjectDeploymentWorkflowConnection>> connectionsCaptor = ArgumentCaptor.captor();
        ArgumentCaptor<Map<String, Object>> inputsCaptor = ArgumentCaptor.captor();

        verify(apiCollectionFacade).createApiCollectionEndpoint(
            endpointCaptor.capture(), connectionsCaptor.capture(), inputsCaptor.capture());

        ApiCollectionEndpointDTO createdEndpoint = endpointCaptor.getValue();

        assertThat(createdEndpoint.apiCollectionId()).isEqualTo(100L);
        assertThat(createdEndpoint.name()).isEqualTo("create-order");
        assertThat(createdEndpoint.httpMethod()).isEqualTo(HttpMethod.POST);
        assertThat(createdEndpoint.path()).isEqualTo("orders");
        assertThat(createdEndpoint.workflowUuid()).isEqualTo("uuid-1");

        ProjectDeploymentWorkflowConnection connection = connectionsCaptor.getValue()
            .getFirst();

        assertThat(connection.getConnectionId()).isEqualTo(61L);
        assertThat(connection.getWorkflowConnectionKey()).isEqualTo("slack");
        assertThat(connection.getWorkflowNodeName()).isEqualTo("node_1");
        assertThat(inputsCaptor.getValue()).containsEntry("region", "eu");
    }

    @Test
    void testCreateEndpointWithoutConnectionsOrInputsPassesNulls() throws Exception {
        when(apiCollectionFacade.getApiCollection(100L)).thenReturn(apiCollection(Environment.PRODUCTION));
        when(apiCollectionFacade.createApiCollectionEndpoint(any(ApiCollectionEndpointDTO.class), any(), any()))
            .thenReturn(endpoint(7L, "create-order", List.of(), Map.of()));

        mockMvc.perform(
            post("/v1/api-collections/100/endpoints")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "create-order", "httpMethod": "POST", "path": "orders", "workflowUuid": "uuid-1"}
                    """))
            .andExpect(status().isCreated());

        verify(apiCollectionFacade).createApiCollectionEndpoint(
            any(ApiCollectionEndpointDTO.class), eq((List<ProjectDeploymentWorkflowConnection>) null),
            eq((Map<String, ?>) null));
    }

    @Test
    void testCreateEndpointWithUnknownWorkflowReturns404() throws Exception {
        when(apiCollectionFacade.getApiCollection(100L)).thenReturn(apiCollection(Environment.PRODUCTION));
        when(apiCollectionFacade.createApiCollectionEndpoint(any(ApiCollectionEndpointDTO.class), any(), any()))
            .thenThrow(new ConfigurationException("No such workflow", ApiCollectionErrorType.WORKFLOW_NOT_FOUND));

        mockMvc.perform(
            post("/v1/api-collections/100/endpoints")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "create-order", "httpMethod": "POST", "path": "orders", "workflowUuid": "uuid-x"}
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("WORKFLOW_NOT_FOUND"));
    }

    @Test
    void testGetEndpointReturnsTheEndpoint() throws Exception {
        stubEndpoint(7L, Environment.PRODUCTION, List.of(), Map.of());

        mockMvc.perform(get("/v1/api-collection-endpoints/7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(7))
            .andExpect(jsonPath("$.name").value("create-order"));
    }

    @Test
    void testUpdateEndpointConnections() throws Exception {
        stubEndpoint(
            7L, Environment.PRODUCTION, List.of(new ProjectDeploymentWorkflowConnection(61L, "slack", "node_1")),
            Map.of("region", "eu"));

        when(apiCollectionFacade.updateApiCollectionEndpoint(any(ApiCollectionEndpointDTO.class), any(), any()))
            .thenReturn(
                endpoint(7L, "create-order", List.of(new ProjectDeploymentWorkflowConnection(62L, "slack", "node_1")),
                    Map.of("region", "eu")));

        mockMvc.perform(
            put("/v1/api-collection-endpoints/7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"connections": [{"workflowNodeName": "node_1", "key": "slack", "connectionId": 62}]}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.connections[0].connectionId").value(62));

        ArgumentCaptor<List<ProjectDeploymentWorkflowConnection>> connectionsCaptor = ArgumentCaptor.captor();

        verify(apiCollectionFacade).updateApiCollectionEndpoint(
            any(ApiCollectionEndpointDTO.class), connectionsCaptor.capture(), isNull());

        assertThat(connectionsCaptor.getValue()).extracting(ProjectDeploymentWorkflowConnection::getConnectionId)
            .containsExactly(62L);
    }

    @Test
    void testUpdateEndpointInputsPassesNullConnectionsSoTheStoredOnesAreKept() throws Exception {
        stubEndpoint(
            7L, Environment.PRODUCTION, List.of(new ProjectDeploymentWorkflowConnection(61L, "slack", "node_1")),
            Map.of("region", "eu"));

        when(apiCollectionFacade.updateApiCollectionEndpoint(any(ApiCollectionEndpointDTO.class), any(), any()))
            .thenReturn(endpoint(7L, "create-order", List.of(), Map.of()));

        mockMvc.perform(
            put("/v1/api-collection-endpoints/7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"inputs": {"region": "us"}}
                    """))
            .andExpect(status().isOk());

        ArgumentCaptor<Map<String, Object>> inputsCaptor = ArgumentCaptor.captor();

        verify(apiCollectionFacade).updateApiCollectionEndpoint(
            any(ApiCollectionEndpointDTO.class), isNull(), inputsCaptor.capture());

        assertThat(inputsCaptor.getValue()).containsEntry("region", "us");
    }

    @Test
    void testUpdateEndpointFieldsKeepsTheOnesLeftOut() throws Exception {
        stubEndpoint(7L, Environment.PRODUCTION, List.of(), Map.of());

        when(apiCollectionFacade.updateApiCollectionEndpoint(any(ApiCollectionEndpointDTO.class), any(), any()))
            .thenReturn(endpoint(7L, "renamed", List.of(), Map.of()));

        mockMvc.perform(
            put("/v1/api-collection-endpoints/7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "renamed"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("renamed"));

        ArgumentCaptor<ApiCollectionEndpointDTO> endpointCaptor = ArgumentCaptor.forClass(
            ApiCollectionEndpointDTO.class);

        verify(apiCollectionFacade).updateApiCollectionEndpoint(endpointCaptor.capture(), isNull(), isNull());

        ApiCollectionEndpointDTO updatedEndpoint = endpointCaptor.getValue();

        assertThat(updatedEndpoint.id()).isEqualTo(7L);
        assertThat(updatedEndpoint.apiCollectionId()).isEqualTo(100L);
        assertThat(updatedEndpoint.name()).isEqualTo("renamed");
        assertThat(updatedEndpoint.httpMethod()).isEqualTo(HttpMethod.POST);
        assertThat(updatedEndpoint.path()).isEqualTo("orders");
        assertThat(updatedEndpoint.workflowUuid()).isNull();
    }

    @Test
    void testEnableEndpoint() throws Exception {
        stubEndpoint(7L, Environment.PRODUCTION, List.of(), Map.of());

        when(apiCollectionFacade.enableApiCollectionEndpoint(7L, true))
            .thenReturn(endpoint(7L, "create-order", List.of(), Map.of()));

        mockMvc.perform(
            put("/v1/api-collection-endpoints/7/enabled")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"enabled": true}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled").value(true));

        verify(apiCollectionFacade).enableApiCollectionEndpoint(7L, true);
    }

    @Test
    void testEndpointOfCollectionOutsideEnvironmentReturns404() throws Exception {
        stubEndpoint(7L, Environment.DEVELOPMENT, List.of(), Map.of());

        mockMvc.perform(get("/v1/api-collection-endpoints/7"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("API_COLLECTION_NOT_FOUND"));

        mockMvc.perform(
            put("/v1/api-collection-endpoints/7/enabled")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"enabled": true}
                    """))
            .andExpect(status().isNotFound());

        mockMvc.perform(delete("/v1/api-collection-endpoints/7"))
            .andExpect(status().isNotFound());

        verify(apiCollectionFacade, never()).enableApiCollectionEndpoint(anyLong(), anyBoolean());
        verify(apiCollectionFacade, never()).deleteApiCollectionEndpoint(anyLong());
    }

    @Test
    void testExplicitDevelopmentEnvironmentAgainstAProductionEndpointReturns404() throws Exception {
        stubEndpoint(7L, Environment.PRODUCTION, List.of(), Map.of());

        mockMvc.perform(get("/v1/api-collection-endpoints/7").header("X-Environment", "DEVELOPMENT"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("API_COLLECTION_NOT_FOUND"));

        when(apiCollectionFacade.getApiCollectionEndpoint(7L)).thenThrow(new AccessDeniedException("Access Denied"));

        mockMvc.perform(get("/v1/api-collection-endpoints/7").header("X-Environment", "DEVELOPMENT"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateCreateAndListOutsideEnvironmentReturn404WithoutMutating() throws Exception {
        stubEndpoint(7L, Environment.DEVELOPMENT, List.of(), Map.of());

        mockMvc.perform(
            put("/v1/api-collection-endpoints/7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "renamed", "inputs": {"region": "us"}}
                    """))
            .andExpect(status().isNotFound());

        mockMvc.perform(
            post("/v1/api-collections/100/endpoints")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "create-order", "httpMethod": "POST", "path": "orders", "workflowUuid": "uuid-1"}
                    """))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/v1/api-collections/100/endpoints"))
            .andExpect(status().isNotFound());

        verify(apiCollectionFacade, never()).updateApiCollectionEndpoint(
            any(ApiCollectionEndpointDTO.class), any(), any());
        verify(apiCollectionFacade, never()).createApiCollectionEndpoint(
            any(ApiCollectionEndpointDTO.class), any(), any());
        verify(apiCollectionFacade, never()).getApiCollectionEndpoints(anyLong());
    }

    @Test
    void testDeleteEndpointReturns204() throws Exception {
        stubEndpoint(7L, Environment.PRODUCTION, List.of(), Map.of());

        mockMvc.perform(delete("/v1/api-collection-endpoints/7"))
            .andExpect(status().isNoContent());

        verify(apiCollectionFacade).deleteApiCollectionEndpoint(7L);
    }

    private void stubEndpoint(
        long id, Environment environment, List<ProjectDeploymentWorkflowConnection> connections,
        Map<String, ?> inputs) {

        when(apiCollectionFacade.getApiCollectionEndpoint(id))
            .thenReturn(endpoint(id, "create-order", connections, inputs));
        when(apiCollectionFacade.getApiCollection(100L)).thenReturn(apiCollection(environment));
    }

    private static ApiCollectionDTO apiCollection(Environment environment) {
        return new ApiCollectionDTO(
            1, "orders", "admin", CREATED_DATE, "the orders", true, List.of(), environment, 100L, "admin",
            CREATED_DATE, "orders", null, 11L, null, 21L, 3, List.of(), 0);
    }

    private static ApiCollectionEndpointDTO endpoint(
        long id, String name, List<ProjectDeploymentWorkflowConnection> connections, Map<String, ?> inputs) {

        return new ApiCollectionEndpointDTO(
            100L, connections, "admin", CREATED_DATE, true, HttpMethod.POST, id, inputs, "admin", CREATED_DATE, name,
            "orders", 901L, 0, "uuid-1");
    }
}
