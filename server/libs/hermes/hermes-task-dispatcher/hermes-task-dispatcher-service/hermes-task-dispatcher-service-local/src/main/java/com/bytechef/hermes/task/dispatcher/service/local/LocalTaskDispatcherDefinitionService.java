
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

package com.bytechef.hermes.task.dispatcher.service.local;

import com.bytechef.hermes.task.dispatcher.TaskDispatcherDefinitionFactory;
import com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDefinition;
import com.bytechef.hermes.task.dispatcher.service.TaskDispatcherDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 */
@Component
public class LocalTaskDispatcherDefinitionService implements TaskDispatcherDefinitionService {

    private final List<TaskDispatcherDefinitionFactory> taskDispatcherDefinitionFactories;

    @SuppressFBWarnings("EI2")
    public LocalTaskDispatcherDefinitionService(
        List<TaskDispatcherDefinitionFactory> taskDispatcherDefinitionFactories) {
        this.taskDispatcherDefinitionFactories = taskDispatcherDefinitionFactories;
    }

    @Override
    public Flux<TaskDispatcherDefinition> getTaskDispatcherDefinitions() {
        return Flux.fromIterable(taskDispatcherDefinitionFactories.stream()
            .map(TaskDispatcherDefinitionFactory::getDefinition)
            .collect(Collectors.toList()));
    }
}
