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

package com.bytechef.ai.copilot.advisor;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.configuration.ai.EmbeddingProviderStatusProvider;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import reactor.core.scheduler.Scheduler;

/**
 * @author Ivica Cardic
 */
public class EnvironmentAwareQuestionAnswerAdvisor implements BaseAdvisor {

    private final QuestionAnswerAdvisor delegate;
    private final EmbeddingProviderStatusProvider embeddingProviderStatusProvider;
    private final String environmentParamKey;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public EnvironmentAwareQuestionAnswerAdvisor(
        VectorStore vectorStore, EmbeddingProviderStatusProvider embeddingProviderStatusProvider,
        String environmentParamKey) {

        this.delegate = QuestionAnswerAdvisor.builder(vectorStore)
            .build();
        this.embeddingProviderStatusProvider = embeddingProviderStatusProvider;
        this.environmentParamKey = environmentParamKey;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        Integer environment = resolveEnvironment(chatClientRequest);

        if (environment == null || !embeddingProviderStatusProvider.isEmbeddingActive(environment)) {
            return chatClientRequest;
        }

        Environment previousEnvironment = EnvironmentContext.fetchCurrentEnvironment();

        EnvironmentContext.set(environment);

        try {
            return delegate.before(chatClientRequest, advisorChain);
        } finally {
            if (previousEnvironment == null) {
                EnvironmentContext.clear();
            } else {
                EnvironmentContext.set(previousEnvironment);
            }
        }
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return delegate.after(chatClientResponse, advisorChain);
    }

    @Override
    public int getOrder() {
        return delegate.getOrder();
    }

    @Override
    public Scheduler getScheduler() {
        return delegate.getScheduler();
    }

    private @Nullable Integer resolveEnvironment(ChatClientRequest chatClientRequest) {
        Map<String, @Nullable Object> context = chatClientRequest.context();

        Long environmentId = MapUtils.getLong(context, environmentParamKey);

        if (environmentId == null || environmentId < 0 || environmentId >= Environment.values().length) {
            return null;
        }

        return environmentId.intValue();
    }
}
