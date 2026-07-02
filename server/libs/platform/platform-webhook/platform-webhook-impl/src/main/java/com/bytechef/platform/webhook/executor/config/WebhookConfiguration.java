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

package com.bytechef.platform.webhook.executor.config;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.webhook.executor.SseStreamBridgeRegistry;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutorImpl;
import com.bytechef.platform.webhook.executor.WebhookWorkflowSyncExecutor;
import com.bytechef.platform.workflow.execution.JobCompletionAwaiter;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class WebhookConfiguration {

    @Bean
    SseStreamBridgeRegistry sseStreamBridgeRegistry() {
        return new SseStreamBridgeRegistry();
    }

    @Bean
    WebhookWorkflowExecutor webhookExecutor(
        ApplicationEventPublisher eventPublisher, JobCompletionAwaiter jobCompletionAwaiter,
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry, PrincipalJobFacade principalJobFacade,
        SseStreamBridgeRegistry sseStreamBridgeRegistry, TaskExecutionService taskExecutionService,
        TaskFileStorage durableTaskFileStorage, TriggerDefinitionService triggerDefinitionService,
        WebhookWorkflowSyncExecutor triggerSyncExecutor, WorkflowService workflowService) {

        return new WebhookWorkflowExecutorImpl(
            eventPublisher, jobCompletionAwaiter, jobPrincipalAccessorRegistry, principalJobFacade,
            sseStreamBridgeRegistry, taskExecutionService, durableTaskFileStorage, triggerDefinitionService,
            triggerSyncExecutor, workflowService, JobCompletionAwaiter.DEFAULT_SYNC_TIMEOUT);
    }
}
