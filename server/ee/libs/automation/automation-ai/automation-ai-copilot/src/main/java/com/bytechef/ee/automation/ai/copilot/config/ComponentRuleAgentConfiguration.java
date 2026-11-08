/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.copilot.config;

import com.agui.core.exception.AGUIException;
import com.agui.core.state.State;
import com.bytechef.ai.copilot.advisor.CopilotGuardrailsAdvisorFactory;
import com.bytechef.ai.copilot.agent.OverrideChatClientResolver;
import com.bytechef.ai.copilot.agent.SliceSpringAIAgent;
import com.bytechef.ai.copilot.tool.CopilotAgentType;
import com.bytechef.ai.copilot.tool.RehydrateContextToolCallback;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ee.automation.ai.tool.componentrule.ComponentRuleToolCallbacksFactory;
import com.bytechef.ee.platform.component.rule.ComponentRuleService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
@ConditionalOnExpression("${bytechef.ai.copilot.enabled:false} or ${bytechef.ai.hub.enabled:false}")
public class ComponentRuleAgentConfiguration {

    @Value("classpath:prompt_component_rule_ask.txt")
    private Resource promptComponentRuleAskResource;

    @Value("classpath:prompt_component_rule_build.txt")
    private Resource promptComponentRuleBuildResource;

    private final State state = new State();

    @Bean
    ComponentRuleToolCallbacksFactory componentRuleToolCallbacksFactory(
        ComponentRuleService componentRuleService, ComponentDefinitionService componentDefinitionService,
        ClusterElementDefinitionService clusterElementDefinitionService, Evaluator evaluator) {

        return new ComponentRuleToolCallbacksFactory(
            componentRuleService, componentDefinitionService, clusterElementDefinitionService, evaluator);
    }

    @Bean
    SliceSpringAIAgent componentRuleAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel,
        ComponentRuleToolCallbacksFactory componentRuleToolCallbacksFactory,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider,
        CopilotGuardrailsAdvisorFactory copilotGuardrailsAdvisorFactory)
        throws AGUIException {

        return SliceSpringAIAgent.builder()
            .advisors(copilotGuardrailsAdvisorFactory.guardrailsAdvisors())
            .agentId(CopilotAgentType.COMPONENT_RULE_ASK.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(readPrompt(promptComponentRuleAskResource))
            .state(state)
            .toolCallbacks(
                wrapToolCallbacks(securityContextRehydrator, componentRuleToolCallbacksFactory.readToolCallbacks()))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    SliceSpringAIAgent componentRuleBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel,
        ComponentRuleToolCallbacksFactory componentRuleToolCallbacksFactory,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider,
        CopilotGuardrailsAdvisorFactory copilotGuardrailsAdvisorFactory)
        throws AGUIException {

        return SliceSpringAIAgent.builder()
            .advisors(copilotGuardrailsAdvisorFactory.guardrailsAdvisors())
            .agentId(CopilotAgentType.COMPONENT_RULE_BUILD.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(readPrompt(promptComponentRuleBuildResource))
            .state(state)
            .toolCallbacks(
                wrapToolCallbacks(securityContextRehydrator, componentRuleToolCallbacksFactory.writeToolCallbacks()))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    private List<ToolCallback> wrapToolCallbacks(
        SecurityContextRehydrator securityContextRehydrator, List<ToolCallback> toolCallbacks) {

        List<ToolCallback> wrapped = new ArrayList<>(toolCallbacks.size());

        for (ToolCallback toolCallback : toolCallbacks) {
            wrapped.add(RehydrateContextToolCallback.wrap(toolCallback, securityContextRehydrator));
        }

        return wrapped;
    }

    private String readPrompt(Resource resource) {
        try {
            InputStream inputStream = resource.getInputStream();

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Failed to read component rule prompt resource: " + resource.getDescription(), exception);
        }
    }
}
