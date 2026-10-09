/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.web.rest.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.tenant.TenantContext;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@WebMvcTest(RemoteTriggerLifecycleFacadeController.class)
@ContextConfiguration(classes = RemoteTriggerLifecycleFacadeControllerIntTest.TestConfiguration.class)
class RemoteTriggerLifecycleFacadeControllerIntTest {

    private static final String TRIGGER_LIFECYCLE_FACADE = "/remote/trigger-lifecycle-facade";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TriggerLifecycleFacade triggerLifecycleFacade;

    private WorkflowExecutionId workflowExecutionId;

    @BeforeEach
    void beforeEach() {
        TenantContext.setCurrentTenantId("public");

        workflowExecutionId = WorkflowExecutionId.of(PlatformType.AUTOMATION, 7L, "workflow-uuid", "trigger_1");
    }

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testExecuteTriggerDisableReadsTheBodyAndAcceptsAMissingConnection() throws Exception {
        mockMvc.perform(
            post(TRIGGER_LIFECYCLE_FACADE + "/execute-trigger-disable")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {"workflowId":"workflow-1","workflowExecutionId":"WORKFLOW_EXECUTION_ID",
                        "triggerWorkflowNodeType":{"name":"webhook","version":1,"operation":"trigger"},
                        "triggerParameters":{"path":"/hook"},"connectionId":null,"webhookUrl":null,
                        "environmentId":-1}"""
                        .replace("WORKFLOW_EXECUTION_ID", workflowExecutionId.toString())))
            .andExpect(status().isOk());

        ArgumentCaptor<WorkflowExecutionId> workflowExecutionIdCaptor = ArgumentCaptor.forClass(
            WorkflowExecutionId.class);

        verify(triggerLifecycleFacade).executeTriggerDisable(
            eq("workflow-1"), workflowExecutionIdCaptor.capture(),
            eq(new WorkflowNodeType("webhook", 1, "trigger")), eq(Map.of("path", "/hook")), isNull());
        verifyNoMoreInteractions(triggerLifecycleFacade);

        assertThat(workflowExecutionIdCaptor.getValue()).hasToString(workflowExecutionId.toString());
    }

    @Test
    void testExecuteTriggerEnableReadsTheBody() throws Exception {
        mockMvc.perform(
            post(TRIGGER_LIFECYCLE_FACADE + "/execute-trigger-enable")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                        {"workflowId":"workflow-1","workflowExecutionId":"WORKFLOW_EXECUTION_ID",
                        "triggerWorkflowNodeType":{"name":"webhook","version":1,"operation":"trigger"},
                        "triggerParameters":{},"connectionId":42,"webhookUrl":"https://hooks.example/1",
                        "environmentId":1}"""
                        .replace("WORKFLOW_EXECUTION_ID", workflowExecutionId.toString())))
            .andExpect(status().isOk());

        verify(triggerLifecycleFacade).executeTriggerEnable(
            eq("workflow-1"), any(WorkflowExecutionId.class), eq(new WorkflowNodeType("webhook", 1, "trigger")),
            eq(Map.of()), eq(42L), eq("https://hooks.example/1"), eq(1L));
        verifyNoMoreInteractions(triggerLifecycleFacade);
    }

    @Configuration
    @Import(RemoteTriggerLifecycleFacadeController.class)
    static class TestConfiguration {
    }
}
