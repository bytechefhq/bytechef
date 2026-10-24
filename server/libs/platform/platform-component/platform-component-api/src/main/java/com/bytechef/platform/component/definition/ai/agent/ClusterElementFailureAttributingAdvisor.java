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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import reactor.core.publisher.Flux;

/**
 * Records a failure raised by the wrapped advisor on the advisor's cluster element, then rethrows it unchanged.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI_EXPOSE_REP2")
public final class ClusterElementFailureAttributingAdvisor implements CallAdvisor, StreamAdvisor {

    private final Advisor delegate;
    private final String workflowNodeName;
    private final String componentName;
    private final ClusterElementCallTarget target;

    public ClusterElementFailureAttributingAdvisor(
        Advisor delegate, String workflowNodeName, String componentName, ClusterElementCallTarget target) {

        this.delegate = delegate;
        this.workflowNodeName = workflowNodeName;
        this.componentName = componentName;
        this.target = target;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        if (!(delegate instanceof CallAdvisor callAdvisor)) {
            throw new IllegalStateException(delegate.getName() + " is not a CallAdvisor");
        }

        try {
            return callAdvisor.adviseCall(
                chatClientRequest, new ClaimingCallAdvisorChain(callAdvisorChain, target));
        } catch (RuntimeException exception) {
            ClusterElementFailureRecording.record(target, workflowNodeName, componentName, exception);

            throw exception;
        }
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(
        ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {

        if (!(delegate instanceof StreamAdvisor streamAdvisor)) {
            throw new IllegalStateException(delegate.getName() + " is not a StreamAdvisor");
        }

        Flux<ChatClientResponse> responses = streamAdvisor.adviseStream(
            chatClientRequest, new ClaimingStreamAdvisorChain(streamAdvisorChain, target));

        return responses
            .doOnError(
                throwable -> ClusterElementFailureRecording.record(
                    target, workflowNodeName, componentName, throwable));
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public int getOrder() {
        return delegate.getOrder();
    }

    private static void claim(ClusterElementCallTarget target, Throwable throwable) {
        ClusterElementCallRecorder recorder = target.recorder();

        recorder.claimFailure(target.taskExecutionId(), throwable);
    }

    private record ClaimingCallAdvisorChain(CallAdvisorChain chain, ClusterElementCallTarget target)
        implements CallAdvisorChain {

        @Override
        public ChatClientResponse nextCall(ChatClientRequest chatClientRequest) {
            try {
                return chain.nextCall(chatClientRequest);
            } catch (RuntimeException exception) {
                claim(target, exception);

                throw exception;
            }
        }

        @Override
        public List<CallAdvisor> getCallAdvisors() {
            return chain.getCallAdvisors();
        }

        @Override
        public CallAdvisorChain copy(CallAdvisor after) {
            return new ClaimingCallAdvisorChain(chain.copy(after), target);
        }

        @Override
        public ObservationRegistry getObservationRegistry() {
            return chain.getObservationRegistry();
        }
    }

    private record ClaimingStreamAdvisorChain(StreamAdvisorChain chain, ClusterElementCallTarget target)
        implements StreamAdvisorChain {

        @Override
        public Flux<ChatClientResponse> nextStream(ChatClientRequest chatClientRequest) {
            Flux<ChatClientResponse> responses;

            try {
                responses = chain.nextStream(chatClientRequest);
            } catch (RuntimeException exception) {
                claim(target, exception);

                throw exception;
            }

            return responses.doOnError(throwable -> claim(target, throwable));
        }

        @Override
        public List<StreamAdvisor> getStreamAdvisors() {
            return chain.getStreamAdvisors();
        }

        @Override
        public StreamAdvisorChain copy(StreamAdvisor after) {
            return new ClaimingStreamAdvisorChain(chain.copy(after), target);
        }

        @Override
        public ObservationRegistry getObservationRegistry() {
            return chain.getObservationRegistry();
        }
    }
}
