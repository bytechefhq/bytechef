/*
 * Copyright 2023-present ByteChef Inc.
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
import com.bytechef.atlas.configuration.repository.annotation.ConditionalOnWorkflowRepositoryFilesystem;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnCoordinator
@ConditionalOnWorkflowRepositoryFilesystem
@ConditionalOnProperty(prefix = "bytechef", name = "workflow.repository.filesystem.watch", havingValue = "true")
public class FilesystemWorkflowWatchConfiguration {

    @Bean
    FilesystemWorkflowWatcher filesystemWorkflowWatcher(
        @Value("${bytechef.workflow.repository.filesystem.location-pattern:${user.home}/bytechef/data/workflows/**/*.{json,yml,yaml}}") String locationPattern,
        TaskExecutor taskExecutor, WorkflowService workflowService) throws IOException {

        return new FilesystemWorkflowWatcher(locationPattern, taskExecutor, workflowService);
    }
}
