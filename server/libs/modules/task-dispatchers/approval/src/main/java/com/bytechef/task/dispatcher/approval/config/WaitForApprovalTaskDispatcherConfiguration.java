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

package com.bytechef.task.dispatcher.approval.config;

import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.task.dispatcher.approval.WaitForApprovalTaskDispatcher;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Deprecated
@Configuration
public class WaitForApprovalTaskDispatcherConfiguration {

    private final ApplicationEventPublisher eventPublisher;
    private final JobService jobService;
    private final TaskExecutionService taskExecutionService;

    @SuppressFBWarnings("EI")
    public WaitForApprovalTaskDispatcherConfiguration(
        ApplicationEventPublisher eventPublisher, JobService jobService, TaskExecutionService taskExecutionService) {

        this.eventPublisher = eventPublisher;
        this.jobService = jobService;
        this.taskExecutionService = taskExecutionService;
    }

    @Bean("waitForApprovalTaskDispatcherResolverFactory_v1")
    TaskDispatcherResolverFactory waitForApprovalTaskDispatcherResolverFactory() {
        return (taskDispatcher) -> new WaitForApprovalTaskDispatcher(eventPublisher, jobService, taskExecutionService);
    }
}
