
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

package com.bytechef.hermes.definition.registry.service;

import com.bytechef.hermes.task.dispatcher.TaskDispatcherDefinitionFactory;
import com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 */
public class TaskDispatcherDefinitionServiceImpl implements TaskDispatcherDefinitionService {

    private final List<TaskDispatcherDefinitionFactory> taskDispatcherDefinitionFactories;

    @SuppressFBWarnings("EI2")
    public TaskDispatcherDefinitionServiceImpl(
        List<TaskDispatcherDefinitionFactory> taskDispatcherDefinitionFactories) {

        this.taskDispatcherDefinitionFactories = taskDispatcherDefinitionFactories;
    }

    @Override
    public Mono<List<TaskDispatcherDefinition>> getTaskDispatcherDefinitionsMono() {
        return Mono.just(
            taskDispatcherDefinitionFactories.stream()
                .map(TaskDispatcherDefinitionFactory::getDefinition)
                .collect(Collectors.toList()));
    }
}
