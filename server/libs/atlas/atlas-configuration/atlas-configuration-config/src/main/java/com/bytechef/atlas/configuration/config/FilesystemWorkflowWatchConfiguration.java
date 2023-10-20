
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.atlas.configuration.config;

import com.bytechef.atlas.configuration.filesystem.FilesystemWorkflowWatcher;
import com.bytechef.atlas.configuration.service.WorkflowService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

import java.io.IOException;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnExpression("""
    '${bytechef.coordinator.enabled:true}' == 'true' and
    '${bytechef.workflow.repository.filesystem.enabled}' == 'true' and
    '${bytechef.workflow.repository.filesystem.watch}' == 'true'
    """)
public class FilesystemWorkflowWatchConfiguration {

    @Bean
    FilesystemWorkflowWatcher filesystemWorkflowWatcher(
        @Value("${bytechef.workflow.repository.filesystem.projects.location-pattern:}") String locationPattern,
        TaskExecutor taskExecutor, WorkflowService workflowService) throws IOException {

        return new FilesystemWorkflowWatcher(locationPattern, taskExecutor, workflowService);
    }
}
