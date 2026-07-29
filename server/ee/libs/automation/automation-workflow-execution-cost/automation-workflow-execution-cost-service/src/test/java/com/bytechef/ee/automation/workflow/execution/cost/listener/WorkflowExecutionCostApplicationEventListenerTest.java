/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.execution.cost.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.automation.workflow.execution.cost.config.WorkflowExecutionCostProperties;
import com.bytechef.ee.automation.workflow.execution.cost.domain.WorkflowExecutionCost;
import com.bytechef.ee.automation.workflow.execution.cost.service.WorkflowExecutionCostService;
import com.bytechef.ee.automation.workflow.execution.cost.service.WorkspaceWorkflowExecutionCostService;
import com.bytechef.ee.platform.ai.llm.usage.AiLlmUsage;
import com.bytechef.ee.platform.ai.llm.usage.LlmUsageSource;
import com.bytechef.ee.platform.ai.llm.usage.service.AiLlmUsageService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkflowExecutionCostApplicationEventListenerTest {

    @Mock
    private AiLlmUsageService aiLlmUsageService;

    @Mock
    private PrincipalJobService principalJobService;

    @Mock
    private ProjectDeploymentService projectDeploymentService;

    @Mock
    private ProjectService projectService;

    @Mock
    private WorkflowExecutionCostService workflowExecutionCostService;

    @Mock
    private WorkspaceWorkflowExecutionCostService workspaceWorkflowExecutionCostService;

    private WorkflowExecutionCostApplicationEventListener listener;

    @BeforeEach
    void beforeEach() {
        listener = new WorkflowExecutionCostApplicationEventListener(
            aiLlmUsageService, principalJobService, projectDeploymentService, projectService,
            new WorkflowExecutionCostProperties(true, new BigDecimal("0.005")),
            workflowExecutionCostService, workspaceWorkflowExecutionCostService);

        when(workflowExecutionCostService.fetchByJobId(anyLong())).thenReturn(Optional.empty());
        when(principalJobService.fetchJobPrincipalId(anyLong(), any(PlatformType.class)))
            .thenReturn(Optional.empty());
    }

    @Test
    void testTerminalStatusCreatesCostRowWithBaseChargePlusAiSum() {
        AiLlmUsage firstUsage = new AiLlmUsage("req-1", "gpt-4o");
        AiLlmUsage secondUsage = new AiLlmUsage("req-2", "gpt-4o");

        firstUsage.setCost(new BigDecimal("0.010000"));
        secondUsage.setCost(new BigDecimal("0.002500"));

        when(aiLlmUsageService.getUsagesByOwner(LlmUsageSource.AI_AGENT, 42L))
            .thenReturn(List.of(firstUsage, secondUsage));

        listener.onApplicationEvent(new JobStatusApplicationEvent(42L, Job.Status.COMPLETED));

        ArgumentCaptor<WorkflowExecutionCost> costCaptor = ArgumentCaptor.forClass(WorkflowExecutionCost.class);

        verify(workspaceWorkflowExecutionCostService).createInWorkspace(costCaptor.capture(), isNull());

        WorkflowExecutionCost cost = costCaptor.getValue();

        assertThat(cost.getJobId()).isEqualTo(42L);
        assertThat(cost.getBaseRunCharge()).isEqualByComparingTo(new BigDecimal("0.005"));
        assertThat(cost.getAiCost()).isEqualByComparingTo(new BigDecimal("0.012500"));
        assertThat(cost.getTotalCost()).isEqualByComparingTo(new BigDecimal("0.017500"));
    }

    @Test
    void testNonTerminalStatusIsIgnored() {
        listener.onApplicationEvent(new JobStatusApplicationEvent(42L, Job.Status.STARTED));

        verify(workspaceWorkflowExecutionCostService, never()).createInWorkspace(any(), any());
    }

    @Test
    void testExistingCostRowIsNotDuplicated() {
        when(workflowExecutionCostService.fetchByJobId(42L))
            .thenReturn(Optional.of(new WorkflowExecutionCost(42L, BigDecimal.ZERO, BigDecimal.ZERO)));

        listener.onApplicationEvent(new JobStatusApplicationEvent(42L, Job.Status.COMPLETED));

        verify(workspaceWorkflowExecutionCostService, never()).createInWorkspace(any(), any());
    }

    @Test
    void testCancelledJobGetsCostRow() {
        when(aiLlmUsageService.getUsagesByOwner(LlmUsageSource.AI_AGENT, 7L)).thenReturn(List.of());

        listener.onApplicationEvent(new JobStatusApplicationEvent(7L, Job.Status.CANCELLED));

        verify(workspaceWorkflowExecutionCostService).createInWorkspace(any(WorkflowExecutionCost.class), isNull());
    }
}
