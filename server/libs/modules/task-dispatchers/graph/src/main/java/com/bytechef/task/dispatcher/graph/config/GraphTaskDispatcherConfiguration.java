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

package com.bytechef.task.dispatcher.graph.config;

import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.GRAPH;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.NODES;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.TRANSITIONS;

import com.bytechef.atlas.configuration.domain.DeferredEvaluationParameterKeys;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.task.dispatcher.graph.GraphTaskDispatcher;
import com.bytechef.task.dispatcher.graph.completion.GraphTaskCompletionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the {@code graph/v1} dispatcher and completion handler with the atlas coordinator, mirroring
 * {@code ConditionTaskDispatcherConfiguration} exactly. Both {@code nodes} and {@code transitions} are registered as
 * deferred-evaluation parameter keys so their nested expressions keep their original, unevaluated form until the owning
 * node is actually dispatched or its outgoing transitions are actually evaluated -- evaluating a transition's
 * {@code condition} or {@code to} eagerly (e.g. when the graph task itself is evaluated as somebody else's sub-task)
 * would resolve it against a context that doesn't yet contain the node outputs it references; the completion handler
 * evaluates each transition at transition time instead, once those outputs exist.
 *
 * @author Ivica Cardic
 */
@Configuration
public class GraphTaskDispatcherConfiguration {

    static {
        DeferredEvaluationParameterKeys.register(GRAPH + "/", NODES, TRANSITIONS);
    }

    @Autowired
    private ContextService contextService;

    @Autowired
    private CounterService counterService;

    @Autowired
    private Evaluator evaluator;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    private TaskFileStorage taskFileStorage;

    @Bean("graphTaskCompletionHandlerFactory_v1")
    TaskCompletionHandlerFactory graphTaskCompletionHandlerFactory() {
        return (taskCompletionHandler, taskDispatcher) -> new GraphTaskCompletionHandler(
            contextService, counterService, evaluator, taskCompletionHandler, taskDispatcher, taskExecutionService,
            taskFileStorage);
    }

    @Bean("graphTaskDispatcherResolverFactory_v1")
    TaskDispatcherResolverFactory graphTaskDispatcherResolverFactory() {
        return (taskDispatcher) -> new GraphTaskDispatcher(
            contextService, counterService, evaluator, eventPublisher, taskDispatcher, taskExecutionService,
            taskFileStorage);
    }
}
