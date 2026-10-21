/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.public_.web.rest;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.facade.AppExecutionFacade;
import com.bytechef.ee.automation.app.facade.AppWorkflowExecutionFacade;
import com.bytechef.ee.automation.app.proxy.ProxyRequest;
import com.bytechef.exception.AbstractErrorType;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.exception.ExecutionException;
import com.bytechef.web.rest.error.GlobalResponseEntityExceptionHandler;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Proves the controllers' own error handlers win over the {@code HIGHEST_PRECEDENCE} global advice, which would answer
 * these app errors with 400 and no {@code code}, and a component's {@link ExecutionException} with a 400 ProblemDetail
 * echoing its input parameters.
 *
 * @version ee
 */
class AppExecutionControllersGlobalHandlerTest {

    private AppExecutionFacade appExecutionFacade;
    private AppWorkflowExecutionFacade appWorkflowExecutionFacade;
    private MockMvc mockMvc;

    @BeforeEach
    void beforeEach() {
        appExecutionFacade = mock(AppExecutionFacade.class);
        appWorkflowExecutionFacade = mock(AppWorkflowExecutionFacade.class);

        mockMvc = MockMvcBuilders
            .standaloneSetup(
                new AppToolApiController(appExecutionFacade), new AppConnectionApiController(appExecutionFacade),
                new AppProxyApiController(appExecutionFacade),
                new AppWorkflowApiController(appWorkflowExecutionFacade))
            .setControllerAdvice(new GlobalResponseEntityExceptionHandler())
            .build();
    }

    @Test
    void testOperationNotAllowedBecomesForbiddenDespiteTheGlobalHandler() throws Exception {
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
    void testActionExecutionFailureBecomesUnprocessableDespiteTheGlobalHandler() throws Exception {
        when(appExecutionFacade.executeAction(eq("crm"), eq("createRecord"), eq(null), anyMap()))
            .thenThrow(
                new ExecutionException(
                    "perform failed", Map.of("apiToken", "s3cr3t"),
                    new AbstractErrorType(ActionDefinition.class, 104) {}));

        mockMvc.perform(
            post("/api/app/v1/connections/crm/actions/createRecord")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"input\":{}}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("COMPONENT_ERROR"))
            .andExpect(jsonPath("$.message").value("perform failed"))
            .andExpect(content().string(not(containsString("s3cr3t"))));
    }

    @Test
    void testActionProviderFailureBecomesProviderErrorDespiteTheGlobalHandler() throws Exception {
        when(appExecutionFacade.executeAction(eq("crm"), eq("createRecord"), eq(null), anyMap()))
            .thenThrow(
                new ExecutionException(
                    "perform failed", new ProviderException("provider said no"), Map.of("apiToken", "s3cr3t"),
                    new AbstractErrorType(ActionDefinition.class, 104) {}));

        mockMvc.perform(
            post("/api/app/v1/connections/crm/actions/createRecord")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"input\":{}}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PROVIDER_ERROR"))
            .andExpect(jsonPath("$.message").value("provider said no"))
            .andExpect(content().string(not(containsString("s3cr3t"))));
    }

    @Test
    void testToolExecutionFailureBecomesUnprocessableDespiteTheGlobalHandler() throws Exception {
        when(appExecutionFacade.executeTool(eq("crm"), eq("sendMessage"), anyMap()))
            .thenThrow(
                new ExecutionException(
                    "tool failed", Map.of("apiToken", "s3cr3t"),
                    new AbstractErrorType(ClusterElementDefinition.class, 104) {}));

        mockMvc.perform(
            post("/api/app/v1/connections/crm/tools/sendMessage")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"input\":{}}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("COMPONENT_ERROR"))
            .andExpect(jsonPath("$.message").value("tool failed"))
            .andExpect(content().string(not(containsString("s3cr3t"))));
    }

    @Test
    void testExecutionTimeoutBecomesGatewayTimeoutDespiteTheGlobalHandler() throws Exception {
        when(appExecutionFacade.executeAction(eq("crm"), eq("slow"), eq(null), anyMap()))
            .thenThrow(new ConfigurationException("timed out", AppErrorType.APP_EXECUTION_TIMEOUT));

        mockMvc.perform(
            post("/api/app/v1/connections/crm/actions/slow")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"input\":{}}"))
            .andExpect(status().isGatewayTimeout())
            .andExpect(jsonPath("$.code").value("APP_EXECUTION_TIMEOUT"));
    }

    @Test
    void testProxyUpstreamUnavailableBecomesBadGatewayDespiteTheGlobalHandler() throws Exception {
        when(appExecutionFacade.proxy(eq("crm"), any(ProxyRequest.class)))
            .thenThrow(
                new ConfigurationException("The upstream is unavailable", AppErrorType.APP_PROXY_UPSTREAM_UNAVAILABLE));

        mockMvc.perform(
            post("/api/app/v1/connections/crm/proxy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"method\":\"GET\",\"path\":\"/items\"}"))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.code").value("APP_PROXY_UPSTREAM_UNAVAILABLE"));
    }

    @Test
    void testWorkflowRunNotFoundBecomesNotFoundDespiteTheGlobalHandler() throws Exception {
        when(appWorkflowExecutionFacade.getWorkflowRun("missing"))
            .thenThrow(new ConfigurationException("not found", AppErrorType.APP_WORKFLOW_RUN_NOT_FOUND));

        mockMvc.perform(get("/api/app/v1/workflow-runs/missing"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("APP_WORKFLOW_RUN_NOT_FOUND"));
    }

    @Test
    void testWorkflowInputInvalidBecomesBadRequestWithCodeDespiteTheGlobalHandler() throws Exception {
        when(appWorkflowExecutionFacade.executeWorkflow(eq("order"), anyMap(), eq(null)))
            .thenThrow(new ConfigurationException("invalid input", AppErrorType.APP_WORKFLOW_INPUT_INVALID));

        mockMvc.perform(
            post("/api/app/v1/workflows/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"input\":{}}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("APP_WORKFLOW_INPUT_INVALID"));
    }
}
