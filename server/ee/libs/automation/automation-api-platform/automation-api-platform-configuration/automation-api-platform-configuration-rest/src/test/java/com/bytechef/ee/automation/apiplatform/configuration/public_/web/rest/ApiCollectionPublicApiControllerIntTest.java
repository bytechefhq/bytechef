/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionDTO;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionVersionUpdateDTO;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionVersionUpdateDTO.MissingConnection;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionVersionUpdateDTO.MissingInput;
import com.bytechef.ee.automation.apiplatform.configuration.facade.ApiCollectionFacade;
import com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.config.ApiCollectionPublicRestTestConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import java.time.Instant;
import java.util.List;
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
@WebMvcTest(ApiCollectionPublicApiController.class)
@WithMockUser
class ApiCollectionPublicApiControllerIntTest {

    private static final Instant CREATED_DATE = Instant.parse("2026-01-01T00:00:00Z");

    @MockitoBean
    private ApiCollectionFacade apiCollectionFacade;

    @MockitoBean(answers = Answers.CALLS_REAL_METHODS)
    private EnvironmentService environmentService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testCreateApiCollectionReturns201() throws Exception {
        when(apiCollectionFacade.createApiCollection(any(ApiCollectionDTO.class)))
            .thenReturn(apiCollection(1L, Environment.STAGING));

        mockMvc.perform(
            post("/v1/workspaces/7/api-collections")
                .header("X-Environment", "staging")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"projectId": 11, "projectVersion": 3, "name": "orders", "contextPath": "orders"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("orders"))
            .andExpect(jsonPath("$.contextPath").value("orders"))
            .andExpect(jsonPath("$.environment").value("STAGING"))
            .andExpect(jsonPath("$.projectId").value(11))
            .andExpect(jsonPath("$.projectVersion").value(3));

        ArgumentCaptor<ApiCollectionDTO> apiCollectionCaptor = ArgumentCaptor.forClass(ApiCollectionDTO.class);

        verify(apiCollectionFacade).createApiCollection(apiCollectionCaptor.capture());

        ApiCollectionDTO createdApiCollection = apiCollectionCaptor.getValue();

        assertThat(createdApiCollection.environment()).isEqualTo(Environment.STAGING);
        assertThat(createdApiCollection.collectionVersion()).isEqualTo(1);
        assertThat(createdApiCollection.projectId()).isEqualTo(11L);
    }

