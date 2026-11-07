/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.tool;

import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolContributor;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.ai.copilot.tool.catalog.SimpleIntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Configuration
public class CodeWorkflowIntelligentToolContributorConfiguration {

    @Bean
    public IntelligentToolContributor codeWorkflowIntelligentToolContributor(
        @Qualifier("codeWorkflowAskSubAgentChatClientFactory") ObjectProvider<IntelligentToolChatClientFactory> codeWorkflowAskFactoryProvider,
        @Qualifier("codeWorkflowBuildSubAgentChatClientFactory") ObjectProvider<IntelligentToolChatClientFactory> codeWorkflowBuildFactoryProvider,
        ObjectProvider<SubAgentChatModelResolver> chatModelResolverProvider) {

        SubAgentChatModelResolver chatModelResolver = chatModelResolverProvider.getIfAvailable();

        List<IntelligentToolDefinition> definitions = List.of(
            new SimpleIntelligentToolDefinition(
                "buildCodeWorkflow", CodeWorkflowAgentType.BUILD_CODE_WORKFLOW, Set.of(),
                variant -> factoryForVariant(variant, codeWorkflowAskFactoryProvider, codeWorkflowBuildFactoryProvider),
                chatClientFactory -> new CodeWorkflowAgentToolCallback(chatClientFactory, chatModelResolver)),
            new SimpleIntelligentToolDefinition(
                "explainCodeWorkflow", CodeWorkflowAgentType.EXPLAIN_CODE_WORKFLOW, Set.of(),
                variant -> askFactory(variant, codeWorkflowAskFactoryProvider),
                chatClientFactory -> CodeWorkflowAgentToolCallback.readOnly(chatClientFactory, chatModelResolver)));

        return () -> definitions;
    }

    @Nullable
    private static IntelligentToolChatClientFactory askFactory(
        IntelligentToolVariant variant, ObjectProvider<IntelligentToolChatClientFactory> askProvider) {

        return variant == IntelligentToolVariant.ASK ? askProvider.getIfAvailable() : null;
    }

    @Nullable
    private static IntelligentToolChatClientFactory factoryForVariant(
        IntelligentToolVariant variant, ObjectProvider<IntelligentToolChatClientFactory> askProvider,
        ObjectProvider<IntelligentToolChatClientFactory> buildProvider) {

        return switch (variant) {
            case ASK -> askProvider.getIfAvailable();
            case BUILD -> buildProvider.getIfAvailable();
        };
    }
}
