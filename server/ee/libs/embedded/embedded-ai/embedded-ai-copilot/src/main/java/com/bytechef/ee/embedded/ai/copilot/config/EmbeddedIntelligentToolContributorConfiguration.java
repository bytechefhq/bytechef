/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.copilot.config;

import com.bytechef.ai.copilot.tool.CopilotAgentType;
import com.bytechef.ai.copilot.tool.ProjectWorkflowAgentToolCallback;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolContributor;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.ai.copilot.tool.catalog.SimpleIntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
class EmbeddedIntelligentToolContributorConfiguration {

    static final CopilotAgentType AGENT_TYPE = CopilotAgentType.BUILD_INTEGRATION_WORKFLOW;

    static final String TOOL_NAME = AGENT_TYPE.key();

    static final String DESCRIPTION =
        """
            Delegate a user request about whole embedded INTEGRATION workflows to the specialised Embedded
            Workflow Editor subagent. Use this for requests that design, edit, debug, or explain an
            integration's workflows (orchestration of tasks, triggers, conditions, loops). It also manages
            the integrations themselves (list/create/update/delete/publish). Prefer calling it over
            reasoning about integration-workflow shape directly. Returns the updated workflow JSON plus a
            change rationale. This is the embedded counterpart of buildWorkflow (which targets
            automation projects). To edit an existing integration's workflow, include its workflowId in
            the request; to create a new integration, describe it in the request instead.""";

    @Bean
    IntelligentToolContributor embeddedIntelligentToolContributor(
        @Qualifier("workflowEditorEmbeddedBuildSubAgentChatClientFactory") //
        ObjectProvider<IntelligentToolChatClientFactory> workflowEditorEmbeddedBuildFactoryProvider,
        ObjectProvider<SubAgentChatModelResolver> chatModelResolverProvider) {

        SubAgentChatModelResolver chatModelResolver = chatModelResolverProvider.getIfAvailable();

        List<IntelligentToolDefinition> definitions = List.of(
            new SimpleIntelligentToolDefinition(
                TOOL_NAME, AGENT_TYPE, Set.of(),
                variant -> variant == IntelligentToolVariant.BUILD
                    ? workflowEditorEmbeddedBuildFactoryProvider.getIfAvailable() : null,
                chatClientFactory -> new ProjectWorkflowAgentToolCallback(
                    chatClientFactory, chatModelResolver, TOOL_NAME, DESCRIPTION, AGENT_TYPE)));

        return () -> definitions;
    }
}
