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

package com.bytechef.platform.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * Exposes TaskDispatcherDefinition over GraphQL.
 *
 * @author ByteChef
 */
@Controller
@ConditionalOnCoordinator
public class TaskDispatcherDefinitionGraphQlController {

    private final TaskDispatcherDefinitionService taskDispatcherDefinitionService;

    public TaskDispatcherDefinitionGraphQlController(TaskDispatcherDefinitionService taskDispatcherDefinitionService) {
        this.taskDispatcherDefinitionService = taskDispatcherDefinitionService;
    }

    @QueryMapping
    public TaskDispatcherDefinition taskDispatcherDefinition(@Argument String name, @Argument Integer version) {
        return taskDispatcherDefinitionService.getTaskDispatcherDefinition(name, version);
    }

    @QueryMapping
    public List<TaskDispatcherDefinition> taskDispatcherDefinitions() {
        return taskDispatcherDefinitionService.getTaskDispatcherDefinitions();
    }

    @QueryMapping
    public List<TaskDispatcherDefinition> taskDispatcherDefinitionVersions(@Argument String name) {
        return taskDispatcherDefinitionService.getTaskDispatcherDefinitionVersions(name);
    }
}
