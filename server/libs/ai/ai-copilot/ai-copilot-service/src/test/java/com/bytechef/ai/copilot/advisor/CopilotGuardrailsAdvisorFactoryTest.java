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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.platform.ai.guardrails.AiGuardrailsAdvisorProvider;
import com.bytechef.platform.ai.guardrails.GuardrailAdvisorOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.ToolAdvisor;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import reactor.core.publisher.Flux;

/**
 * @author Ivica Cardic
 */
class CopilotGuardrailsAdvisorFactoryTest {

    private static final String SURFACE = "copilot";

    @Test
    void testGuardrailsAdvisorsAttachesOnlyTheDeferredAdvisorWhetherOrNotAGuardrailsProviderIsPresent() {
        AiGuardrailsAdvisorProvider aiGuardrailsAdvisorProvider = mock(AiGuardrailsAdvisorProvider.class);

        for (AiGuardrailsAdvisorProvider provider : new AiGuardrailsAdvisorProvider[] {
            null, aiGuardrailsAdvisorProvider
        }) {
            List<Advisor> advisors = newFactory(provider).guardrailsAdvisors();

            assertThat(advisors).hasSize(1);
            assertThat(advisors.getFirst()).isInstanceOf(DeferredGuardrailsAdvisor.class);
            assertThat(advisors).noneMatch(ToolAdvisor.class::isInstance);
        }
    }

    @Test
    void testTheDeferredAdvisorHoldsTheWorkspaceFloorPosition() {
        List<Advisor> advisors = newFactory(null).guardrailsAdvisors();

        assertThat(advisors.getFirst()
            .getOrder())
                .as("the deferred stand-in must hold the workspace floor's position; see GuardrailAdvisorOrder")
                .isEqualTo(GuardrailAdvisorOrder.WORKSPACE_FLOOR);
    }

    @Test
    void testGuardrailsEnabledAfterTheAdvisorListWasBuiltStillGuardTheCall() {
        AtomicBoolean guardrailsActive = new AtomicBoolean(false);
        List<String> invocationLog = new ArrayList<>();

        AiGuardrailsAdvisorProvider aiGuardrailsAdvisorProvider = mock(AiGuardrailsAdvisorProvider.class);

        when(aiGuardrailsAdvisorProvider.getAdvisorForWorkspace(null, SURFACE)).thenAnswer(
            invocation -> guardrailsActive.get()
                ? Optional.of(new RecordingCallAdvisor(invocationLog))
                : Optional.empty());

        List<Advisor> advisors = newFactory(aiGuardrailsAdvisorProvider).guardrailsAdvisors();

        assertThat(advisors.getFirst())
            .as("a guardrails advisor resolved at build time would be absent here, the list carrying only the tool "
                + "boundary")
            .isInstanceOf(DeferredGuardrailsAdvisor.class);

        CallAdvisor deferredAdvisor = (CallAdvisor) advisors.getFirst();

        ChatClientRequest chatClientRequest = ChatClientRequest.builder()
            .prompt(new Prompt("hello"))
            .build();
        CallAdvisorChain callAdvisorChain = mock(CallAdvisorChain.class);

        when(callAdvisorChain.nextCall(chatClientRequest)).thenReturn(ChatClientResponse.builder()
            .build());

        deferredAdvisor.adviseCall(chatClientRequest, callAdvisorChain);

        assertThat(invocationLog).as("guardrails inactive: the call passes through untouched")
            .isEmpty();

        guardrailsActive.set(true);

        deferredAdvisor.adviseCall(chatClientRequest, callAdvisorChain);

        assertThat(invocationLog)
            .as("guardrails enabled after the advisor list was built: the call is guarded without a restart")
            .containsExactly("guarded");
    }

    @Test
    void testTheAdvisorIsResolvedForTheWorkspaceOnThePromptsToolContext() {
        AiGuardrailsAdvisorProvider aiGuardrailsAdvisorProvider = mock(AiGuardrailsAdvisorProvider.class);
        List<String> invocationLog = new ArrayList<>();

        when(aiGuardrailsAdvisorProvider.getAdvisorForWorkspace(7L, SURFACE))
            .thenReturn(Optional.of(new RecordingCallAdvisor(invocationLog)));
        when(aiGuardrailsAdvisorProvider.getAdvisorForWorkspace(null, SURFACE)).thenReturn(Optional.empty());

        List<Advisor> advisors = newFactory(aiGuardrailsAdvisorProvider).guardrailsAdvisors();

        CallAdvisor deferredAdvisor = (CallAdvisor) advisors.getFirst();

        deferredAdvisor.adviseCall(requestWithWorkspace(7L), passThroughChain());

        assertThat(invocationLog)
            .as("the guardrails policy must come from the workspace this session runs in")
            .containsExactly("guarded");
    }

