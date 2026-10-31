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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRecord.Status;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * @author Ivica Cardic
 */
class ClusterElementFailureAttributingAdvisorTest {

    private final ClusterElementCallRecorder recorder = new ClusterElementCallRecorder();
    private final ClusterElementCallTarget target = new ClusterElementCallTarget(recorder, 1L, 2L);
    private final ChatClientRequest request = mock(ChatClientRequest.class);
    private final CallAdvisorChain chain = mock(CallAdvisorChain.class);

    @AfterEach
    void afterEach() {
        recorder.drain(2L);
    }

    @Test
    void testAdvisorOwnFailureIsRecordedOnItsElement() {
        RuntimeException exception = new RuntimeException("store unavailable");
        CallAdvisor delegate = mock(CallAdvisor.class);

        when(delegate.adviseCall(eq(request), any())).thenThrow(exception);

        CallAdvisor advisor = new ClusterElementFailureAttributingAdvisor(delegate, "chatMemory_1", "simple", target);

        assertThatThrownBy(() -> advisor.adviseCall(request, chain)).isSameAs(exception);

        List<ClusterElementCallRecord> records = recorder.drain(2L)
            .records();

        assertThat(records).hasSize(1);
        assertThat(records.getFirst()
            .status()).isEqualTo(Status.FAILED);
        assertThat(records.getFirst()
            .workflowNodeName()).isEqualTo("chatMemory_1");
        assertThat(records.getFirst()
            .error()).isEqualTo("store unavailable");
    }

    @Test
    void testFailureFromBelowThatWasClaimedPassesThroughUnrecorded() {
        RuntimeException exception = new RuntimeException("model failed");

        recorder.claimFailure(2L, exception);

        when(chain.nextCall(request)).thenThrow(exception);

        CallAdvisor delegate = new PassThroughAdvisor();
        CallAdvisor advisor = new ClusterElementFailureAttributingAdvisor(delegate, "chatMemory_1", "simple", target);

        assertThatThrownBy(() -> advisor.adviseCall(request, chain)).isSameAs(exception);
        assertThat(recorder.drain(2L)
            .records()).isEmpty();
    }

    @Test
    void testFailureFromBelowIsNotAttributedToTheAdvisor() {
        RuntimeException exception = new IllegalStateException("No ToolCallback found for tool name: invented");

        when(chain.nextCall(request)).thenThrow(exception);

        CallAdvisor advisor = new ClusterElementFailureAttributingAdvisor(
            new PassThroughAdvisor(), "chatMemory_1", "simple", target);

        assertThatThrownBy(() -> advisor.adviseCall(request, chain)).isSameAs(exception);
        assertThat(recorder.drain(2L)
            .records()).isEmpty();
    }

    @Test
    void testStreamFailureFromBelowIsNotAttributedToTheAdvisor() {
        RuntimeException exception = new IllegalStateException("No ToolCallback found for tool name: invented");
        StreamAdvisorChain streamAdvisorChain = mock(StreamAdvisorChain.class);

        when(streamAdvisorChain.nextStream(request)).thenReturn(Flux.error(exception));

        StreamAdvisor advisor = new ClusterElementFailureAttributingAdvisor(
            new PassThroughAdvisor(), "chatMemory_1", "simple", target);

        StepVerifier.create(advisor.adviseStream(request, streamAdvisorChain))
            .expectErrorMatches(throwable -> throwable == exception)
            .verify();

        assertThat(recorder.drain(2L)
            .records()).isEmpty();
    }

    @Test
    void testStreamOwnFailureIsRecordedOnce() {
        RuntimeException exception = new RuntimeException("store unavailable");
        StreamAdvisor delegate = mock(StreamAdvisor.class);
        StreamAdvisorChain streamAdvisorChain = mock(StreamAdvisorChain.class);

        when(delegate.adviseStream(eq(request), any()))
            .thenReturn(Flux.error(exception));

        StreamAdvisor advisor = new ClusterElementFailureAttributingAdvisor(delegate, "chatMemory_1", "simple", target);

        StepVerifier.create(advisor.adviseStream(request, streamAdvisorChain))
            .expectErrorMatches(throwable -> throwable == exception)
            .verify();

        List<ClusterElementCallRecord> records = recorder.drain(2L)
            .records();

        assertThat(records).hasSize(1);
        assertThat(records.getFirst()
            .status()).isEqualTo(Status.FAILED);
        assertThat(records.getFirst()
            .workflowNodeName()).isEqualTo("chatMemory_1");
    }

    @Test
    void testOrderAndNameDelegate() {
        CallAdvisor delegate = new PassThroughAdvisor();
        ClusterElementFailureAttributingAdvisor advisor = new ClusterElementFailureAttributingAdvisor(
            delegate, "chatMemory_1", "simple", target);

        assertThat(advisor.getName()).isEqualTo("pass-through");
        assertThat(advisor.getOrder()).isEqualTo(7);
    }

    @Test
    void testNonCallAdvisorDelegateIsRejectedForACall() {
        StreamAdvisor delegate = mock(StreamAdvisor.class);
        ClusterElementFailureAttributingAdvisor advisor = new ClusterElementFailureAttributingAdvisor(
            delegate, "chatMemory_1", "simple", target);

        assertThatThrownBy(() -> advisor.adviseCall(request, chain)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testNonStreamAdvisorDelegateIsRejectedForAStream() {
        CallAdvisor delegate = mock(CallAdvisor.class);
        ClusterElementFailureAttributingAdvisor advisor = new ClusterElementFailureAttributingAdvisor(
            delegate, "chatMemory_1", "simple", target);

        assertThatThrownBy(() -> advisor.adviseStream(request, mock(StreamAdvisorChain.class)))
            .isInstanceOf(IllegalStateException.class);
    }

    private static final class PassThroughAdvisor implements CallAdvisor, StreamAdvisor {

        @Override
        public ChatClientResponse adviseCall(
            ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {

            return callAdvisorChain.nextCall(chatClientRequest);
        }

        @Override
        public Flux<ChatClientResponse> adviseStream(
            ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {

            return streamAdvisorChain.nextStream(chatClientRequest);
        }

        @Override
        public String getName() {
            return "pass-through";
        }

        @Override
        public int getOrder() {
            return 7;
        }
    }
}
