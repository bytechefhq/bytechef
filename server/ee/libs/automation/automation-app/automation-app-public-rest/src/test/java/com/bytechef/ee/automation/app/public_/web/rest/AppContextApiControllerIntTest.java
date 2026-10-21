/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.ee.automation.app.dto.AppContextDTO;
import com.bytechef.ee.automation.app.dto.AppCurrentUserDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.facade.AppExecutionFacade;
import com.bytechef.ee.automation.app.facade.AppWorkflowExecutionFacade;
import com.bytechef.ee.automation.app.public_.web.rest.config.AppExecutionRestTestConfiguration;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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
@WebMvcTest(AppContextApiController.class)
@WithMockUser
class AppContextApiControllerIntTest {

    @MockitoBean
    private AppExecutionFacade appExecutionFacade;

    @MockitoBean
    private AppWorkflowExecutionFacade appWorkflowExecutionFacade;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testContextReturnsConfigurationAndSignInSettings() throws Exception {
        when(appExecutionFacade.getContext()).thenReturn(
            new AppContextDTO(
                1L, "CRM", 2L, "Production", Environment.PRODUCTION, Map.of("theme", "dark"),
                AppUserAuthMode.REQUIRED, "https://bytechef.example", "app-client-1",
                "https://bytechef.example/oauth2/authorize", "https://bytechef.example/oauth2/token"));

        mockMvc.perform(get("/api/app/v1/context"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.appId").value(1))
            .andExpect(jsonPath("$.appName").value("CRM"))
            .andExpect(jsonPath("$.appInstanceId").value(2))
            .andExpect(jsonPath("$.appInstanceName").value("Production"))
            .andExpect(jsonPath("$.environment").value("PRODUCTION"))
            .andExpect(jsonPath("$.configuration.theme").value("dark"))
            .andExpect(jsonPath("$.userAuthMode").value("REQUIRED"))
            .andExpect(jsonPath("$.issuer").value("https://bytechef.example"))
            .andExpect(jsonPath("$.clientId").value("app-client-1"))
            .andExpect(jsonPath("$.authorizationEndpoint").value("https://bytechef.example/oauth2/authorize"))
            .andExpect(jsonPath("$.tokenEndpoint").value("https://bytechef.example/oauth2/token"));
    }

    @Test
    void testContextWithoutClientHasNullSignInSettings() throws Exception {
        when(appExecutionFacade.getContext()).thenReturn(
            new AppContextDTO(
                1L, "CRM", 2L, "Development", Environment.DEVELOPMENT, Map.of(), AppUserAuthMode.NONE, null, null,
                null, null));

        mockMvc.perform(get("/api/app/v1/context"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userAuthMode").value("NONE"))
            .andExpect(jsonPath("$.issuer").isEmpty())
            .andExpect(jsonPath("$.clientId").isEmpty());
    }

    @Test
    void testMeReturnsUserAndScopes() throws Exception {
        when(appExecutionFacade.getCurrentUser()).thenReturn(
            new AppCurrentUserDTO(
                42L, "jane@example.com", "Jane", "Doe", "jane@example.com", "EDITOR", List.of("APP_VIEW", "APP_USE")));

        mockMvc.perform(get("/api/app/v1/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(42))
            .andExpect(jsonPath("$.login").value("jane@example.com"))
            .andExpect(jsonPath("$.firstName").value("Jane"))
            .andExpect(jsonPath("$.lastName").value("Doe"))
            .andExpect(jsonPath("$.email").value("jane@example.com"))
            .andExpect(jsonPath("$.workspaceRole").value("EDITOR"))
            .andExpect(jsonPath("$.scopes.length()").value(2))
            .andExpect(jsonPath("$.scopes[1]").value("APP_USE"));
    }

    @Test
    void testMeWithoutUserIs401() throws Exception {
        when(appExecutionFacade.getCurrentUser()).thenThrow(
            new ConfigurationException("A signed-in user is required", AppErrorType.APP_USER_REQUIRED));

        mockMvc.perform(get("/api/app/v1/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string(AbstractAppApiController.APP_ERROR_HEADER, "true"))
            .andExpect(jsonPath("$.code").value("APP_USER_REQUIRED"));
    }

    @Test
    void testUserTokenErrorsMapToTheirStatus() throws Exception {
        when(appExecutionFacade.getCurrentUser()).thenThrow(
            new ConfigurationException("Invalid", AppErrorType.APP_USER_TOKEN_INVALID));

        mockMvc.perform(get("/api/app/v1/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("APP_USER_TOKEN_INVALID"));

        doThrow(new ConfigurationException("Forbidden", AppErrorType.APP_USER_FORBIDDEN))
            .when(appExecutionFacade)
            .getCurrentUser();

        mockMvc.perform(get("/api/app/v1/me"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("APP_USER_FORBIDDEN"));
    }
}
