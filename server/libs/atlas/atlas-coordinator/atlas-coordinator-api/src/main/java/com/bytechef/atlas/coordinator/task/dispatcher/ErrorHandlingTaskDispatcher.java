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

package com.bytechef.atlas.coordinator.task.dispatcher;

import com.bytechef.atlas.coordinator.event.TaskExecutionErrorEvent;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.error.ExecutionError;
import java.util.Arrays;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Matija Petanjek
 */
public abstract class ErrorHandlingTaskDispatcher implements TaskDispatcher<TaskExecution> {

    private final ApplicationEventPublisher eventPublisher;

    public ErrorHandlingTaskDispatcher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void dispatch(TaskExecution taskExecution) {
        try {
            doDispatch(taskExecution);
        } catch (Exception exception) {
            taskExecution.setError(
                new ExecutionError(exception.getMessage(), Arrays.asList(ExceptionUtils.getStackFrames(exception))));

            eventPublisher.publishEvent(new TaskExecutionErrorEvent(taskExecution));
        }
    }

    public abstract void doDispatch(TaskExecution taskExecution);
}
