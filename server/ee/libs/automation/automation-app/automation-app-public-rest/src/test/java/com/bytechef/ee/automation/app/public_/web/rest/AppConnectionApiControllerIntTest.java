/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.ee.automation.app.dto.AppActionDTO;
import com.bytechef.ee.automation.app.dto.AppBindingDTO;
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
@WebMvcTest(AppConnectionApiController.class)
@WithMockUser
class AppConnectionApiControllerIntTest {

    @MockitoBean
    private AppExecutionFacade appExecutionFacade;

    @MockitoBean
    private AppWorkflowExecutionFacade appWorkflowExecutionFacade;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetConnectionsOmitsConnectionIdentity() throws Exception {
        when(appExecutionFacade.getBindings()).thenReturn(
            List.of(
                new AppBindingDTO(
                    7L, 3L, "crm", 42L, "Secret connection name", "hubspot", 2, 5, List.of("createContact"),
                    List.of("searchRecords"), true, List.of("GET", "POST"))));

        mockMvc.perform(get("/api/app/v1/connections"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].bindingKey").value("crm"))
            .andExpect(jsonPath("$[0].componentName").value("hubspot"))
            .andExpect(jsonPath("$[0].connectionVersion").value(2))
            .andExpect(jsonPath("$[0].componentVersion").value(5))
            .andExpect(jsonPath("$[0].actionNames[0]").value("createContact"))
            .andExpect(jsonPath("$[0].toolNames[0]").value("searchRecords"))
            .andExpect(jsonPath("$[0].proxyEnabled").value(true))
            .andExpect(jsonPath("$[0].proxyAllowedMethods[1]").value("POST"))
            .andExpect(jsonPath("$[0].connectionId").doesNotExist())
            .andExpect(jsonPath("$[0].connectionName").doesNotExist())
            .andExpect(jsonPath("$[0].appId").doesNotExist())
            .andExpect(jsonPath("$[0].id").doesNotExist());
    }

    @Test
    void testGetConnectionsSerialisesAnUnresolvableComponentVersionAsNull() throws Exception {
        when(appExecutionFacade.getBindings()).thenReturn(
            List.of(
                new AppBindingDTO(
                    7L, 3L, "legacy", 42L, "Legacy", "deleted-component", 2, null, List.of("createContact"),
                    List.of(), false, List.of())));

        mockMvc.perform(get("/api/app/v1/connections"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].bindingKey").value("legacy"))
            .andExpect(jsonPath("$[0].connectionVersion").value(2))
            .andExpect(jsonPath("$[0].componentVersion").value(nullValue()));
    }

    @Test
    void testExecuteActionWrapsTheResult() throws Exception {
        when(appExecutionFacade.executeAction("crm", "createContact", 2, Map.of("email", "a@b.c")))
            .thenReturn(Map.of("id", "99"));

        mockMvc.perform(
            post("/api/app/v1/connections/crm/actions/createContact")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"componentVersion\":2,\"input\":{\"email\":\"a@b.c\"}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.id").value("99"));
    }

    @Test
    void testExecuteActionWithoutComponentVersionPassesNull() throws Exception {
        when(appExecutionFacade.executeAction(eq("crm"), eq("createContact"), isNull(), anyMap()))
            .thenReturn(List.of(1, 2));

        mockMvc.perform(
            post("/api/app/v1/connections/crm/actions/createContact")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"input\":{}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result[1]").value(2));

        verify(appExecutionFacade).executeAction("crm", "createContact", null, Map.of());
    }

    @Test
    void testExecuteActionWithNullResult() throws Exception {
        when(appExecutionFacade.executeAction(eq("crm"), eq("noop"), isNull(), anyMap())).thenReturn(null);

        mockMvc.perform(
            post("/api/app/v1/connections/crm/actions/noop")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"input\":{}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").hasJsonPath())
            .andExpect(jsonPath("$.result").value((Object) null));
    }

    @Test
    void testExecuteActionConnectionUnavailableIsConflict() throws Exception {
        when(appExecutionFacade.executeAction(eq("crm"), eq("createContact"), isNull(), anyMap()))
            .thenThrow(new ConfigurationException("unavailable", AppErrorType.APP_CONNECTION_UNAVAILABLE));

        mockMvc.perform(
            post("/api/app/v1/connections/crm/actions/createContact")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"input\":{}}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("APP_CONNECTION_UNAVAILABLE"));
    }

    @Test
    void testGetActions() throws Exception {
        when(appExecutionFacade.getActions()).thenReturn(
            List.of(
                new AppActionDTO(
                    "crm", "hubspot", "createContact", "Creates a contact",
                    Map.of("type", "object", "properties", Map.of("email", Map.of("type", "string")))),
                new AppActionDTO("crm", "hubspot", "ping", null, Map.of("type", "object"))));

        mockMvc.perform(get("/api/app/v1/actions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].bindingKey").value("crm"))
            .andExpect(jsonPath("$[0].componentName").value("hubspot"))
            .andExpect(jsonPath("$[0].actionName").value("createContact"))
            .andExpect(jsonPath("$[0].description").value("Creates a contact"))
            .andExpect(jsonPath("$[0].inputSchema.properties.email.type").value("string"))
            .andExpect(jsonPath("$[1].description").value((Object) null))
            .andExpect(jsonPath("$[1].description").hasJsonPath());
    }

    @Test
    void testGetActionsAccessDeniedIsForbidden() throws Exception {
        when(appExecutionFacade.getActions()).thenThrow(new AccessDeniedException("Not an app request"));

        mockMvc.perform(get("/api/app/v1/actions"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }
}
