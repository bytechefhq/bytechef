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
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Contributes the embedded intelligent delegate tool — {@code buildIntegrationWorkflow} — to the
 * {@link com.bytechef.ai.copilot.tool.catalog.IntelligentToolCatalog}, the embedded counterpart of the CE
 * {@code CopilotIntelligentToolContributorConfiguration} and the EE automation
 * {@code AutomationIntelligentToolContributorConfiguration}.
 *
 * <p>
 * There is no embedded ASK subagent {@link IntelligentToolChatClientFactory} bean — only BUILD exists — so
 * {@link IntelligentToolDefinition#chatClientFactory(IntelligentToolVariant)} always returns {@code null} for
 * {@link IntelligentToolVariant#ASK} and the catalog skips this definition for ASK surfaces. The
 * {@link IntelligentToolChatClientFactory} bean is resolved via an {@link ObjectProvider} so a missing bean (feature
 * module absent) yields a {@code null} factory for BUILD too, looked up lazily inside {@code chatClientFactory} so
 * declaring the definition never forces the bean to be instantiated. That bean is declared in
 * {@code EmbeddedCopilotConfiguration}.
 * </p>
 *
 * <p>
 * The delegate reuses the shared {@link ProjectWorkflowAgentToolCallback} via its variant constructor, parameterized
 * with the embedded tool name, description, and {@link CopilotAgentType#BUILD_INTEGRATION_WORKFLOW} — exactly as the
 * management MCP contributor built it before this refactor.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
class EmbeddedIntelligentToolContributorConfiguration {

    // Package-private (not private) so EmbeddedIntelligentToolContributorConfigurationTest can pin this LLM-facing
    // routing
    // text directly rather than duplicating it — a botched move of this description between classes is exactly
    // the risk a move-only refactor should catch.
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
                "buildIntegrationWorkflow", CopilotAgentType.BUILD_INTEGRATION_WORKFLOW.key(), Set.of(),
                variant -> variant == IntelligentToolVariant.BUILD
                    ? workflowEditorEmbeddedBuildFactoryProvider.getIfAvailable() : null,
                chatClientFactory -> new ProjectWorkflowAgentToolCallback(
                    chatClientFactory, chatModelResolver, "buildIntegrationWorkflow", DESCRIPTION,
                    CopilotAgentType.BUILD_INTEGRATION_WORKFLOW)));

        return () -> definitions;
    }
}
