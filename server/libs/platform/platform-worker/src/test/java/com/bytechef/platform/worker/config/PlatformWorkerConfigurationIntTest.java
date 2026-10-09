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

package com.bytechef.platform.worker.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.atlas.worker.task.handler.TaskExecutionPostOutputProcessor;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.platform.worker.task.SseStreamTaskExecutionPostOutputProcessor;
import com.bytechef.platform.worker.task.SuspendTaskExecutionPostOutputProcessor;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Ivica Cardic
 */
@SpringJUnitConfig(PlatformWorkerConfiguration.class)
@MockitoBean(types = {
    MessageBroker.class, TriggerScheduler.class
})
class PlatformWorkerConfigurationIntTest {

    @Autowired
    private List<TaskExecutionPostOutputProcessor> taskExecutionPostOutputProcessors;

    @Test
    void testSseStreamProcessorRunsBeforeSuspendProcessor() {
        int sseStreamIndex = indexOf(SseStreamTaskExecutionPostOutputProcessor.class);
        int suspendIndex = indexOf(SuspendTaskExecutionPostOutputProcessor.class);

        assertTrue(sseStreamIndex >= 0, "The SSE stream processor must be registered");
        assertTrue(suspendIndex >= 0, "The suspend processor must be registered");
        assertTrue(
            sseStreamIndex < suspendIndex,
            "The SSE stream processor must run before the suspend processor so the Suspend it returns gets persisted");
    }

    private int indexOf(Class<?> processorClass) {
        for (int index = 0; index < taskExecutionPostOutputProcessors.size(); index++) {
            if (processorClass.isInstance(taskExecutionPostOutputProcessors.get(index))) {
                return index;
            }
        }

        return -1;
    }
}
