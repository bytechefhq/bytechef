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

import com.bytechef.ee.automation.app.dto.AppWorkflowDTO;
import com.bytechef.ee.automation.app.dto.AppWorkflowRunDTO;
import com.bytechef.ee.automation.app.dto.AppWorkflowRunStatus;
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
@WebMvcTest(AppWorkflowApiController.class)
@WithMockUser
class AppWorkflowApiControllerIntTest {

    @MockitoBean
    private AppExecutionFacade appExecutionFacade;

    @MockitoBean
    private AppWorkflowExecutionFacade appWorkflowExecutionFacade;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testExecuteWorkflowThatFinishedIsOk() throws Exception {
        when(appWorkflowExecutionFacade.executeWorkflow("order", Map.of("orderId", "A-1"), 5))
            .thenReturn(
                new AppWorkflowRunDTO(
                    "run-1", "order", AppWorkflowRunStatus.COMPLETED, Map.of("approved", true), null, null));

        mockMvc.perform(
            post("/api/app/v1/workflows/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"input\":{\"orderId\":\"A-1\"},\"waitSeconds\":5}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.runId").value("run-1"))
            .andExpect(jsonPath("$.workflowKey").value("order"))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.result.approved").value(true));

        verify(appWorkflowExecutionFacade).executeWorkflow("order", Map.of("orderId", "A-1"), 5);
    }

    @Test
    void testExecuteWorkflowStillRunningIsAccepted() throws Exception {
        when(appWorkflowExecutionFacade.executeWorkflow(eq("order"), anyMap(), isNull()))
            .thenReturn(new AppWorkflowRunDTO("run-2", "order", AppWorkflowRunStatus.RUNNING, null, null, null));

        mockMvc.perform(
            post("/api/app/v1/workflows/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"input\":{}}"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.runId").value("run-2"))
            .andExpect(jsonPath("$.status").value("RUNNING"))
            .andExpect(jsonPath("$.result").value(nullValue()))
            .andExpect(jsonPath("$.error").value(nullValue()))
            .andExpect(jsonPath("$.approvalFormUrl").value(nullValue()));
    }

    @Test
    void testExecuteWorkflowWaitingOnApprovalIsAccepted() throws Exception {
        when(appWorkflowExecutionFacade.executeWorkflow(eq("order"), anyMap(), isNull()))
            .thenReturn(
                new AppWorkflowRunDTO(
                    "run-3", "order", AppWorkflowRunStatus.APPROVAL_REQUIRED, null, null,
                    "https://example.com/approvals/run-3"));

        mockMvc.perform(
            post("/api/app/v1/workflows/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"input\":{}}"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("APPROVAL_REQUIRED"))
            .andExpect(jsonPath("$.approvalFormUrl").value("https://example.com/approvals/run-3"));
    }

    @Test
    void testFailedRunCarriesTheErrorMessage() throws Exception {
        when(appWorkflowExecutionFacade.executeWorkflow(eq("order"), anyMap(), isNull()))
            .thenReturn(new AppWorkflowRunDTO("run-4", "order", AppWorkflowRunStatus.FAILED, null, "boom", null));

        mockMvc.perform(
            post("/api/app/v1/workflows/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"input\":{}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.error.message").value("boom"));
    }

    @Test
    void testOmittedWaitPassesNull() throws Exception {
        when(appWorkflowExecutionFacade.executeWorkflow(eq("order"), anyMap(), isNull()))
            .thenReturn(new AppWorkflowRunDTO("run-5", "order", AppWorkflowRunStatus.RUNNING, null, null, null));

        mockMvc.perform(
            post("/api/app/v1/workflows/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"input\":{}}"))
            .andExpect(status().isAccepted());

        verify(appWorkflowExecutionFacade).executeWorkflow("order", Map.of(), null);
    }

    @Test
    void testNegativeWaitReachesTheFacade() throws Exception {
        when(appWorkflowExecutionFacade.executeWorkflow("order", Map.of(), -1))
            .thenThrow(new ConfigurationException("invalid wait", AppErrorType.APP_WORKFLOW_INPUT_INVALID));

        mockMvc.perform(
            post("/api/app/v1/workflows/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"input\":{},\"waitSeconds\":-1}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("APP_WORKFLOW_INPUT_INVALID"));

        verify(appWorkflowExecutionFacade).executeWorkflow("order", Map.of(), -1);
    }

    @Test
    void testMissingInputIsBadRequest() throws Exception {
        mockMvc.perform(
            post("/api/app/v1/workflows/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void testUnavailableWorkflowIsConflict() throws Exception {
        when(appWorkflowExecutionFacade.executeWorkflow(eq("order"), anyMap(), isNull()))
            .thenThrow(new ConfigurationException("unavailable", AppErrorType.APP_WORKFLOW_UNAVAILABLE));

        mockMvc.perform(
            post("/api/app/v1/workflows/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"input\":{}}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("APP_WORKFLOW_UNAVAILABLE"));
    }

    @Test
    void testGetWorkflowRunReturnsTheRun() throws Exception {
        when(appWorkflowExecutionFacade.getWorkflowRun("run-1"))
            .thenReturn(
                new AppWorkflowRunDTO(
                    "run-1", "order", AppWorkflowRunStatus.COMPLETED, Map.of("approved", true), null, null));

        mockMvc.perform(get("/api/app/v1/workflow-runs/run-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.runId").value("run-1"))
            .andExpect(jsonPath("$.workflowKey").value("order"))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.result.approved").value(true));
    }

    @Test
    void testUnknownRunIsNotFound() throws Exception {
        when(appWorkflowExecutionFacade.getWorkflowRun("missing"))
            .thenThrow(new ConfigurationException("not found", AppErrorType.APP_WORKFLOW_RUN_NOT_FOUND));

        mockMvc.perform(get("/api/app/v1/workflow-runs/missing"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("APP_WORKFLOW_RUN_NOT_FOUND"));
    }

    @Test
    void testGetWorkflowsReturnsSchemasAsObjects() throws Exception {
        when(appWorkflowExecutionFacade.getWorkflows()).thenReturn(
            List.of(
                new AppWorkflowDTO(
                    "order", "Order", "Places an order", Map.of("type", "object"), Map.of("type", "object"), true)));

        mockMvc.perform(get("/api/app/v1/workflows"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].workflowKey").value("order"))
            .andExpect(jsonPath("$[0].name").value("Order"))
            .andExpect(jsonPath("$[0].description").value("Places an order"))
            .andExpect(jsonPath("$[0].inputSchema.type").value("object"))
            .andExpect(jsonPath("$[0].outputSchema.type").value("object"))
            .andExpect(jsonPath("$[0].available").value(true));
    }

    @Test
    void testGetWorkflowsListsAnUnavailableWorkflowWithNullSchemas() throws Exception {
        when(appWorkflowExecutionFacade.getWorkflows())
            .thenReturn(List.of(new AppWorkflowDTO("gone", "gone", null, null, null, false)));

        mockMvc.perform(get("/api/app/v1/workflows"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].workflowKey").value("gone"))
            .andExpect(jsonPath("$[0].available").value(false))
            .andExpect(jsonPath("$[0].description").value(nullValue()))
            .andExpect(jsonPath("$[0].inputSchema").value(nullValue()));
    }
}