    @Test
    void testCreateApiCollectionWithALeadingSlashContextPathReturns400() throws Exception {
        mockMvc.perform(
            post("/v1/workspaces/7/api-collections")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"projectId": 11, "projectVersion": 3, "name": "orders", "contextPath": "/orders"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_CONTEXT_PATH"));

        verify(apiCollectionFacade, never()).createApiCollection(any(ApiCollectionDTO.class));
    }

    @Test
    void testGetApiCollectionsFiltersByTheRequestEnvironmentAndProject() throws Exception {
        when(apiCollectionFacade.getApiCollections(7L, (long) Environment.PRODUCTION.ordinal(), 11L, null))
            .thenReturn(List.of(apiCollection(1L, Environment.PRODUCTION)));

        mockMvc.perform(get("/v1/workspaces/7/api-collections").param("projectId", "11"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].environment").value("PRODUCTION"));
    }

    @Test
    void testGetApiCollectionOutsideEnvironmentReturns404() throws Exception {
        when(apiCollectionFacade.getApiCollection(1L)).thenReturn(apiCollection(1L, Environment.DEVELOPMENT));

        mockMvc.perform(get("/v1/api-collections/1"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("API_COLLECTION_NOT_FOUND"));

        mockMvc.perform(get("/v1/api-collections/1").header("X-Environment", "development"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void testGetApiCollectionTheCallerMayNotReadReturns404() throws Exception {
        when(apiCollectionFacade.getApiCollection(1L)).thenThrow(new AccessDeniedException("Access Denied"));

        mockMvc.perform(get("/v1/api-collections/1"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("API_COLLECTION_NOT_FOUND"));
    }

    @Test
    void testExplicitDevelopmentEnvironmentAgainstAProductionCollectionReturns404() throws Exception {
        when(apiCollectionFacade.getApiCollection(1L)).thenReturn(apiCollection(1L, Environment.PRODUCTION));

        mockMvc.perform(get("/v1/api-collections/1").header("X-Environment", "DEVELOPMENT"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("API_COLLECTION_NOT_FOUND"));

        when(apiCollectionFacade.getApiCollection(1L)).thenThrow(new AccessDeniedException("Access Denied"));

        mockMvc.perform(get("/v1/api-collections/1").header("X-Environment", "DEVELOPMENT"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateApiCollectionKeepsTheFieldsLeftOut() throws Exception {
        ApiCollectionDTO existingApiCollection = apiCollection(1L, Environment.PRODUCTION);

        when(apiCollectionFacade.getApiCollection(1L)).thenReturn(existingApiCollection);
        when(apiCollectionFacade.updateApiCollection(any(ApiCollectionDTO.class)))
            .thenReturn(existingApiCollection);

        mockMvc.perform(
            put("/v1/api-collections/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "renamed"}
                    """))
            .andExpect(status().isOk());

        ArgumentCaptor<ApiCollectionDTO> apiCollectionCaptor = ArgumentCaptor.forClass(ApiCollectionDTO.class);

        verify(apiCollectionFacade).updateApiCollection(apiCollectionCaptor.capture());

        ApiCollectionDTO updatedApiCollection = apiCollectionCaptor.getValue();

        assertThat(updatedApiCollection.id()).isEqualTo(1L);
        assertThat(updatedApiCollection.name()).isEqualTo("renamed");
        assertThat(updatedApiCollection.description()).isEqualTo("the orders");
        assertThat(updatedApiCollection.contextPath()).isEqualTo("orders");
    }

    @Test
    void testUpdateProjectVersionReturnsRemovedAndMissing() throws Exception {
        ApiCollectionDTO movedApiCollection = apiCollection(1L, Environment.PRODUCTION);

        when(apiCollectionFacade.getApiCollection(1L)).thenReturn(movedApiCollection);
        when(apiCollectionFacade.updateApiCollectionProjectVersion(1L, 4, true))
            .thenReturn(
                new ApiCollectionVersionUpdateDTO(
                    movedApiCollection, List.of("gone"), List.of("paused"),
                    List.of(new MissingConnection("create-order", "slack_1", "slack", "slack")),
                    List.of(new MissingInput("create-order", "customerId"))));

        mockMvc.perform(
            put("/v1/api-collections/1/project-version")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"projectVersion": 4, "removeMissingEndpoints": true}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.apiCollection.id").value(1))
            .andExpect(jsonPath("$.removedEndpointNames[0]").value("gone"))
            .andExpect(jsonPath("$.disabledEndpointNames[0]").value("paused"))
            .andExpect(jsonPath("$.missingConnections[0].endpointName").value("create-order"))
            .andExpect(jsonPath("$.missingConnections[0].workflowNodeName").value("slack_1"))
            .andExpect(jsonPath("$.missingConnections[0].key").value("slack"))
            .andExpect(jsonPath("$.missingConnections[0].componentName").value("slack"))
            .andExpect(jsonPath("$.missingInputs[0].endpointName").value("create-order"))
            .andExpect(jsonPath("$.missingInputs[0].inputName").value("customerId"));
    }

    @Test
    void testUpdateProjectVersionDefaultsToKeepingMissingEndpoints() throws Exception {
        ApiCollectionDTO movedApiCollection = apiCollection(1L, Environment.PRODUCTION);

        when(apiCollectionFacade.getApiCollection(1L)).thenReturn(movedApiCollection);
        when(apiCollectionFacade.updateApiCollectionProjectVersion(1L, 4, false))
            .thenReturn(
                new ApiCollectionVersionUpdateDTO(movedApiCollection, List.of(), List.of(), List.of(), List.of()));

        mockMvc.perform(
            put("/v1/api-collections/1/project-version")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"projectVersion": 4}
                    """))
            .andExpect(status().isOk());

        verify(apiCollectionFacade).updateApiCollectionProjectVersion(eq(1L), eq(4), eq(false));
    }

    @Test
    void testDeleteApiCollectionReturns204() throws Exception {
        when(apiCollectionFacade.getApiCollection(1L)).thenReturn(apiCollection(1L, Environment.PRODUCTION));

        mockMvc.perform(delete("/v1/api-collections/1"))
            .andExpect(status().isNoContent());

        verify(apiCollectionFacade).deleteApiCollection(1L);
    }

    @Test
    void testDeleteApiCollectionOutsideEnvironmentReturns404WithoutDeleting() throws Exception {
        when(apiCollectionFacade.getApiCollection(1L)).thenReturn(apiCollection(1L, Environment.DEVELOPMENT));

        mockMvc.perform(delete("/v1/api-collections/1"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("API_COLLECTION_NOT_FOUND"));

        verify(apiCollectionFacade, never()).deleteApiCollection(1L);
    }

    private static ApiCollectionDTO apiCollection(long id, Environment environment) {
        return new ApiCollectionDTO(
            1, "orders", "admin", CREATED_DATE, "the orders", true, List.of(), environment, id, "admin", CREATED_DATE,
            "orders", null, 11L, null, 21L, 3, List.of(), 0);
    }
}
