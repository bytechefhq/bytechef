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

package com.bytechef.component.ai.agent.utils.cluster.subagent;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.tenant.TenantContext;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agent.tools.task.repository.BackgroundTask;

/**
 * @author Ivica Cardic
 */
class ByteChefTaskRepositoryTest {

    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    @AfterEach
    void tearDown() {
        executorService.shutdownNow();
    }

    @Test
    void testUnknownTaskIdYieldsAnActionableResultInsteadOfNull() {
        ByteChefTaskRepository taskRepository = new ByteChefTaskRepository(executorService);

        BackgroundTask backgroundTask = taskRepository.getTasks("never-started");

        assertThat(backgroundTask).isNotNull();
        assertThat(backgroundTask.isCompleted()).isTrue();
        assertThat(backgroundTask.getResult()).contains("never-started");
        assertThat(backgroundTask.getResult()).containsIgnoringCase("re-issue");
    }

    @Test
    void testStartedTaskIsReturnedAndCompletes() throws Exception {
        ByteChefTaskRepository taskRepository = new ByteChefTaskRepository(executorService);

        BackgroundTask backgroundTask = taskRepository.putTask("task-1", () -> "done");

        assertThat(backgroundTask.waitForCompletion(5000L)).isTrue();

        assertThat(taskRepository.getTasks("task-1")
            .getResult()).isEqualTo("done");
    }

    @Test
    void testTenantContextIsCarriedOntoTheBackgroundThread() throws Exception {
        ByteChefTaskRepository taskRepository = new ByteChefTaskRepository(executorService);

        AtomicReference<String> backgroundTenantId = new AtomicReference<>();

        BackgroundTask backgroundTask = TenantContext.callWithTenantId(
            "bytechef_000042",
            () -> taskRepository.putTask("task-1", () -> {
                backgroundTenantId.set(TenantContext.getCurrentTenantId());

                return "done";
            }));

        assertThat(backgroundTask.waitForCompletion(5000L)).isTrue();

        assertThat(backgroundTenantId.get()).isEqualTo("bytechef_000042");
    }

    @Test
    void testRemovedTaskFallsBackToTheActionableResult() {
        ByteChefTaskRepository taskRepository = new ByteChefTaskRepository(executorService);

        taskRepository.putTask("task-1", () -> "done");
        taskRepository.removeTask("task-1");

        assertThat(taskRepository.getTasks("task-1")
            .getResult()).containsIgnoringCase("re-issue");
    }
}
