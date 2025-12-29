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

package com.bytechef.platform.workflow.task.dispatcher.service;

import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface TaskDispatcherDefinitionService {

    @Nullable
    OutputResponse executeOutput(String name, int version, Map<String, ?> inputParameters);

    @Nullable
    OutputResponse executeVariableProperties(String name, int version, Map<String, ?> inputParameters);

    String executeWorkflowNodeDescription(String name, int version, Map<String, ?> inputParameters);

    Optional<TaskDispatcherDefinition> fetchTaskDispatcherDefinition(String name, @Nullable Integer version);

    TaskDispatcherDefinition getTaskDispatcherDefinition(String name, @Nullable Integer version);

    List<TaskDispatcherDefinition> getTaskDispatcherDefinitions();

    List<TaskDispatcherDefinition> getTaskDispatcherDefinitionVersions(String name);

    boolean isDynamicOutputDefined(String componentName, int componentVersion);
}
