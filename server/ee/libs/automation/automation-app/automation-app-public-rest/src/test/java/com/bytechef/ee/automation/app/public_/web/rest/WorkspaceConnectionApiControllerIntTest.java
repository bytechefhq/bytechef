/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.ee.automation.app.dto.AppWorkspaceConnectionDTO;
import com.bytechef.ee.automation.app.facade.AppApiCollectionFacade;
import com.bytechef.ee.automation.app.facade.AppFacade;
import com.bytechef.ee.automation.app.facade.AppWorkflowFacade;
import com.bytechef.ee.automation.app.public_.web.rest.config.AppManagementRestTestConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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
@ContextConfiguration(classes = AppManagementRestTestConfiguration.class)
@TestPropertySource(properties = "bytechef.edition=ee")
@WebMvcTest(WorkspaceConnectionApiController.class)
@WithMockUser
class WorkspaceConnectionApiControllerIntTest {

    @MockitoBean
    private AppApiCollectionFacade appApiCollectionFacade;

    @MockitoBean
    private AppFacade appFacade;

    @MockitoBean
    private AppWorkflowFacade appWorkflowFacade;

    @MockitoBean
    private EnvironmentService environmentService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetWorkspaceConnectionsPassesWorkspaceEnvironmentAndComponent() throws Exception {
        when(environmentService.getEnvironment("DEVELOPMENT")).thenReturn(Environment.DEVELOPMENT);
        when(appFacade.getConnections(1049L, Environment.DEVELOPMENT, "slack")).thenReturn(
            List.of(
                new AppWorkspaceConnectionDTO(
                    101L, "Slack A", "slack", 1, "OAUTH2_AUTHORIZATION_CODE", "VALID", false, true),
                new AppWorkspaceConnectionDTO(-300L, "OpenAI", "openai", 1, null, "VALID", true, false)));

        mockMvc.perform(
            get("/v1/connections").param("workspaceId", "1049")
                .param("componentName", "slack")
                .header("X-Environment", "DEVELOPMENT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(101))
            .andExpect(jsonPath("$[0].name").value("Slack A"))
            .andExpect(jsonPath("$[0].componentName").value("slack"))
            .andExpect(jsonPath("$[0].connectionVersion").value(1))
            .andExpect(jsonPath("$[0].authorizationType").value("OAUTH2_AUTHORIZATION_CODE"))
            .andExpect(jsonPath("$[0].credentialStatus").value("VALID"))
            .andExpect(jsonPath("$[0].aiProvider").value(false))
            .andExpect(jsonPath("$[0].usable").value(true))
            .andExpect(jsonPath("$[1].aiProvider").value(true))
            .andExpect(jsonPath("$[1].usable").value(false))
            .andExpect(jsonPath("$[0].parameters").doesNotExist());
    }

    @Test
    void testGetWorkspaceConnectionsAccessDeniedIsForbiddenNotServerError() throws Exception {
        when(environmentService.getEnvironment("DEVELOPMENT")).thenReturn(Environment.DEVELOPMENT);
        when(appFacade.getConnections(1049L, Environment.DEVELOPMENT, null))
            .thenThrow(new AccessDeniedException("Access Denied"));

        mockMvc.perform(
            get("/v1/connections").param("workspaceId", "1049")
                .header("X-Environment", "DEVELOPMENT"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }
}
