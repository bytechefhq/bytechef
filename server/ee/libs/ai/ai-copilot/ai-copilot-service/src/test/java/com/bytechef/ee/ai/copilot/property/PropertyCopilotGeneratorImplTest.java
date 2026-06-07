/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.agent.catalog.CatalogChatClientResolver;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class PropertyCopilotGeneratorImplTest {

    private final WorkflowNodeOutputFacade workflowNodeOutputFacade = mock(WorkflowNodeOutputFacade.class);
    private final Evaluator evaluator = mock(Evaluator.class);
    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private static ChatResponse buildChatResponse(String text) {
        Generation generation = new Generation(new AssistantMessage(text));

        return ChatResponse.builder()
            .generations(List.of(generation))
            .build();
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<CatalogChatClientResolver> emptyCatalogChatClientResolverProvider() {
        ObjectProvider<CatalogChatClientResolver> catalogChatClientResolverProvider = mock(ObjectProvider.class);

        when(catalogChatClientResolverProvider.getIfAvailable()).thenReturn(null);

        return catalogChatClientResolverProvider;
    }

    @SuppressWarnings("unchecked")
    private PropertyCopilotGeneratorImpl generatorReturning(String llmText) {
        ChatModel chatModel = mock(ChatModel.class);
        ChatResponse chatResponse = buildChatResponse(llmText);

        when(chatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(chatResponse);
        when(workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(any(), any(), anyLong()))
            .thenReturn(List.of());
        when(workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(any(), any(), anyLong()))
            .thenReturn(Map.of());

        ObjectProvider<MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);

        when(meterRegistryProvider.getIfAvailable()).thenReturn(meterRegistry);

        return new PropertyCopilotGeneratorImpl(
            chatModel, evaluator, new PropertyCopilotPromptBuilder(), List.of(), workflowNodeOutputFacade,
            meterRegistryProvider, "", emptyCatalogChatClientResolverProvider());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGenerateUsesCatalogResolvedChatClientWhenAvailable() {
        EnvironmentContext.clear();

        ChatModel chatModel = mock(ChatModel.class);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        CatalogChatClientResolver catalogChatClientResolver = mock(CatalogChatClientResolver.class);

        when(catalogChatClientResolver.resolveDefault(Environment.STAGING.ordinal())).thenReturn(chatClient);
        when(chatClient.prompt(any(String.class))
            .call()
            .content()).thenReturn("a constant value");
        when(workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(any(), any(), anyLong())).thenReturn(List.of());
        when(workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(any(), any(), anyLong()))
            .thenReturn(Map.of());

        ObjectProvider<MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);

        when(meterRegistryProvider.getIfAvailable()).thenReturn(meterRegistry);

        ObjectProvider<CatalogChatClientResolver> catalogChatClientResolverProvider = mock(ObjectProvider.class);

        when(catalogChatClientResolverProvider.getIfAvailable()).thenReturn(catalogChatClientResolver);

        PropertyCopilotGeneratorImpl generator = new PropertyCopilotGeneratorImpl(
            chatModel, evaluator, new PropertyCopilotPromptBuilder(), List.of(), workflowNodeOutputFacade,
            meterRegistryProvider, "", catalogChatClientResolverProvider);

        try {
            PropertyCopilotResult result = generator.generate(new PropertyCopilotRequest(
                "value", PropertyCopilotMode.TEXT, "wf1", "node2", "message", "STRING", true,
                Environment.STAGING.ordinal()));

            assertThat(result.value()).isEqualTo("a constant value");

            verify(chatModel, never()).call(any(org.springframework.ai.chat.prompt.Prompt.class));
        } finally {
            EnvironmentContext.clear();
        }
    }

    @Test
    void testGenerateFallsBackToChatModelWhenNoCatalogResolver() {
        PropertyCopilotGeneratorImpl generator = generatorReturning("a constant value");

        PropertyCopilotResult result = generator.generate(new PropertyCopilotRequest(
            "value", PropertyCopilotMode.TEXT, "wf1", "node2", "message", "STRING", true, 0));

        assertThat(result.value()).isEqualTo("a constant value");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGenerateBindsRequestEnvironmentDuringChatModelCall() {
        EnvironmentContext.clear();

        ChatModel chatModel = mock(ChatModel.class);
        AtomicReference<Environment> observedEnvironment = new AtomicReference<>();

        when(chatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenAnswer(invocation -> {
            observedEnvironment.set(EnvironmentContext.getCurrentEnvironment());

            return buildChatResponse("a constant value");
        });
        when(workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(any(), any(), anyLong())).thenReturn(List.of());
        when(workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(any(), any(), anyLong()))
            .thenReturn(Map.of());

        ObjectProvider<MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);

        when(meterRegistryProvider.getIfAvailable()).thenReturn(meterRegistry);

        PropertyCopilotGeneratorImpl generator = new PropertyCopilotGeneratorImpl(
            chatModel, evaluator, new PropertyCopilotPromptBuilder(), List.of(), workflowNodeOutputFacade,
            meterRegistryProvider, "", emptyCatalogChatClientResolverProvider());

        try {
            generator.generate(new PropertyCopilotRequest(
                "value", PropertyCopilotMode.TEXT, "wf1", "node2", "message", "STRING", true,
                Environment.STAGING.ordinal()));

            assertThat(observedEnvironment.get()).isEqualTo(Environment.STAGING);
            assertThat(EnvironmentContext.fetchCurrentEnvironment()).isNull();
        } finally {
            EnvironmentContext.clear();
        }
    }

    @Test
    void testTextModeReturnsValueVerbatim() {
        PropertyCopilotGeneratorImpl generator = generatorReturning("Hello ${trigger_1.firstName}");

        // pill resolves -> evaluator returns the substituted value (no surviving ${...})
        when(evaluator.evaluate(any(), any(), eq(true))).thenReturn(Map.of("value", "Hello Ada"));

        PropertyCopilotResult result = generator.generate(new PropertyCopilotRequest(
            "greet", PropertyCopilotMode.TEXT, "wf1", "node2", "message", "STRING", true, 0));

        assertThat(result.value()).isEqualTo("Hello ${trigger_1.firstName}");
        assertThat(result.valid()).isTrue();
        assertThat(result.message()).isNull();
    }

    @Test
    void testTextModeReturnsConstantWithoutPillsAsValid() {
        PropertyCopilotGeneratorImpl generator = generatorReturning("a constant value");

        PropertyCopilotResult result = generator.generate(new PropertyCopilotRequest(
            "value", PropertyCopilotMode.TEXT, "wf1", "node2", "message", "STRING", true, 0));

        assertThat(result.value()).isEqualTo("a constant value");
        assertThat(result.valid()).isTrue();
        assertThat(result.message()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testTextModeUnresolvedPillThenRepaired() {
        ChatModel chatModel = mock(ChatModel.class);
        ChatResponse bad = buildChatResponse("Hi ${missing.name}");
        ChatResponse good = buildChatResponse("Hi ${trigger_1.firstName}");

        when(chatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(bad, good);
        when(workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(any(), any(), anyLong())).thenReturn(List.of());
        when(workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(any(), any(), anyLong()))
            .thenReturn(Map.of());

        // first pill stays unresolved (${...} survives), second resolves
        when(evaluator.evaluate(any(), any(), eq(true)))
            .thenReturn(Map.of("value", "${missing.name}"))
            .thenReturn(Map.of("value", "Ada"));

        ObjectProvider<MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);

        when(meterRegistryProvider.getIfAvailable()).thenReturn(meterRegistry);

        PropertyCopilotGeneratorImpl generator = new PropertyCopilotGeneratorImpl(
            chatModel, evaluator, new PropertyCopilotPromptBuilder(), List.of(), workflowNodeOutputFacade,
            meterRegistryProvider, "", emptyCatalogChatClientResolverProvider());

        PropertyCopilotResult result = generator.generate(new PropertyCopilotRequest(
            "greet", PropertyCopilotMode.TEXT, "wf1", "node2", "message", "STRING", true, 0));

        assertThat(result.value()).isEqualTo("Hi ${trigger_1.firstName}");
        assertThat(result.valid()).isTrue();
    }

    @Test
    void testTextModeStillUnresolvedAfterRepairReturnsInvalid() {
        PropertyCopilotGeneratorImpl generator = generatorReturning("Hi ${missing.name}");

        when(evaluator.evaluate(any(), any(), eq(true))).thenReturn(Map.of("value", "${missing.name}"));

        PropertyCopilotResult result = generator.generate(new PropertyCopilotRequest(
            "greet", PropertyCopilotMode.TEXT, "wf1", "node2", "message", "STRING", true, 0));

        assertThat(result.value()).isEqualTo("Hi ${missing.name}");
        assertThat(result.valid()).isFalse();
        assertThat(result.message()).isNotBlank();
    }

    @Test
    void testFormulaModeStripsFencesAndValidates() {
        PropertyCopilotGeneratorImpl generator = generatorReturning("```\n=upperCase(${trigger_1.city})\n```");
        // evaluator does not throw -> valid
        when(evaluator.evaluate(any(), any(), eq(false))).thenReturn(Map.of("value", "PARIS"));

        PropertyCopilotResult result = generator.generate(new PropertyCopilotRequest(
            "uppercase city", PropertyCopilotMode.FORMULA, "wf1", "node2", "city", "STRING", true, 0));

        assertThat(result.value()).isEqualTo("=upperCase(${trigger_1.city})");
        assertThat(result.valid()).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testFormulaModeInvalidThenRepaired() {
        ChatModel chatModel = mock(ChatModel.class);
        ChatResponse bad = buildChatResponse("=bogus(");
        ChatResponse good = buildChatResponse("=concat(${a})");

        when(chatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(bad, good);
        when(workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(any(), any(), anyLong())).thenReturn(List.of());
        when(workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(any(), any(), anyLong()))
            .thenReturn(Map.of());

        // first validate throws, second succeeds
        when(evaluator.evaluate(any(), any(), eq(false)))
            .thenThrow(new RuntimeException("parse error"))
            .thenReturn(Map.of("value", "x"));

        ObjectProvider<MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);

        when(meterRegistryProvider.getIfAvailable()).thenReturn(meterRegistry);

        PropertyCopilotGeneratorImpl generator = new PropertyCopilotGeneratorImpl(
            chatModel, evaluator, new PropertyCopilotPromptBuilder(), List.of(), workflowNodeOutputFacade,
            meterRegistryProvider, "", emptyCatalogChatClientResolverProvider());

        PropertyCopilotResult result = generator.generate(new PropertyCopilotRequest(
            "concat a", PropertyCopilotMode.FORMULA, "wf1", "node2", "city", "STRING", true, 0));

        assertThat(result.value()).isEqualTo("=concat(${a})");
        assertThat(result.valid()).isTrue();
    }

    @Test
    void testFormulaModeStillInvalidAfterRepairReturnsInvalid() {
        PropertyCopilotGeneratorImpl generator = generatorReturning("=bogus(");

        when(evaluator.evaluate(any(), any(), eq(false))).thenThrow(new RuntimeException("parse error"));

        PropertyCopilotResult result = generator.generate(new PropertyCopilotRequest(
            "x", PropertyCopilotMode.FORMULA, "wf1", "node2", "city", "STRING", true, 0));

        assertThat(result.value()).isEqualTo("=bogus(");
        assertThat(result.valid()).isFalse();
        assertThat(result.message()).isNotBlank();
    }

    @Test
    void testJsonSchemaModeStripsFencesAndValidates() {
        PropertyCopilotGeneratorImpl generator = generatorReturning(
            "```json\n{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\"}}}\n```");

        PropertyCopilotResult result = generator.generate(new PropertyCopilotRequest(
            "order schema", PropertyCopilotMode.JSON_SCHEMA, "wf1", "node2", "responseSchema", "STRING", true, 0));

        assertThat(result.value()).isEqualTo("{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\"}}}");
        assertThat(result.valid()).isTrue();
        assertThat(result.message()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testJsonSchemaModeInvalidThenRepaired() {
        ChatModel chatModel = mock(ChatModel.class);
        ChatResponse bad = buildChatResponse("not json at all");
        ChatResponse good = buildChatResponse("{\"type\":\"object\",\"properties\":{}}");

        when(chatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(bad, good);
        when(workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(any(), any(), anyLong())).thenReturn(List.of());

        ObjectProvider<MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);

        when(meterRegistryProvider.getIfAvailable()).thenReturn(meterRegistry);

        PropertyCopilotGeneratorImpl generator = new PropertyCopilotGeneratorImpl(
            chatModel, evaluator, new PropertyCopilotPromptBuilder(), List.of(), workflowNodeOutputFacade,
            meterRegistryProvider);

        PropertyCopilotResult result = generator.generate(new PropertyCopilotRequest(
            "order schema", PropertyCopilotMode.JSON_SCHEMA, "wf1", "node2", "responseSchema", "STRING", true, 0));

        assertThat(result.value()).isEqualTo("{\"type\":\"object\",\"properties\":{}}");
        assertThat(result.valid()).isTrue();
    }

    @Test
    void testJsonSchemaModeStillInvalidAfterRepairReturnsInvalid() {
        PropertyCopilotGeneratorImpl generator = generatorReturning("definitely not json");

        PropertyCopilotResult result = generator.generate(new PropertyCopilotRequest(
            "order schema", PropertyCopilotMode.JSON_SCHEMA, "wf1", "node2", "responseSchema", "STRING", true, 0));

        assertThat(result.value()).isEqualTo("definitely not json");
        assertThat(result.valid()).isFalse();
        assertThat(result.message()).isNotBlank();
    }
}
