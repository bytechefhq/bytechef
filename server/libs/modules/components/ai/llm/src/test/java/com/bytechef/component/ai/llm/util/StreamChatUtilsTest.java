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

package com.bytechef.component.ai.llm.util;

import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.llm.ChatModel.ResponseFormat;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.SseEmitterHandler.SseEmitter;
import com.bytechef.component.definition.Parameters;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class StreamChatUtilsTest {

    @Test
    void testCreateSseEmitterHandlerSetsTheStreamedTextAsTheOutput() throws InterruptedException {
        Parameters parameters = mock(Parameters.class);

        when(parameters.getRequiredFromPath(RESPONSE + "." + RESPONSE_FORMAT, ResponseFormat.class))
            .thenReturn(ResponseFormat.TEXT);

        RecordingSseEmitter recordingSseEmitter = new RecordingSseEmitter();

        try (SubmissionPublisher<String> publisher = new SubmissionPublisher<>()) {
            StreamChatUtils.createSseEmitterHandler(publisher, parameters, mock(ActionContext.class))
                .handle(recordingSseEmitter);

            publisher.submit("Hello");
            publisher.submit(", world");
        }

        assertThat(recordingSseEmitter.awaitCompletion()).isTrue();
        assertThat(recordingSseEmitter.sent).containsExactly("Hello", ", world");
        assertThat(recordingSseEmitter.output.get()).isEqualTo("Hello, world");
    }

    @Test
    void testCreateSseEmitterHandlerSetsNoOutputWhenNoTextWasStreamed() throws InterruptedException {
        Parameters parameters = mock(Parameters.class);

        when(parameters.getRequiredFromPath(RESPONSE + "." + RESPONSE_FORMAT, ResponseFormat.class))
            .thenReturn(ResponseFormat.TEXT);

        RecordingSseEmitter recordingSseEmitter = new RecordingSseEmitter();

        try (SubmissionPublisher<String> publisher = new SubmissionPublisher<>()) {
            StreamChatUtils.createSseEmitterHandler(publisher, parameters, mock(ActionContext.class))
                .handle(recordingSseEmitter);
        }

        assertThat(recordingSseEmitter.awaitCompletion()).isTrue();
        assertThat(recordingSseEmitter.output.get()).isNull();
    }

    @Test
    void testCreateSseEmitterHandlerFailsTheStreamAndSetsNoOutputWhenASendFails() throws InterruptedException {
        RecordingSseEmitter recordingSseEmitter = new RecordingSseEmitter(new IllegalStateException("disconnected"));

        try (SubmissionPublisher<String> publisher = new SubmissionPublisher<>()) {
            StreamChatUtils.createSseEmitterHandler(publisher, mock(Parameters.class), mock(ActionContext.class))
                .handle(recordingSseEmitter);

            publisher.submit("Hello");
        }

        assertThat(recordingSseEmitter.awaitCompletion()).isTrue();
        assertThat(recordingSseEmitter.error.get()).hasMessage("disconnected");
        assertThat(recordingSseEmitter.output.get()).isNull();
    }

    private static final class RecordingSseEmitter implements SseEmitter {

        private final CountDownLatch completed = new CountDownLatch(1);
        private final AtomicReference<Throwable> error = new AtomicReference<>();
        private final AtomicReference<Object> output = new AtomicReference<>();
        private final RuntimeException sendFailure;
        private final List<Object> sent = new CopyOnWriteArrayList<>();

        private RecordingSseEmitter() {
            this(null);
        }

        private RecordingSseEmitter(RuntimeException sendFailure) {
            this.sendFailure = sendFailure;
        }

        @Override
        public void addTimeoutListener(Runnable timeoutListener) {
        }

        @Override
        public void complete() {
            completed.countDown();
        }

        @Override
        public void error(Throwable throwable) {
            error.set(throwable);

            completed.countDown();
        }

        @Override
        public void send(Object data) {
            if (sendFailure != null) {
                throw sendFailure;
            }

            sent.add(data);
        }

        @Override
        public void setOutput(Object output) {
            this.output.set(output);
        }

        boolean awaitCompletion() throws InterruptedException {
            return completed.await(5, TimeUnit.SECONDS);
        }
    }
}
