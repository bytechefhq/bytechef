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

package com.bytechef.platform.worker.task;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.task.handler.TaskExecutionPostOutputProcessor;
import com.bytechef.component.definition.ActionDefinition.WebSocketHandler;
import org.jspecify.annotations.Nullable;

/**
 * Fails a workflow task whose action is a realtime (WebSocket) action.
 *
 * <p>
 * A realtime action returns a {@link WebSocketHandler} rather than a value: it expects to be attached to a live
 * WebSocket session and to keep running until the caller disconnects. That only happens inside a voice session, which
 * {@code VoiceSessionEngine} runs outside the task engine entirely. Dropped into an ordinary workflow there is no
 * session to attach to, so the handler would sit there with nothing feeding it.
 *
 * <p>
 * This used to be a processor that blocked the task until the emitter completed — which, with nothing able to complete
 * it, meant the task occupied a worker thread until the JVM restarted. Failing immediately with an explanation is the
 * honest answer: the workflow is misconfigured, and a hung job says nothing about why.
 *
 * @author Ivica Cardic
 */
public class RealtimeActionTaskExecutionPostOutputProcessor implements TaskExecutionPostOutputProcessor {

    @Override
    public @Nullable Object process(TaskExecution taskExecution, Object output) {
        if (!(output instanceof WebSocketHandler)) {
            return output;
        }

        throw new IllegalStateException(
            describe(taskExecution) + " is a realtime action and cannot run as a workflow task. Realtime actions " +
                "run only inside a voice session, declared in the websocketTasks pipeline on a voice trigger.");
    }

    /**
     * Names the task for the error message, defensively: this runs on a failure path, and a message-building NPE would
     * replace a clear explanation with a confusing one.
     */
    private static String describe(TaskExecution taskExecution) {
        try {
            return "Task '" + taskExecution.getName() + "' (" + taskExecution.getType() + ")";
        } catch (RuntimeException runtimeException) {
            return "This task";
        }
    }
}
