/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.agent.ChatModelUnavailableException;
import com.bytechef.ai.copilot.agent.OverrideChatClientResolver;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * @version ee
 * @author Ivica Cardic
 */
class PropertyCopilotGeneratorTest {

    private static final long DEVELOPMENT_ORDINAL = 0L;
    private static final long PRODUCTION_ORDINAL = 2L;

    private final ChatModel chatModel = mock(ChatModel.class);
    private final Evaluator evaluator = mock(Evaluator.class);
    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final OverrideChatClientResolver overrideChatClientResolver = mock(OverrideChatClientResolver.class);
    private final WorkflowNodeOutputFacade workflowNodeOutputFacade = mock(WorkflowNodeOutputFacade.class);

    private PropertyCopilotGeneratorImpl generator;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        EnvironmentContext.clear();

        when(overrideChatClientResolver.resolveDefaultChatModel(anyInt())).thenReturn(chatModel);
        when(workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(any(), any(), anyLong()))
            .thenReturn(List.of());
        when(workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(any(), any(), anyLong()))
            .thenReturn(Map.of());

        ObjectProvider<MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);

        when(meterRegistryProvider.getIfAvailable()).thenReturn(meterRegistry);

        generator = new PropertyCopilotGeneratorImpl(
            evaluator, new PropertyCopilotPromptBuilder(), List.of(), workflowNodeOutputFacade, meterRegistryProvider,
            overrideChatClientResolver);
    }

    @AfterEach
    void tearDown() {
        EnvironmentContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Nested
    class ChatModelResolutionTest {

        @Test
        void testGenerateUsesTheCatalogModelOfTheRequestEnvironment() {
            givenModelReplies("a constant value");

            PropertyCopilotResult result = generator.generate(textRequest(Environment.STAGING.ordinal()));

            assertThat(result.value()).isEqualTo("a constant value");
            verify(overrideChatClientResolver).resolveDefaultChatModel(Environment.STAGING.ordinal());
        }

        @Test
        void testGenerateFailsWhenNoProviderIsEnabled() {
            when(overrideChatClientResolver.resolveDefaultChatModel(anyInt()))
                .thenThrow(ChatModelUnavailableException.noProviderEnabled(1));

            assertThatThrownBy(() -> generator.generate(textRequest(1)))
                .isInstanceOf(ChatModelUnavailableException.class)
                .hasMessageContaining("No AI provider is enabled in environment 1");
        }

        @Test
        void testGenerateFailsWhenTheResolverHasNoCatalog() {
            when(overrideChatClientResolver.resolveDefaultChatModel(anyInt())).thenReturn(null);

            assertThatThrownBy(() -> generator.generate(textRequest(1)))
                .isInstanceOf(ChatModelUnavailableException.class)
                .hasMessageContaining("No AI provider is enabled in environment 1");
        }

        @Test
        void testGenerateBindsRequestEnvironmentDuringChatModelCall() {
            AtomicReference<Environment> observedEnvironment = new AtomicReference<>();

            when(chatModel.call(any(Prompt.class))).thenAnswer(invocation -> {
                observedEnvironment.set(EnvironmentContext.getCurrentEnvironment());

                return buildChatResponse("a constant value");
            });

            generator.generate(textRequest(Environment.STAGING.ordinal()));

            assertThat(observedEnvironment.get()).isEqualTo(Environment.STAGING);
            assertThat(EnvironmentContext.fetchCurrentEnvironment()).isNull();
        }
    }

    @Nested
    class PrincipalEnvironmentTest {

        @Test
        void testGenerateUsesConfinedPrincipalEnvironmentAtExecution() {
            authenticate(new TestApiKeyAuthenticationToken(PRODUCTION_ORDINAL, new User("connected-user-1", "",
                List.of())));

            givenModelReplies("a constant value");

            generator.generate(textRequest(DEVELOPMENT_ORDINAL));

            assertThat(captureSampleOutputEnvironmentId()).isEqualTo(PRODUCTION_ORDINAL);
            verify(overrideChatClientResolver).resolveDefaultChatModel(Math.toIntExact(PRODUCTION_ORDINAL));
        }

        @Test
        void testGenerateHonoursSessionPrincipalRequestedEnvironment() {
            authenticate(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

            givenModelReplies("a constant value");

            generator.generate(textRequest(DEVELOPMENT_ORDINAL));

            assertThat(captureSampleOutputEnvironmentId()).isEqualTo(DEVELOPMENT_ORDINAL);
        }

        private long captureSampleOutputEnvironmentId() {
            ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

            verify(workflowNodeOutputFacade).getPreviousWorkflowNodeSampleOutputs(
                eq("wf1"), eq("node2"), environmentIdCaptor.capture());

            return environmentIdCaptor.getValue();
        }
    }

    @Nested
    class TextModeTest {

        @Test
        void testTextModeReturnsValueVerbatim() {
            givenModelReplies("Hello ${trigger_1.firstName}");

            when(evaluator.evaluate(any(), any(), eq(true))).thenReturn(Map.of("value", "Hello Ada"));

            PropertyCopilotResult result = generator.generate(textRequest(0));

            assertThat(result).isEqualTo(PropertyCopilotResult.valid("Hello ${trigger_1.firstName}"));
        }

        @Test
        void testTextModeReturnsConstantWithoutPillsAsValid() {
            givenModelReplies("a constant value");

            PropertyCopilotResult result = generator.generate(textRequest(0));

            assertThat(result).isEqualTo(PropertyCopilotResult.valid("a constant value"));
        }

        @Test
        void testTextModeUnresolvedPillThenRepaired() {
            givenModelReplies("Hi ${missing.name}", "Hi ${trigger_1.firstName}");

            when(evaluator.evaluate(any(), any(), eq(true)))
                .thenReturn(Map.of("value", "${missing.name}"))
                .thenReturn(Map.of("value", "Ada"));

            PropertyCopilotResult result = generator.generate(textRequest(0));

            assertThat(result).isEqualTo(PropertyCopilotResult.valid("Hi ${trigger_1.firstName}"));
        }

        @Test
        void testTextModeStillUnresolvedAfterRepairReturnsInvalid() {
            givenModelReplies("Hi ${missing.name}");

            when(evaluator.evaluate(any(), any(), eq(true))).thenReturn(Map.of("value", "${missing.name}"));

            PropertyCopilotResult result = generator.generate(textRequest(0));

            assertThat(result.value()).isEqualTo("Hi ${missing.name}");
            assertThat(result.valid()).isFalse();
            assertThat(result.message()).isNotBlank();
        }

        @Test
        void testTextModeEmptyModelOutputIsInvalid() {
            givenModelReplies("```\n```");

            PropertyCopilotResult result = generator.generate(textRequest(0));

            assertThat(result.valid()).isFalse();
            assertThat(result.value()).isEmpty();
            assertThat(result.message()).contains("returned no value");
        }
    }

    @Nested
    class FormulaModeTest {

        @Test
        void testFormulaModeStripsFencesAndValidates() {
            givenModelReplies("```\n=upperCase(${trigger_1.city})\n```");

            when(evaluator.evaluate(any(), any(), eq(false))).thenReturn(Map.of("value", "PARIS"));

            PropertyCopilotResult result = generator.generate(formulaRequest());

            assertThat(result).isEqualTo(PropertyCopilotResult.valid("=upperCase(${trigger_1.city})"));
        }

        @Test
        void testFormulaModeInvalidThenRepaired() {
            givenModelReplies("=bogus(", "=concat(${a})");

            when(evaluator.evaluate(any(), any(), eq(false)))
                .thenThrow(new IllegalArgumentException("Unparseable formula expression"))
                .thenReturn(Map.of("value", "x"));

            PropertyCopilotResult result = generator.generate(formulaRequest());

            assertThat(result).isEqualTo(PropertyCopilotResult.valid("=concat(${a})"));
        }

        @Test
        void testFormulaModeStillInvalidAfterRepairReturnsInvalid() {
            givenModelReplies("=bogus(");

            when(evaluator.evaluate(any(), any(), eq(false)))
                .thenThrow(new IllegalArgumentException("Unparseable formula expression"));

            PropertyCopilotResult result = generator.generate(formulaRequest());

            assertThat(result.value()).isEqualTo("=bogus(");
            assertThat(result.valid()).isFalse();
            assertThat(result.message()).isNotBlank();
        }

        @Test
        void testFormulaModeDoesNotSwallowUnexpectedEvaluatorFailures() {
            givenModelReplies("=upperCase(${trigger_1.city})");

            when(evaluator.evaluate(any(), any(), eq(false))).thenThrow(new IllegalStateException("evaluator broken"));

            assertThatThrownBy(() -> generator.generate(formulaRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("evaluator broken");
        }

        @Test
        void testFormulaModeEmptyModelOutputIsInvalid() {
            givenModelReplies("");

            PropertyCopilotResult result = generator.generate(formulaRequest());

            assertThat(result.valid()).isFalse();
            assertThat(result.value()).isEmpty();
            assertThat(result.message()).contains("returned no value");
        }
    }

    private void givenModelReplies(String firstReply, String... nextReplies) {
        ChatResponse[] nextResponses = new ChatResponse[nextReplies.length];

        for (int index = 0; index < nextReplies.length; index++) {
            nextResponses[index] = buildChatResponse(nextReplies[index]);
        }

        when(chatModel.call(any(Prompt.class))).thenReturn(buildChatResponse(firstReply), nextResponses);
    }

    private static ChatResponse buildChatResponse(String text) {
        return ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage(text))))
            .build();
    }

    private static PropertyCopilotRequest textRequest(long environmentId) {
        return new PropertyCopilotRequest(
            "value", PropertyCopilotMode.TEXT, "wf1", "node2", "message", "STRING", true, environmentId);
    }

    private static PropertyCopilotRequest formulaRequest() {
        return new PropertyCopilotRequest(
            "uppercase city", PropertyCopilotMode.FORMULA, "wf1", "node2", "city", "STRING", true, 0);
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.getContext()
            .setAuthentication(authentication);
    }

    private static final class TestApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

        private TestApiKeyAuthenticationToken(long environmentId, User user) {
            super(environmentId, user);
        }
    }
}
