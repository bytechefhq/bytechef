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

package com.bytechef.component.ai.llm.advisor;

import com.bytechef.component.definition.ActionContext;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;

/**
 * Advisor that logs LLM request and response through ByteChef's {@link ActionContext#log} infrastructure, making
 * interactions visible in the workflow execution log viewer.
 *
 * @author Ivica Cardic
 */
public class ContextLoggerAdvisor extends SimpleLoggerAdvisor {

    private final ActionContext context;

    public ContextLoggerAdvisor(ActionContext context) {
        super(DEFAULT_REQUEST_TO_STRING, DEFAULT_RESPONSE_TO_STRING, 0);

        this.context = context;
    }

    @Override
    protected void logRequest(ChatClientRequest request) {
        context.log(log -> log.debug("LLM request: {}", DEFAULT_REQUEST_TO_STRING.apply(request)));
    }

    @Override
    protected void logResponse(ChatClientResponse chatClientResponse) {
        context.log(
            log -> log.debug("LLM response: {}", DEFAULT_RESPONSE_TO_STRING.apply(chatClientResponse.chatResponse())));
    }
}
