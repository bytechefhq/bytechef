/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.facade.AppFacade;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.web.rest.error.GlobalResponseEntityExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Proves the controller's own error handlers win over the {@code HIGHEST_PRECEDENCE} global advice, which would answer
 * these exceptions with 500 and 400.
 *
 * @version ee
 */
class AppApiControllerGlobalHandlerTest {

    private AppFacade appFacade;
    private MockMvc mockMvc;

    @BeforeEach
    void beforeEach() {
        appFacade = mock(AppFacade.class);

        EnvironmentService environmentService = new EnvironmentService() {

            @Override
            public List<Environment> getEnvironments() {
                return List.of(Environment.values());
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(new AppApiController(appFacade, environmentService))
            .setControllerAdvice(new GlobalResponseEntityExceptionHandler())
            .build();
    }

    @Test
    void testAccessDeniedBecomesForbiddenDespiteTheGlobalHandler() throws Exception {
        when(appFacade.getApp(1L)).thenThrow(new AccessDeniedException("Access Denied"));

        mockMvc.perform(get("/v1/apps/1"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void testAppNotFoundBecomesNotFoundDespiteTheGlobalHandler() throws Exception {
        when(appFacade.getApp(1L)).thenThrow(new ConfigurationException("missing", AppErrorType.APP_NOT_FOUND));

        mockMvc.perform(get("/v1/apps/1"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("APP_NOT_FOUND"));
    }

    @Test
    void testIllegalArgumentBecomesBadRequestWithCodeDespiteTheGlobalHandler() throws Exception {
        when(appFacade.getApp(1L)).thenThrow(new IllegalArgumentException("bad"));

        mockMvc.perform(get("/v1/apps/1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
}
