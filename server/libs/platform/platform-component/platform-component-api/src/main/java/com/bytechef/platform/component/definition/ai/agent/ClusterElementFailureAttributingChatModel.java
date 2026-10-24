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
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * Records a failure raised by the wrapped model on the model's cluster element, then rethrows it unchanged.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI_EXPOSE_REP2")
public final class ClusterElementFailureAttributingChatModel implements ChatModel {

    private final ChatModel delegate;
    private final String workflowNodeName;
    private final String componentName;
    private final ClusterElementCallTarget target;

    public ClusterElementFailureAttributingChatModel(
        ChatModel delegate, String workflowNodeName, String componentName, ClusterElementCallTarget target) {

        this.delegate = delegate;
        this.workflowNodeName = workflowNodeName;
        this.componentName = componentName;
        this.target = target;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        try {
            return delegate.call(prompt);
        } catch (RuntimeException exception) {
            ClusterElementFailureRecording.record(target, workflowNodeName, componentName, exception);

            throw exception;
        }
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return delegate.stream(prompt)
            .doOnError(
                throwable -> ClusterElementFailureRecording.record(
                    target, workflowNodeName, componentName, throwable));
    }

    @Override
    public ChatOptions getOptions() {
        return delegate.getOptions();
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return delegate.getDefaultOptions();
    }
}
