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

package com.bytechef.platform.workflow.task.dispatcher.facade;

import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class TaskDispatcherDefinitionFacadeImpl implements TaskDispatcherDefinitionFacade {

    private final TaskDispatcherDefinitionService taskDispatcherDefinitionService;

    @SuppressFBWarnings("EI")
    public TaskDispatcherDefinitionFacadeImpl(TaskDispatcherDefinitionService taskDispatcherDefinitionService) {
        this.taskDispatcherDefinitionService = taskDispatcherDefinitionService;
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public OutputResponse executeOutput(String name, int version, Map<String, ?> inputParameters) {
        return taskDispatcherDefinitionService.executeOutput(name, version, inputParameters);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public TaskDispatcherDefinition getTaskDispatcherDefinition(String name, @Nullable Integer version) {
        return taskDispatcherDefinitionService.getTaskDispatcherDefinition(name, version);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public List<TaskDispatcherDefinition> getTaskDispatcherDefinitions() {
        return taskDispatcherDefinitionService.getTaskDispatcherDefinitions();
    }
}
