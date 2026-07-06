/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.advisor;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.configuration.ai.EmbeddingProviderStatusProvider;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import reactor.core.scheduler.Scheduler;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EnvironmentAwareQuestionAnswerAdvisor implements BaseAdvisor {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentAwareQuestionAnswerAdvisor.class);

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

        boolean embeddingActive = environment != null && embeddingProviderStatusProvider.isEmbeddingActive(environment);
        Map<String, @Nullable Object> context = chatClientRequest.context();

        log.trace(
            "Copilot RAG: advisorContextKeys={}, {}={}, resolvedEnvironment={}, embeddingActive={}", context.keySet(),
            environmentParamKey, context.get(environmentParamKey), environment, embeddingActive);

        if (!embeddingActive) {
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