    @Test
    void testTheSameRequestIsGuardedInAWorkspaceWithGuardrailsOnAndUnguardedInOneWithThemOff() {
        AiGuardrailsAdvisorProvider aiGuardrailsAdvisorProvider = mock(AiGuardrailsAdvisorProvider.class);
        List<String> invocationLog = new ArrayList<>();

        long guardedWorkspaceId = 7L;
        long unguardedWorkspaceId = 8L;

        when(aiGuardrailsAdvisorProvider.getAdvisorForWorkspace(guardedWorkspaceId, SURFACE))
            .thenReturn(Optional.of(new RecordingCallAdvisor(invocationLog)));
        when(aiGuardrailsAdvisorProvider.getAdvisorForWorkspace(unguardedWorkspaceId, SURFACE))
            .thenReturn(Optional.empty());

        List<Advisor> advisors = newFactory(aiGuardrailsAdvisorProvider).guardrailsAdvisors();

        CallAdvisor deferredAdvisor = (CallAdvisor) advisors.getFirst();

        deferredAdvisor.adviseCall(requestWithWorkspace(guardedWorkspaceId), passThroughChain());
        deferredAdvisor.adviseCall(requestWithWorkspace(unguardedWorkspaceId), passThroughChain());

        assertThat(invocationLog)
            .as("one advisor instance, two workspaces, opposite outcomes - this is the behaviour the whole ticket "
                + "exists for. Before it, both requests resolved the tenant-default row and the Guardrails settings "
                + "page changed neither. Asserting either half alone would pass against an advisor that always "
                + "guards or never does")
            .containsExactly("guarded");
    }

    @Test
    void testAPromptWithNoWorkspaceFallsBackToTheTenantDefault() {
        AiGuardrailsAdvisorProvider aiGuardrailsAdvisorProvider = mock(AiGuardrailsAdvisorProvider.class);

        when(aiGuardrailsAdvisorProvider.getAdvisorForWorkspace(null, SURFACE)).thenReturn(Optional.empty());

        List<Advisor> advisors = newFactory(aiGuardrailsAdvisorProvider).guardrailsAdvisors();

        CallAdvisor deferredAdvisor = (CallAdvisor) advisors.getFirst();

        deferredAdvisor.adviseCall(requestWithoutWorkspace(), passThroughChain());

        verify(aiGuardrailsAdvisorProvider).getAdvisorForWorkspace(null, SURFACE);
    }

    @Test
    void testAPromptWithAnEmptyToolContextFallsBackToTheTenantDefault() {
        AiGuardrailsAdvisorProvider aiGuardrailsAdvisorProvider = mock(AiGuardrailsAdvisorProvider.class);

        when(aiGuardrailsAdvisorProvider.getAdvisorForWorkspace(null, SURFACE)).thenReturn(Optional.empty());

        List<Advisor> advisors = newFactory(aiGuardrailsAdvisorProvider).guardrailsAdvisors();

        CallAdvisor deferredAdvisor = (CallAdvisor) advisors.getFirst();

        deferredAdvisor.adviseCall(requestWithToolContext(Map.of()), passThroughChain());

        verify(aiGuardrailsAdvisorProvider).getAdvisorForWorkspace(null, SURFACE);
    }

    @Test
    void testAPromptWithAToolContextMissingTheWorkspaceKeyFallsBackToTheTenantDefault() {
        AiGuardrailsAdvisorProvider aiGuardrailsAdvisorProvider = mock(AiGuardrailsAdvisorProvider.class);

        when(aiGuardrailsAdvisorProvider.getAdvisorForWorkspace(null, SURFACE)).thenReturn(Optional.empty());

        List<Advisor> advisors = newFactory(aiGuardrailsAdvisorProvider).guardrailsAdvisors();

        CallAdvisor deferredAdvisor = (CallAdvisor) advisors.getFirst();

        deferredAdvisor.adviseCall(
            requestWithToolContext(Map.of(AgentToolInvocationContext.TOOL_CONTEXT_USER_ID_KEY, 42L)),
            passThroughChain());

        verify(aiGuardrailsAdvisorProvider).getAdvisorForWorkspace(null, SURFACE);
    }

