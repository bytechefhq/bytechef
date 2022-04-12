/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.integri.atlas.task.dispatcher.sequence;

import com.integri.atlas.engine.coordinator.job.Job;
import com.integri.atlas.engine.coordinator.task.completion.TaskCompletionHandler;
import com.integri.atlas.engine.coordinator.workflow.repository.WorkflowMapper;
import com.integri.atlas.engine.coordinator.workflow.repository.YAMLWorkflowMapper;
import com.integri.atlas.engine.core.context.Context;
import com.integri.atlas.engine.core.message.broker.MessageBroker;
import com.integri.atlas.engine.core.task.dispatcher.TaskDispatcher;
import com.integri.atlas.engine.core.task.dispatcher.TaskDispatcherResolver;
import com.integri.atlas.engine.core.task.evaluator.spel.SpelTaskEvaluator;
import com.integri.atlas.engine.worker.task.handler.TaskHandler;
import com.integri.atlas.task.dispatcher.sequence.completion.SequenceTaskCompletionHandler;
import com.integri.atlas.task.handler.BaseTaskIntTest;
import com.integri.atlas.task.handler.core.Var;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Ivica Cardic
 */
@SpringBootTest
public class SequenceTaskDispatcherIntTest extends BaseTaskIntTest {

    @Test
    public void testSequence() {
        Job job = startJob("samples/sequence.yaml", Map.of());

        Context context = contextRepository.peek(job.getId());

        Assertions.assertEquals(3, (Integer) context.get("value3"));
        Assertions.assertEquals(3, (Integer) context.get("sumSequence"));
    }

    @Override
    protected List<TaskCompletionHandler> getTaskCompletionHandlers(
        TaskCompletionHandler taskCompletionHandler,
        TaskDispatcher taskDispatcher
    ) {
        return List.of(
            new SequenceTaskCompletionHandler(
                taskExecutionRepository,
                taskCompletionHandler,
                taskDispatcher,
                contextRepository,
                SpelTaskEvaluator.create()
            )
        );
    }

    @Override
    protected List<TaskDispatcherResolver> getTaskDispatcherResolvers(
        MessageBroker coordinatorMessageBroker,
        TaskDispatcher taskDispatcher
    ) {
        return List.of(
            new SequenceTaskDispatcher(
                contextRepository,
                coordinatorMessageBroker,
                taskDispatcher,
                taskExecutionRepository,
                SpelTaskEvaluator.create()
            )
        );
    }

    @Override
    protected Map<String, TaskHandler<?>> getTaskHandlerResolverMap() {
        return Map.of("core/var", new Var());
    }

    @Override
    protected WorkflowMapper getWorkflowMapper() {
        return new YAMLWorkflowMapper();
    }
}
