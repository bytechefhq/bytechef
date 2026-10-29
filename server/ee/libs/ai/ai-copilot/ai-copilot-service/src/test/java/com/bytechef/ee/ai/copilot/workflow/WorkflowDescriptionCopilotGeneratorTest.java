/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.agent.ChatModelUnavailableException;
import com.bytechef.ai.copilot.agent.OverrideChatClientResolver;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.ee.ai.copilot.CopilotGenerationException;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * @version ee
 * @author Ivica Cardic
 */
class WorkflowDescriptionCopilotGeneratorTest {

    private final ChatModel chatModel = mock(ChatModel.class);
    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final OverrideChatClientResolver overrideChatClientResolver = mock(OverrideChatClientResolver.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGenerateWholeWorkflowDescriptionStripsFences() {
        WorkflowDescriptionCopilotGeneratorImpl generator = generatorReturning(
            "{\"label\":\"Sync\"}", "```\nSyncs records nightly.\n```");

        WorkflowDescriptionCopilotResult result = generator.generate(
            new WorkflowDescriptionCopilotRequest("wf1", null, 0));

        assertThat(result.value()).isEqualTo("Syncs records nightly.");
    }

    @Test
    void testGenerateNodeDescription() {
        WorkflowDescriptionCopilotGeneratorImpl generator = generatorReturning(
            "{\"tasks\":[{\"name\":\"node1\"}]}", "Sends a Slack message.");

        WorkflowDescriptionCopilotResult result = generator.generate(
            new WorkflowDescriptionCopilotRequest("wf1", "node1", 0));

        assertThat(result.value()).isEqualTo("Sends a Slack message.");
    }

    @Test
    void testGenerateStripsLanguageTaggedFences() {
        WorkflowDescriptionCopilotGeneratorImpl generator = generatorReturning(
            "{\"label\":\"Sync\"}", "```markdown\nSyncs records nightly.\n```");

        WorkflowDescriptionCopilotResult result = generator.generate(
            new WorkflowDescriptionCopilotRequest("wf1", null, 0));

        assertThat(result.value()).isEqualTo("Syncs records nightly.");
    }

    @Test
    void testGenerateUsesTheCatalogModelOfTheRequestEnvironment() {
        WorkflowDescriptionCopilotGeneratorImpl generator = generatorReturning("{}", "Syncs records.");

        generator.generate(new WorkflowDescriptionCopilotRequest("wf1", null, 1));

        verify(overrideChatClientResolver).resolveDefaultChatModel(1);
    }

    @Test
    void testGenerateUsesTheConfinedPrincipalEnvironment() {
        SecurityContextHolder.getContext()
            .setAuthentication(new TestApiKeyAuthenticationToken(2, new User("connected-user-1", "", List.of())));

        WorkflowDescriptionCopilotGeneratorImpl generator = generatorReturning("{}", "Syncs records.");

        generator.generate(new WorkflowDescriptionCopilotRequest("wf1", null, 0));

        verify(overrideChatClientResolver).resolveDefaultChatModel(2);
    }

    @Test
    void testGenerateFailsWhenNoProviderIsEnabled() {
        WorkflowDescriptionCopilotGeneratorImpl generator = generatorReturning("{}", "unused");

        when(overrideChatClientResolver.resolveDefaultChatModel(anyInt()))
            .thenThrow(ChatModelUnavailableException.noProviderEnabled(1));

        assertThatThrownBy(() -> generator.generate(new WorkflowDescriptionCopilotRequest("wf1", null, 1)))
            .isInstanceOf(ChatModelUnavailableException.class)
            .hasMessageContaining("No AI provider is enabled in environment 1");
    }

    @Test
    void testGenerateFailsOnEmptyModelOutput() {
        WorkflowDescriptionCopilotGeneratorImpl generator = generatorReturning("{}", "```\n```");

        assertThatThrownBy(() -> generator.generate(new WorkflowDescriptionCopilotRequest("wf1", null, 0)))
            .isInstanceOf(CopilotGenerationException.class)
            .hasMessageContaining("returned no description");
    }

    @SuppressWarnings("unchecked")
    private WorkflowDescriptionCopilotGeneratorImpl generatorReturning(String definition, String llmText) {
        when(chatModel.call(any(Prompt.class))).thenReturn(
            ChatResponse.builder()
                .generations(List.of(new Generation(new AssistantMessage(llmText))))
                .build());
        when(overrideChatClientResolver.resolveDefaultChatModel(anyInt())).thenReturn(chatModel);

        Workflow workflow = mock(Workflow.class);

        when(workflow.getDefinition()).thenReturn(definition);
        when(workflowService.getWorkflow(any())).thenReturn(workflow);

        ObjectProvider<MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);

        when(meterRegistryProvider.getIfAvailable()).thenReturn(meterRegistry);

        return new WorkflowDescriptionCopilotGeneratorImpl(
            overrideChatClientResolver, workflowService, new WorkflowDescriptionPromptBuilder(),
            meterRegistryProvider);
    }

    private static final class TestApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

        private TestApiKeyAuthenticationToken(long environmentId, User user) {
            super(environmentId, user);
        }
    }
}
