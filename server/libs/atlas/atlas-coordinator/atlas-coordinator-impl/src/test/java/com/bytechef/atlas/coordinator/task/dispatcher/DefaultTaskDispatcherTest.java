
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

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.message.broker.TaskMessageRoute;
import com.bytechef.atlas.configuration.task.WorkflowTask;
import java.util.List;
import java.util.Map;

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
            (k, m) -> Assertions.assertEquals(TaskMessageRoute.TASKS, k), List.of());

        defaultTaskDispatcher.dispatch(
            TaskExecution.builder()
                .workflowTask(
                    WorkflowTask.of(
                        Map.of(WorkflowConstants.NAME, "name", WorkflowConstants.TYPE, "type", "test", "value")))
                .build());
    }

    @Test
    public void test2() {
        TaskExecution taskExecution =
            TaskExecution.builder()
                .workflowTask(
                    WorkflowTask.of(
                        Map.of(WorkflowConstants.NAME, "name", WorkflowConstants.TYPE, "test", "node", "encoder")))
                .build();

        DefaultTaskDispatcher defaultTaskDispatcher = new DefaultTaskDispatcher(
            (k, m) -> Assertions.assertEquals(TaskMessageRoute.ofWorkerRoute("encoder"), k), List.of());

        defaultTaskDispatcher.dispatch(taskExecution);
    }

    @Test
    public void test3() {
        TaskExecution taskExecution =
            TaskExecution.builder()
                .workflowTask(
                    WorkflowTask.of(
                        Map.of(
                            WorkflowConstants.NAME, "name", WorkflowConstants.TYPE, "test", "node", "encoder.xlarge")))
                .build();

        DefaultTaskDispatcher defaultTaskDispatcher = new DefaultTaskDispatcher(
            (k, m) -> Assertions.assertEquals(TaskMessageRoute.ofWorkerRoute("encoder.xlarge"), k), List.of());

        defaultTaskDispatcher.dispatch(taskExecution);
    }
}
