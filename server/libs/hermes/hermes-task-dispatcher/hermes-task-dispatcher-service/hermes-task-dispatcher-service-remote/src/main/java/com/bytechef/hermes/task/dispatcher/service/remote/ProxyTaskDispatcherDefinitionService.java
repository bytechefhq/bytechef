
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

package com.bytechef.hermes.task.dispatcher.service.remote;

import com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDefinition;
import com.bytechef.hermes.task.dispatcher.service.TaskDispatcherDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

/**
 * @author Ivica Cardic
 */
@Component
public class ProxyTaskDispatcherDefinitionService implements TaskDispatcherDefinitionService {

    private final WebClient.Builder builder;

    @SuppressFBWarnings("EI2")
    public ProxyTaskDispatcherDefinitionService(WebClient.Builder builder) {
        this.builder = builder;
    }

    @Override
    public Flux<TaskDispatcherDefinition> getTaskDispatcherDefinitions() {
        return builder.build()
            .get()
            .uri("http://coordinator-service-app/api/definitions/task-dispatchers")
            .header("Content-Type", "application/json")
            .retrieve()
            .bodyToFlux(TaskDispatcherDefinition.class);
    }
}
