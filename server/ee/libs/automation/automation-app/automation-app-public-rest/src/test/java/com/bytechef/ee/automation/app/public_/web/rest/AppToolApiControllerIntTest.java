/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.ee.automation.app.dto.AppToolDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.facade.AppExecutionFacade;
import com.bytechef.ee.automation.app.facade.AppWorkflowExecutionFacade;
import com.bytechef.ee.automation.app.public_.web.rest.config.AppExecutionRestTestConfiguration;
import com.bytechef.exception.ConfigurationException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
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
@ContextConfiguration(classes = AppExecutionRestTestConfiguration.class)
@TestPropertySource(properties = "bytechef.edition=ee")
@WebMvcTest(AppToolApiController.class)
@WithMockUser
class AppToolApiControllerIntTest {

    @MockitoBean
    private AppExecutionFacade appExecutionFacade;

    @MockitoBean
    private AppWorkflowExecutionFacade appWorkflowExecutionFacade;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetTools() throws Exception {
        when(appExecutionFacade.getTools()).thenReturn(
            List.of(
                new AppToolDTO(
                    "CRM__SEARCH_RECORDS", "crm", "hubspot", "searchRecords", "Searches records",
                    "{\"type\":\"object\"}"),
                new AppToolDTO("CRM__PING", "crm", "hubspot", "ping", null, "{}")));

        mockMvc.perform(get("/api/app/v1/tools"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("CRM__SEARCH_RECORDS"))
            .andExpect(jsonPath("$[0].bindingKey").value("crm"))
            .andExpect(jsonPath("$[0].componentName").value("hubspot"))
            .andExpect(jsonPath("$[0].toolName").value("searchRecords"))
            .andExpect(jsonPath("$[0].description").value("Searches records"))
            .andExpect(jsonPath("$[0].parameters").value("{\"type\":\"object\"}"))
            .andExpect(jsonPath("$[1].name").value("CRM__PING"));
    }

    @Test
    void testExecuteToolWrapsTheResult() throws Exception {
        when(appExecutionFacade.executeTool("crm", "searchRecords", Map.of("q", "x")))
            .thenReturn(Map.of("count", 3));

        mockMvc.perform(
            post("/api/app/v1/connections/crm/tools/searchRecords")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"input\":{\"q\":\"x\"}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.count").value(3));
    }

    @Test
    void testExecuteToolWithNullResult() throws Exception {
        when(appExecutionFacade.executeTool(eq("crm"), eq("ping"), anyMap())).thenReturn(null);

        mockMvc.perform(
            post("/api/app/v1/connections/crm/tools/ping")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"input\":{}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value((Object) null))
            .andExpect(jsonPath("$.result").hasJsonPath());
    }

    @Test
    void testExecuteToolNotAllowedIsForbidden() throws Exception {
        when(appExecutionFacade.executeTool(eq("crm"), eq("deleteAll"), anyMap()))
            .thenThrow(new ConfigurationException("not allowed", AppErrorType.APP_OPERATION_NOT_ALLOWED));

        mockMvc.perform(
            post("/api/app/v1/connections/crm/tools/deleteAll")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"input\":{}}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("APP_OPERATION_NOT_ALLOWED"));
    }

    @Test
    void testExecuteToolTimeoutIsGatewayTimeout() throws Exception {
        when(appExecutionFacade.executeTool(eq("crm"), eq("slow"), anyMap()))
            .thenThrow(new ConfigurationException("timed out", AppErrorType.APP_EXECUTION_TIMEOUT));

        mockMvc.perform(
            post("/api/app/v1/connections/crm/tools/slow")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"input\":{}}"))
            .andExpect(status().isGatewayTimeout())
            .andExpect(jsonPath("$.code").value("APP_EXECUTION_TIMEOUT"));
    }

    @Test
    void testGetToolsAccessDeniedIsForbidden() throws Exception {
        when(appExecutionFacade.getTools()).thenThrow(new AccessDeniedException("Not an app request"));

        mockMvc.perform(get("/api/app/v1/tools"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void testExecuteToolPassesTheInputThrough() throws Exception {
        when(appExecutionFacade.executeTool(any(), any(), anyMap())).thenReturn("ok");

        mockMvc.perform(
            post("/api/app/v1/connections/crm/tools/searchRecords")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"input\":{\"q\":\"x\",\"limit\":5}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("ok"));

        verify(appExecutionFacade).executeTool("crm", "searchRecords", Map.of("q", "x", "limit", 5));
    }
}
