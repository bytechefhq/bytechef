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

import com.bytechef.tenant.TenantContext;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import org.springaicommunity.agent.tools.task.repository.BackgroundTask;
import org.springaicommunity.agent.tools.task.repository.DefaultTaskRepository;
import org.springaicommunity.agent.tools.task.repository.TaskRepository;

/**
 * Task repository for background subagent runs. Wraps the library's in-memory repository and changes two things.
 *
 * <p>
 * <b>An unknown task id yields an actionable result instead of {@code null}.</b> Background tasks are in-process and
 * deliberately not durable: a run interrupted by a crash restarts from the beginning. On resume the agent's
 * conversation is restored from its checkpoint, so it may still hold ids whose futures died with the process. The task
 * output tool calls {@code isCompleted()} and {@code getResult()} straight on whatever this returns, so handing back
 * {@code null} would surface as a {@code NullPointerException} rather than something the model can act on. The same
 * applies to an id the model simply invented.
 *
 * <p>
 * <b>The tenant is carried onto the background thread.</b> Tenant id is a thread local that selects the database
 * schema, and a task submitted to the executor would otherwise run without it — so any tool the subagent calls would
 * read the wrong schema, or none. It is captured on the submitting thread and reinstated around the task body.
 *
 * @author Ivica Cardic
 */
public class ByteChefTaskRepository implements TaskRepository {

    private final DefaultTaskRepository defaultTaskRepository;

    public ByteChefTaskRepository(ExecutorService executorService) {
        // ownsExecutor = false: the executor is shared across runs and outlives any single repository, so this
        // repository must never shut it down.
        this.defaultTaskRepository = new DefaultTaskRepository(executorService, false);
    }

    @Override
    public void clear() {
        defaultTaskRepository.clear();
    }

    @Override
    public BackgroundTask getTasks(String taskId) {
        BackgroundTask backgroundTask = defaultTaskRepository.getTasks(taskId);

        if (backgroundTask == null) {
            return lostTask(taskId);
        }

        return backgroundTask;
    }

    @Override
    public BackgroundTask putTask(String taskId, Supplier<String> supplier) {
        String tenantId = TenantContext.getCurrentTenantId();

        return defaultTaskRepository.putTask(taskId, () -> TenantContext.callWithTenantId(tenantId, supplier::get));
    }

    @Override
    public void removeTask(String taskId) {
        defaultTaskRepository.removeTask(taskId);
    }

    private static BackgroundTask lostTask(String taskId) {
        return new BackgroundTask(
            taskId,
            CompletableFuture.completedFuture(
                "Task '%s' is no longer available. Background tasks do not survive a restart, so re-issue the task if you still need its result."
                    .formatted(taskId)));
    }
}
