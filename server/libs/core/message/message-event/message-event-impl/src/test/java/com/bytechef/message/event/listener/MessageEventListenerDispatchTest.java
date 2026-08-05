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

package com.bytechef.message.event.listener;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

import com.bytechef.message.broker.MessageBroker;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Pins that plain (non-{@link com.bytechef.message.event.MessageEvent}) payload events published through the Spring
 * context never reach {@link MessageEventListener} — its generic {@code MessageEvent<?>} parameter must not be matched
 * optimistically for foreign payload types, which would fail with {@code ClassCastException} at dispatch time (seen
 * with {@code DeleteJobEvent} under DevTools).
 *
 * @author Ivica Cardic
 */
public class MessageEventListenerDispatchTest {

    @Test
    public void testForeignPayloadEventDoesNotReachMessageEventListener() {
        try (AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(
            TestConfiguration.class)) {

            assertThatCode(() -> applicationContext.publishEvent(new ForeignPayloadEvent(123L)))
                .doesNotThrowAnyException();
        }
    }

    record ForeignPayloadEvent(long jobId) {
    }

    @Configuration
    static class TestConfiguration {

        @Bean
        MessageEventListener messageEventListener() {
            return new MessageEventListener(mock(MessageBroker.class), List.of());
        }
    }
}
