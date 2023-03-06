
/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.atlas.coordinator.task.dispatcher;

import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.task.WorkflowTask;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
public class DefaultTaskDispatcherTest {

    @Test
    public void test1() {
        DefaultTaskDispatcher defaultTaskDispatcher = new DefaultTaskDispatcher(
            (k, m) -> Assertions.assertEquals(Queues.TASKS, k), List.of());

        defaultTaskDispatcher.dispatch(new TaskExecution(WorkflowTask.of("type", "test", "value")));
    }

    @Test
    public void test2() {
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("test", "node", "encoder"));

        DefaultTaskDispatcher defaultTaskDispatcher = new DefaultTaskDispatcher(
            (k, m) -> Assertions.assertEquals("encoder", k), List.of());

        defaultTaskDispatcher.dispatch(taskExecution);
    }

    @Test
    public void test3() {
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("test", "node", "encoder.xlarge"));

        DefaultTaskDispatcher defaultTaskDispatcher = new DefaultTaskDispatcher(
            (k, m) -> Assertions.assertEquals("encoder.xlarge", k), List.of());

        defaultTaskDispatcher.dispatch(taskExecution);
    }
}
