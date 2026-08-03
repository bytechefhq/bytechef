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
 * @author Ivica Cardic
 */
public class TenantAwareTaskRepository implements TaskRepository {

    private final DefaultTaskRepository defaultTaskRepository;

    public TenantAwareTaskRepository(ExecutorService executorService) {
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
