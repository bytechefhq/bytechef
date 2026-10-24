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

package com.bytechef.platform.component.definition.ai.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRecord.Status;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * @author Ivica Cardic
 */
class ClusterElementFailureAttributingChatModelTest {

    private final ClusterElementCallRecorder recorder = new ClusterElementCallRecorder();
    private final ClusterElementCallTarget target = new ClusterElementCallTarget(recorder, 1L, 2L);
    private final ChatModel delegate = mock(ChatModel.class);
    private final Prompt prompt = new Prompt("hello");
    private final ChatModel chatModel = new ClusterElementFailureAttributingChatModel(
        delegate, "model_1", "openai", target);

    @AfterEach
    void afterEach() {
        recorder.drain(2L);
    }

    @Test
    void testCallFailureIsRecordedOnTheModelElementAndRethrown() {
        RuntimeException exception = new RuntimeException("rate limited");

        when(delegate.call(prompt)).thenThrow(exception);

        assertThatThrownBy(() -> chatModel.call(prompt)).isSameAs(exception);

        List<ClusterElementCallRecord> records = recorder.drain(2L)
            .records();

        assertThat(records).hasSize(1);

        ClusterElementCallRecord callRecord = records.getFirst();

        assertThat(callRecord.status()).isEqualTo(Status.FAILED);
        assertThat(callRecord.workflowNodeName()).isEqualTo("model_1");
        assertThat(callRecord.componentName()).isEqualTo("openai");
        assertThat(callRecord.error()).isEqualTo("rate limited");
        assertThat(callRecord.parentId()).isNull();
    }

    @Test
    void testCallFailureUnderACurrentCallNestsUnderIt() {
        when(delegate.call(prompt)).thenThrow(new RuntimeException("boom"));

        ClusterElementCallRef ref = new ClusterElementCallRef("parent-id", "aiAgent_1");

        ClusterElementCallScope.callWith(ref, () -> {
            assertThatThrownBy(() -> chatModel.call(prompt)).isInstanceOf(RuntimeException.class);

            return null;
        });

        assertThat(recorder.drain(2L)
            .records()
            .getFirst()
            .parentId()).isEqualTo("parent-id");
    }

    @Test
    void testAlreadyClaimedFailureIsNotRecordedTwice() {
        RuntimeException exception = new RuntimeException("boom");

        recorder.claimFailure(2L, exception);

        when(delegate.call(prompt)).thenThrow(exception);

        assertThatThrownBy(() -> chatModel.call(prompt)).isSameAs(exception);
        assertThat(recorder.drain(2L)
            .records()).isEmpty();
    }

    @Test
    void testStreamFailureIsRecordedOnce() {
        RuntimeException exception = new RuntimeException("stream broke");

        when(delegate.stream(prompt)).thenReturn(Flux.error(exception));

        StepVerifier.create(chatModel.stream(prompt))
            .expectErrorMatches(error -> error == exception)
            .verify();

        List<ClusterElementCallRecord> records = recorder.drain(2L)
            .records();

        assertThat(records).hasSize(1);
        assertThat(records.getFirst()
            .status()).isEqualTo(Status.FAILED);
        assertThat(records.getFirst()
            .error()).isEqualTo("stream broke");
    }

    @Test
    void testSuccessRecordsNothing() {
        ChatResponse chatResponse = mock(ChatResponse.class);

        when(delegate.call(prompt)).thenReturn(chatResponse);

        assertThat(chatModel.call(prompt)).isSameAs(chatResponse);
        assertThat(recorder.drain(2L)
            .records()).isEmpty();
    }

    @Test
    void testOptionsAreDelegated() {
        ChatOptions options = mock(ChatOptions.class);

        when(delegate.getOptions()).thenReturn(options);

        assertThat(chatModel.getOptions()).isSameAs(options);
    }
}
