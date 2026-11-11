/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.public_.web.rest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.ee.embedded.execution.facade.ActionFacade;
import com.bytechef.ee.embedded.execution.public_.web.rest.config.EmbeddedExecutionPublicRestTestConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = EmbeddedExecutionPublicRestTestConfiguration.class)
@Import(ActionApiController.class)
@TestPropertySource(properties = "bytechef.edition=ee")
@WebMvcTest(ActionApiController.class)
@WithMockUser(username = ActionApiControllerIntTest.EXTERNAL_USER_ID)
class ActionApiControllerIntTest {

    static final String EXTERNAL_USER_ID = "external-user-1";

    private static final String ACTION_PATH =
        "/v1/{externalUserId}/components/{componentName}/versions/{componentVersion}/actions/{actionName}";
    private static final String REQUEST_BODY = "{\"input\":{\"uri\":\"https://example.com\"}}";

    @MockitoBean
    private ActionFacade actionFacade;

    @MockitoBean
    private EnvironmentService environmentService;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void beforeEach() {
        when(environmentService.getEnvironment("DEVELOPMENT")).thenReturn(Environment.DEVELOPMENT);
    }

    @Test
    void testExecuteActionReturnsTheActionResult() throws Exception {
        when(
            actionFacade.executeAction(
                eq(EXTERNAL_USER_ID), eq("httpClient"), eq(1), eq("get"), eq(Map.of("uri", "https://example.com")),
                isNull(), eq(Environment.DEVELOPMENT)))
                    .thenReturn(Map.of("status", 200));

        mockMvc
            .perform(
                post(ACTION_PATH, EXTERNAL_USER_ID, "httpClient", 1, "get")
                    .with(csrf())
                    .header("X-Environment", "DEVELOPMENT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(REQUEST_BODY))
            .andExpect(status().isOk())
            .andExpect(content().json("{\"result\":{\"status\":200}}", JsonCompareMode.STRICT));
    }

    @Test
    void testExecuteActionReturnsANullResultWhenTheActionReturnsNothing() throws Exception {
        when(
            actionFacade.executeAction(
                eq(EXTERNAL_USER_ID), eq("httpClient"), eq(1), eq("get"), eq(Map.of("uri", "https://example.com")),
                isNull(), eq(Environment.DEVELOPMENT)))
                    .thenReturn(null);

        mockMvc
            .perform(
                post(ACTION_PATH, EXTERNAL_USER_ID, "httpClient", 1, "get")
                    .with(csrf())
                    .header("X-Environment", "DEVELOPMENT")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(REQUEST_BODY))
            .andExpect(status().isOk())
            .andExpect(content().json("{\"result\":null}", JsonCompareMode.STRICT));
    }
}