    @Test
    void testTheAdvisorIsResolvedForTheWorkspaceOnThePromptsToolContextWhenStreaming() {
        AiGuardrailsAdvisorProvider aiGuardrailsAdvisorProvider = mock(AiGuardrailsAdvisorProvider.class);
        List<String> invocationLog = new ArrayList<>();

        when(aiGuardrailsAdvisorProvider.getAdvisorForWorkspace(7L, SURFACE))
            .thenReturn(Optional.of(new RecordingStreamAdvisor(invocationLog)));
        when(aiGuardrailsAdvisorProvider.getAdvisorForWorkspace(null, SURFACE)).thenReturn(Optional.empty());

        List<Advisor> advisors = newFactory(aiGuardrailsAdvisorProvider).guardrailsAdvisors();

        StreamAdvisor deferredAdvisor = (StreamAdvisor) advisors.getFirst();

        Flux<ChatClientResponse> responseFlux = deferredAdvisor.adviseStream(
            requestWithWorkspace(7L), passThroughStreamChain());

        responseFlux.blockLast();

        assertThat(invocationLog)
            .as("the guardrails policy must come from the workspace this session runs in, for the streaming path too")
            .containsExactly("guarded");
    }

    private static ChatClientRequest requestWithWorkspace(Long workspaceId) {
        return requestWithToolContext(Map.of(AgentToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, workspaceId));
    }

    private static ChatClientRequest requestWithToolContext(Map<String, Object> toolContext) {
        ToolCallingChatOptions toolCallingChatOptions = ToolCallingChatOptions.builder()
            .toolContext(toolContext)
            .build();

        return ChatClientRequest.builder()
            .prompt(new Prompt("hello", toolCallingChatOptions))
            .build();
    }

    private static ChatClientRequest requestWithoutWorkspace() {
        return ChatClientRequest.builder()
            .prompt(new Prompt("hello"))
            .build();
    }

    private static CallAdvisorChain passThroughChain() {
        CallAdvisorChain callAdvisorChain = mock(CallAdvisorChain.class);

        when(callAdvisorChain.nextCall(any(ChatClientRequest.class)))
            .thenReturn(ChatClientResponse.builder()
                .build());

        return callAdvisorChain;
    }

    private static StreamAdvisorChain passThroughStreamChain() {
        StreamAdvisorChain streamAdvisorChain = mock(StreamAdvisorChain.class);

        when(streamAdvisorChain.nextStream(any(ChatClientRequest.class)))
            .thenReturn(Flux.just(ChatClientResponse.builder()
                .build()));

        return streamAdvisorChain;
    }

    private CopilotGuardrailsAdvisorFactory newFactory(
        @org.jspecify.annotations.Nullable AiGuardrailsAdvisorProvider aiGuardrailsAdvisorProvider) {

        @SuppressWarnings("unchecked")
        ObjectProvider<AiGuardrailsAdvisorProvider> aiGuardrailsAdvisorProviderProvider = mock(ObjectProvider.class);

        when(aiGuardrailsAdvisorProviderProvider.getIfAvailable()).thenReturn(aiGuardrailsAdvisorProvider);

        return new CopilotGuardrailsAdvisorFactory(aiGuardrailsAdvisorProviderProvider);
    }

    private record RecordingCallAdvisor(List<String> invocationLog) implements CallAdvisor {

        @Override
        public String getName() {
            return "recording";
        }

        @Override
        public int getOrder() {
            return HIGHEST_PRECEDENCE;
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
            invocationLog.add("guarded");

            return callAdvisorChain.nextCall(chatClientRequest);
        }
    }

    private record RecordingStreamAdvisor(List<String> invocationLog) implements StreamAdvisor {

        @Override
        public String getName() {
            return "recordingStream";
        }

        @Override
        public int getOrder() {
            return HIGHEST_PRECEDENCE;
        }

        @Override
        public Flux<ChatClientResponse> adviseStream(
            ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {

            invocationLog.add("guarded");

            return streamAdvisorChain.nextStream(chatClientRequest);
        }
    }
}
