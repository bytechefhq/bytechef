/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.copilot.config;

import com.bytechef.ai.copilot.tool.CopilotAgentType;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolContributor;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.ai.copilot.tool.catalog.SimpleIntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import com.bytechef.ee.automation.ai.copilot.tool.CodeWorkflowAgentToolCallback;
import com.bytechef.ee.automation.ai.copilot.tool.CustomComponentAgentToolCallback;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Contributes the two EE automation Copilot intelligent delegate tools — {@code buildCustomComponent} and
 * {@code buildCodeWorkflow} — to the {@link com.bytechef.ai.copilot.tool.catalog.IntelligentToolCatalog}, the EE
 * automation counterpart of the CE {@code CopilotIntelligentToolContributorConfiguration}.
 *
 * <p>
 * Every {@link IntelligentToolChatClientFactory} is resolved via an {@link ObjectProvider} so a missing bean simply
 * yields a {@code null} {@link IntelligentToolDefinition#chatClientFactory(IntelligentToolVariant)} for that variant,
 * which the catalog skips. Providers are looked up lazily — inside {@code chatClientFactory}, not while this bean
 * method builds the definition list — so declaring the two definitions never forces a {@code ChatClient} bean to be
 * instantiated; instantiation happens only when a surface (AI Hub, the management MCP contributor) actually asks the
 * catalog for a callback. Each {@link IntelligentToolChatClientFactory} bean for each domain is declared in
 * {@code AutomationCopilotConfiguration}.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
class AutomationIntelligentToolContributorConfiguration {

    @Bean
    IntelligentToolContributor automationIntelligentToolContributor(
        @Qualifier("customComponentAskSubAgentChatClientFactory") //
        ObjectProvider<IntelligentToolChatClientFactory> customComponentAskFactoryProvider,
        @Qualifier("customComponentBuildSubAgentChatClientFactory") //
        ObjectProvider<IntelligentToolChatClientFactory> customComponentBuildFactoryProvider,
        @Qualifier("codeWorkflowAskSubAgentChatClientFactory") //
        ObjectProvider<IntelligentToolChatClientFactory> codeWorkflowAskFactoryProvider,
        @Qualifier("codeWorkflowBuildSubAgentChatClientFactory") //
        ObjectProvider<IntelligentToolChatClientFactory> codeWorkflowBuildFactoryProvider,
        ObjectProvider<SubAgentChatModelResolver> chatModelResolverProvider) {

        SubAgentChatModelResolver chatModelResolver = chatModelResolverProvider.getIfAvailable();

        List<IntelligentToolDefinition> definitions = List.of(
            new SimpleIntelligentToolDefinition(
                "buildCustomComponent", CopilotAgentType.BUILD_CUSTOM_COMPONENT.key(), Set.of(),
                variant -> factoryForVariant(
                    variant, customComponentAskFactoryProvider, customComponentBuildFactoryProvider),
                chatClientFactory -> new CustomComponentAgentToolCallback(chatClientFactory, chatModelResolver)),
            new SimpleIntelligentToolDefinition(
                "buildCodeWorkflow", CopilotAgentType.BUILD_CODE_WORKFLOW.key(), Set.of(),
                variant -> factoryForVariant(variant, codeWorkflowAskFactoryProvider, codeWorkflowBuildFactoryProvider),
                chatClientFactory -> new CodeWorkflowAgentToolCallback(chatClientFactory, chatModelResolver)));

        return () -> definitions;
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
