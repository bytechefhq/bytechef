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

package com.bytechef.platform.notification.delivery.config;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;

/**
 * @author Ivica Cardic
 */
public class RestartableAsyncTaskExecutor implements AsyncTaskExecutor, AutoCloseable {

    private volatile SimpleAsyncTaskExecutor delegate = createDelegate();

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public void execute(Runnable task) {
        delegate.execute(task);
    }

    public void restart() {
        delegate.close();

        delegate = createDelegate();
    }

    public void setTaskDecorator(TaskDecorator taskDecorator) {
        delegate.setTaskDecorator(taskDecorator);
    }

    private static SimpleAsyncTaskExecutor createDelegate() {
        SimpleAsyncTaskExecutor simpleAsyncTaskExecutor = new SimpleAsyncTaskExecutor("webhook-delivery-test-");

        simpleAsyncTaskExecutor.setCancelRemainingTasksOnClose(true);
        simpleAsyncTaskExecutor.setVirtualThreads(true);

        return simpleAsyncTaskExecutor;
    }
}
