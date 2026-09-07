/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.scheduler.job;

import static com.bytechef.platform.scheduler.constant.QuartzTriggerSchedulerConstants.CONNECTION_ID;
import static com.bytechef.platform.scheduler.constant.QuartzTriggerSchedulerConstants.DYNAMIC_WEBHOOK_TRIGGER_REFRESH;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.service.TriggerStateService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
public class DynamicWebhookTriggerRefreshJobTest {

    private static final String WORKFLOW_ID = "workflow1";
    private static final Map<String, ?> WEBHOOK_PARAMETERS = Map.of("subscriptionId", "subscription-1");

    private final JobPrincipalAccessor jobPrincipalAccessor = mock(JobPrincipalAccessor.class);
    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry = mock(JobPrincipalAccessorRegistry.class);
    private final JobExecutionContext jobExecutionContext = mock(JobExecutionContext.class);
    private final Scheduler scheduler = mock(Scheduler.class);
    private final TriggerDefinitionFacade triggerDefinitionFacade = mock(TriggerDefinitionFacade.class);
    private final TriggerStateService triggerStateService = mock(TriggerStateService.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);

    private final WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
        PlatformType.AUTOMATION, 123L, "test-webhook-workflow", "testTrigger");

    private DynamicWebhookTriggerRefreshJob dynamicWebhookTriggerRefreshJob;

    @BeforeEach
    public void setUp() {
        dynamicWebhookTriggerRefreshJob = new DynamicWebhookTriggerRefreshJob();

        dynamicWebhookTriggerRefreshJob.setPrincipalAccessorRegistry(jobPrincipalAccessorRegistry);
        dynamicWebhookTriggerRefreshJob.setRemoteTriggerDefinitionFacade(triggerDefinitionFacade);
        dynamicWebhookTriggerRefreshJob.setTriggerStateService(triggerStateService);
        dynamicWebhookTriggerRefreshJob.setWorkflowService(workflowService);

        Workflow workflow = new Workflow(
            WORKFLOW_ID,
            """
                {
                    "triggers": [
                        {
                            "name": "testTrigger",
                            "type": "testComponent/v1/testTriggerOperation"
                        }
                    ]
                }
                """,
            Format.JSON);

        when(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(any())).thenReturn(jobPrincipalAccessor);
        when(jobPrincipalAccessor.getWorkflowId(anyLong(), anyString())).thenReturn(WORKFLOW_ID);
        when(workflowService.getWorkflow(WORKFLOW_ID)).thenReturn(workflow);
        when(triggerStateService.<WebhookEnableOutput>fetchValue(any()))
            .thenReturn(Optional.of(new WebhookEnableOutput(WEBHOOK_PARAMETERS, null)));
        when(jobExecutionContext.getScheduler()).thenReturn(scheduler);
    }

    @Test
    public void testExecuteRefreshesAndRequeuesUsingTheSameTriggerKey() throws Exception {
        // Given
        Instant webhookExpirationDate = Instant.now()
            .plus(1, ChronoUnit.HOURS);

        when(triggerDefinitionFacade.executeDynamicWebhookRefresh(
            "testComponent", 1, "testTriggerOperation", WEBHOOK_PARAMETERS, 456L))
                .thenReturn(new WebhookEnableOutput(WEBHOOK_PARAMETERS, webhookExpirationDate));

        when(jobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap(456L));

        // When
        dynamicWebhookTriggerRefreshJob.execute(jobExecutionContext);

        // Then
        ArgumentCaptor<TriggerKey> triggerKeyArgumentCaptor = ArgumentCaptor.forClass(TriggerKey.class);
        ArgumentCaptor<Trigger> triggerArgumentCaptor = ArgumentCaptor.forClass(Trigger.class);

        verify(scheduler).rescheduleJob(triggerKeyArgumentCaptor.capture(), triggerArgumentCaptor.capture());

        TriggerKey triggerKey = triggerKeyArgumentCaptor.getValue();

        Assertions.assertEquals(workflowExecutionId.toString(), triggerKey.getName());
        Assertions.assertEquals(DYNAMIC_WEBHOOK_TRIGGER_REFRESH, triggerKey.getGroup());

        Trigger trigger = triggerArgumentCaptor.getValue();

        Assertions.assertEquals(Date.from(webhookExpirationDate), trigger.getStartTime());

        verify(triggerStateService).save(any(), any());
    }

    @Test
    public void testExecuteWithoutConnection() throws Exception {
        // Given
        when(triggerDefinitionFacade.executeDynamicWebhookRefresh(
            eq("testComponent"), eq(1), eq("testTriggerOperation"), any(), isNull()))
                .thenReturn(new WebhookEnableOutput(WEBHOOK_PARAMETERS, Instant.now()
                    .plus(1, ChronoUnit.HOURS)));

        when(jobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap(null));

        // When
        dynamicWebhookTriggerRefreshJob.execute(jobExecutionContext);

        // Then
        verify(triggerDefinitionFacade).executeDynamicWebhookRefresh(
            eq("testComponent"), eq(1), eq("testTriggerOperation"), any(), isNull());
        verify(scheduler).rescheduleJob(any(), any());
    }

    @Test
    public void testExecuteWithoutWebhookExpirationDate() throws Exception {
        // Given
        when(triggerDefinitionFacade.executeDynamicWebhookRefresh(
            "testComponent", 1, "testTriggerOperation", WEBHOOK_PARAMETERS, 456L))
                .thenReturn(new WebhookEnableOutput(WEBHOOK_PARAMETERS, null));

        when(jobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap(456L));

        // When
        dynamicWebhookTriggerRefreshJob.execute(jobExecutionContext);

        // Then
        verify(scheduler, never()).rescheduleJob(any(), any());
    }

    private JobDataMap jobDataMap(Long connectionId) {
        JobDataMap jobDataMap = new JobDataMap();

        jobDataMap.put("workflowExecutionId", workflowExecutionId.toString());
        jobDataMap.put(CONNECTION_ID, connectionId);

        return jobDataMap;
    }
}
