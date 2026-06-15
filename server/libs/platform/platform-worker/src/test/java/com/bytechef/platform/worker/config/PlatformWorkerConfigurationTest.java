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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.message.broker.MessageBroker;
import com.bytechef.platform.scheduler.TriggerScheduler;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.Order;

/**
 * @author Ivica Cardic
 */
class PlatformWorkerConfigurationTest {

    @Test
    void testSseStreamProcessorRunsBeforeSuspendProcessor() throws NoSuchMethodException {
        int sseStreamOrder = getOrder("sseStreamTaskExecutionPostOutputProcessor", MessageBroker.class);
        int suspendOrder = getOrder("suspendTaskExecutionPostOutputProcessor", TriggerScheduler.class);

        assertTrue(
            sseStreamOrder < suspendOrder,
            "The SSE stream processor must run before the suspend processor so the Suspend it returns gets persisted");
    }

    private static int getOrder(String methodName, Class<?> parameterType) throws NoSuchMethodException {
        Method method = PlatformWorkerConfiguration.class.getDeclaredMethod(methodName, parameterType);

        Order order = method.getAnnotation(Order.class);

        assertNotNull(order, methodName + " must declare @Order");

        return order.value();
    }
}
