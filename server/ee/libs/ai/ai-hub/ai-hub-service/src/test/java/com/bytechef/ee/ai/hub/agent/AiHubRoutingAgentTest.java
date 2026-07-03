/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.RunAgentParameters;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.BaseMessage;
import com.agui.core.state.State;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgent;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentResource;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentResourceKind;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentService;
import com.bytechef.ee.ai.hub.task.AiHubTask;
import com.bytechef.ee.ai.hub.task.AiHubTaskKind;
import com.bytechef.ee.ai.hub.task.AiHubTaskService;
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AiHubRoutingAgent}. The router's only behaviour worth pinning is "the right kind goes to the
 * right agent" — but the regression cost of getting that wrong is huge (every task routes to the wrong agent and unit
 * tests of the leaf agents wouldn't catch it). The tests below pin every branch of the dispatch decision tree,
 * including the fallback paths that fire when the bridge bean is absent or the task lookup fails.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubRoutingAgentTest {

    private static final String AGENT_ID = "ai_hub_ask";
    private static final String THREAD_ID = "thread-1";

    private AiHubSpringAIAgent llmAgent;
    private WebhookBridgeAgent webhookBridgeAgent;
    private AiHubTaskService taskService;
    private AgentSubscriber subscriber;
    private AssetFileFacade assetFileFacade;

    @BeforeEach
    void setUp() {
        llmAgent = mock(AiHubSpringAIAgent.class);
        webhookBridgeAgent = mock(WebhookBridgeAgent.class);
        taskService = mock(AiHubTaskService.class);
        subscriber = mock(AgentSubscriber.class);
        assetFileFacade = mock(AssetFileFacade.class);

        when(llmAgent.runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class)))
            .thenReturn(CompletableFuture.completedFuture(null));
        when(webhookBridgeAgent.runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class)))
            .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void testWorkflowChatTaskDispatchesToBridgeAgent() throws AGUIException {
        AiHubTask task = mock(AiHubTask.class);

        when(task.getKind()).thenReturn(AiHubTaskKind.WORKFLOW_CHAT);
        when(taskService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(task));

        AiHubRoutingAgent agent =
            new AiHubRoutingAgent(AGENT_ID, llmAgent, webhookBridgeAgent, taskService,
                assetFileFacade, null);

        agent.runAgent(parametersOf(THREAD_ID), subscriber)
            .join();

        verify(webhookBridgeAgent).runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class));
        verify(llmAgent, never()).runAgent(any(), any());
    }

    @Test
    void testStandardTaskDispatchesToLlmAgent() throws AGUIException {
        AiHubTask task = mock(AiHubTask.class);

        when(task.getKind()).thenReturn(AiHubTaskKind.STANDARD);
        when(taskService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(task));

        AiHubRoutingAgent agent =
            new AiHubRoutingAgent(AGENT_ID, llmAgent, webhookBridgeAgent, taskService,
                assetFileFacade, null);

        agent.runAgent(parametersOf(THREAD_ID), subscriber)
            .join();

        verify(llmAgent).runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class));
        verify(webhookBridgeAgent, never()).runAgent(any(), any());
    }

    @Test
    void testMissingTaskFallsBackToLlmAgent() throws AGUIException {
        // When the threadId doesn't map to a task row (race with delete, unknown threadId), the router
        // falls through to the LLM agent rather than throwing — the LLM agent's own error handling will
        // surface the missing-task case if it matters. Pin: bridge is NOT invoked.
        when(taskService.findByThreadId(THREAD_ID)).thenReturn(Optional.empty());

        AiHubRoutingAgent agent =
            new AiHubRoutingAgent(AGENT_ID, llmAgent, webhookBridgeAgent, taskService,
                assetFileFacade, null);

        agent.runAgent(parametersOf(THREAD_ID), subscriber)
            .join();

        verify(llmAgent).runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class));
        verify(webhookBridgeAgent, never()).runAgent(any(), any());
    }

    @Test
    void testWorkflowChatFallsBackToLlmAgentWhenBridgeBeanAbsent() throws AGUIException {
        // Deployments without the webhook coordinator don't register a WebhookBridgeAgent bean. Without the
        // fallback the user would stare at a hung stream forever. Falling back to the LLM agent surfaces
        // SOMETHING — sub-optimal but recoverable. Pin: WORKFLOW_CHAT + null bridge → LLM agent.
        AiHubTask task = mock(AiHubTask.class);

        when(task.getKind()).thenReturn(AiHubTaskKind.WORKFLOW_CHAT);
        when(taskService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(task));

        AiHubRoutingAgent agent =
            new AiHubRoutingAgent(AGENT_ID, llmAgent, null, taskService, assetFileFacade, null);

        agent.runAgent(parametersOf(THREAD_ID), subscriber)
            .join();

        verify(llmAgent).runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class));
    }

    @Test
    void testNullThreadIdRoutesToLlmAgentWithoutLookup() throws AGUIException {
        // Defensive: a malformed RunAgentParameters with no threadId should NOT trigger a database lookup —
        // findByThreadId(null) would explode. The router's resolveKind short-circuits on blank/null threadId
        // and returns STANDARD (the safe default). Pin: taskService is NEVER called.
        AiHubRoutingAgent agent =
            new AiHubRoutingAgent(AGENT_ID, llmAgent, webhookBridgeAgent, taskService,
                assetFileFacade, null);

        agent.runAgent(parametersOf(null), subscriber)
            .join();

        verify(taskService, never()).findByThreadId(any());
        verify(llmAgent).runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class));
    }

    @Test
    void testAiHubPersonalAgentTaskDispatchesToLlmAgentWithInstructionsOverlay()
        throws AGUIException {
        // PERSONAL_AGENT runs through the same LLM agent as STANDARD, but with the agent's instructions
        // injected into per-turn State so createSystemMessage picks them up. Pin both: target is the
        // LLM agent (NOT the bridge), and the State carries the instructions + title keys.
        AiHubTask task = mock(AiHubTask.class);

        when(task.getKind()).thenReturn(AiHubTaskKind.PERSONAL_AGENT);
        when(task.getId()).thenReturn(11L);
        when(taskService.getWorkspaceId(task.getId())).thenReturn(7L);
        when(task.getUserId()).thenReturn(3L);
        when(task.getAiHubPersonalAgentId()).thenReturn(42L);
        when(taskService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(task));

        AiHubPersonalAgent agent = mock(AiHubPersonalAgent.class);

        when(agent.getInstructions()).thenReturn("Always cite sources.");
        when(agent.getTitle()).thenReturn("Research Assistant");

        AiHubPersonalAgentService aiHubPersonalAgentService =
            mock(AiHubPersonalAgentService.class);

        when(aiHubPersonalAgentService.findOwned(42L, 7L, 3L)).thenReturn(Optional.of(agent));

        AiHubRoutingAgent routingAgent =
            new AiHubRoutingAgent(AGENT_ID, llmAgent, webhookBridgeAgent, taskService,
                assetFileFacade, aiHubPersonalAgentService);

        State state = new State();

        routingAgent
            .runAgent(RunAgentParameters.builder()
                .threadId(THREAD_ID)
                .runId("run-1")
                .messages(List.<BaseMessage>of())
                .state(state)
                .build(), subscriber)
            .join();

        verify(llmAgent).runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class));
        verify(webhookBridgeAgent, never()).runAgent(any(), any());

        // The state mutation is the load-bearing contract: AiHubSpringAIAgent.createSystemMessage
        // reads PERSONAL_AGENT_INSTRUCTIONS_KEY and PERSONAL_AGENT_TITLE_KEY from state. If the router
        // forgets to set them, the agent runs as plain LLM and the user wonders why their custom
        // instructions are being ignored.
        assertThat(state.get(AiHubStateKeys.PERSONAL_AGENT_INSTRUCTIONS_KEY))
            .isEqualTo("Always cite sources.");
        assertThat(state.get(AiHubStateKeys.PERSONAL_AGENT_TITLE_KEY))
            .isEqualTo("Research Assistant");
    }

    @Test
    void testAiHubPersonalAgentTaskWithBlankInstructionsSetsTitleOnly() throws AGUIException {
        // Edge case: an agent with a title set but instructions left blank. The router skips the
        // instructions overlay (nothing to inject) but still sets the title key so the LLM can refer to
        // itself by name in replies. Pin both: title key set, instructions key NOT set — the agent's
        // identity surfaces without a hollow "Agent-specific instructions:" block in the prompt.
        AiHubTask task = mock(AiHubTask.class);

        when(task.getKind()).thenReturn(AiHubTaskKind.PERSONAL_AGENT);
        when(task.getId()).thenReturn(11L);
        when(taskService.getWorkspaceId(task.getId())).thenReturn(7L);
        when(task.getUserId()).thenReturn(3L);
        when(task.getAiHubPersonalAgentId()).thenReturn(42L);
        when(taskService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(task));

        AiHubPersonalAgent agent = mock(AiHubPersonalAgent.class);

        // Instructions intentionally blank — exercises the early-return branch in
        // applyAiHubPersonalAgentOverlay.
        when(agent.getInstructions()).thenReturn("   ");
        when(agent.getTitle()).thenReturn("Research Assistant");

        AiHubPersonalAgentService aiHubPersonalAgentService =
            mock(AiHubPersonalAgentService.class);

        when(aiHubPersonalAgentService.findOwned(42L, 7L, 3L)).thenReturn(Optional.of(agent));

        AiHubRoutingAgent routingAgent =
            new AiHubRoutingAgent(AGENT_ID, llmAgent, webhookBridgeAgent, taskService,
                assetFileFacade, aiHubPersonalAgentService);

        State state = new State();

        routingAgent
            .runAgent(RunAgentParameters.builder()
                .threadId(THREAD_ID)
                .runId("run-1")
                .messages(List.<BaseMessage>of())
                .state(state)
                .build(), subscriber)
            .join();

        verify(llmAgent).runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class));

        // Title-only overlay: title key present so the LLM has the agent's name, instructions key absent so
        // the prompt doesn't carry an empty "Agent-specific instructions:" block. Both pins matter — without
        // the title, the LLM has no agent identity; without skipping the empty instructions, the prompt
        // contains a misleading hint that the agent has rules to follow.
        assertThat(state.get(AiHubStateKeys.PERSONAL_AGENT_TITLE_KEY))
            .isEqualTo("Research Assistant");
        assertThat(state.get(AiHubStateKeys.PERSONAL_AGENT_INSTRUCTIONS_KEY))
            .isNull();
    }

    @Test
    void testAiHubPersonalAgentTaskFallsBackToPlainLlmWhenServiceBeanAbsent() throws AGUIException {
        // Deployments without the personal-agent feature don't register a AiHubPersonalAgentService bean. The
        // routing agent's constructor accepts null for that dependency and the runAgent path must degrade
        // gracefully — run the task as plain LLM (no instructions overlay) instead of throwing
        // an NPE. Same recoverable-degradation pattern as the missing-bridge fallback above.
        AiHubTask task = mock(AiHubTask.class);

        when(task.getKind()).thenReturn(AiHubTaskKind.PERSONAL_AGENT);
        when(task.getId()).thenReturn(11L);
        when(taskService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(task));

        // Construct WITHOUT a AiHubPersonalAgentService — the last-arg null mirrors a deployment that didn't
        // wire the bean. The other null is for webhookBridgeAgent which we set explicitly so this test
        // doesn't conflate the two missing-bean paths.
        AiHubRoutingAgent routingAgent =
            new AiHubRoutingAgent(AGENT_ID, llmAgent, webhookBridgeAgent, taskService,
                assetFileFacade, null);

        State state = new State();

        routingAgent
            .runAgent(RunAgentParameters.builder()
                .threadId(THREAD_ID)
                .runId("run-1")
                .messages(List.<BaseMessage>of())
                .state(state)
                .build(), subscriber)
            .join();

        // Plain LLM still runs — no exception, no overlay keys leaked into state. The user sees an
        // unflavoured response but the task works.
        verify(llmAgent).runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class));
        assertThat(state.get(AiHubStateKeys.PERSONAL_AGENT_INSTRUCTIONS_KEY))
            .isNull();
        assertThat(state.get(AiHubStateKeys.PERSONAL_AGENT_TITLE_KEY))
            .isNull();
    }

    @Test
    void testAiHubPersonalAgentTaskFallsBackToPlainLlmWhenAgentDeleted() throws AGUIException {
        // The agent was deleted but the task row still references it (data race). The router
        // gracefully degrades — runs the task as plain LLM instead of throwing — and the
        // overlay keys stay absent from state. The user sees an unflavoured response, not a crashed turn.
        AiHubTask task = mock(AiHubTask.class);

        when(task.getKind()).thenReturn(AiHubTaskKind.PERSONAL_AGENT);
        when(task.getId()).thenReturn(11L);
        when(taskService.getWorkspaceId(task.getId())).thenReturn(7L);
        when(task.getUserId()).thenReturn(3L);
        when(task.getAiHubPersonalAgentId()).thenReturn(42L);
        when(taskService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(task));

        AiHubPersonalAgentService aiHubPersonalAgentService =
            mock(AiHubPersonalAgentService.class);

        when(aiHubPersonalAgentService.findOwned(42L, 7L, 3L)).thenReturn(Optional.empty());

        AiHubRoutingAgent routingAgent =
            new AiHubRoutingAgent(AGENT_ID, llmAgent, webhookBridgeAgent, taskService,
                assetFileFacade, aiHubPersonalAgentService);

        State state = new State();

        routingAgent
            .runAgent(RunAgentParameters.builder()
                .threadId(THREAD_ID)
                .runId("run-1")
                .messages(List.<BaseMessage>of())
                .state(state)
                .build(), subscriber)
            .join();

        verify(llmAgent).runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class));
        assertThat(state.get(AiHubStateKeys.PERSONAL_AGENT_INSTRUCTIONS_KEY))
            .isNull();
    }

    @Test
    void testMergeReferencedResourcesAppendsAgentResourcesWithComposerKinds() {
        List<AiHubPersonalAgentResource> agentResources = List.of(
            new AiHubPersonalAgentResource(1L, AiHubPersonalAgentResourceKind.WORKFLOW, "wf-1", "Daily standup"),
            new AiHubPersonalAgentResource(1L, AiHubPersonalAgentResourceKind.DATA_TABLE, "dt-9", "Customers"));

        List<Object> merged = AiHubRoutingAgent.mergeReferencedResources(agentResources, null);

        assertThat(merged).hasSize(2);
        assertThat(merged.get(0)).isEqualTo(Map.of("kind", "workflow", "id", "wf-1", "name", "Daily standup"));
        assertThat(merged.get(1)).isEqualTo(Map.of("kind", "dataTable", "id", "dt-9", "name", "Customers"));
    }

    @Test
    void testMergeReferencedResourcesKeepsClientEntriesAndDedupsByKindAndId() {
        // The client already @-mentioned wf-1 this turn; the agent also pins wf-1. The merged list keeps the
        // client entry and does NOT add a duplicate, but still appends the agent's other resource.
        List<Map<String, Object>> clientSent = List.of(
            Map.of("kind", "workflow", "id", "wf-1", "name", "Daily standup"));

        List<AiHubPersonalAgentResource> agentResources = List.of(
            new AiHubPersonalAgentResource(1L, AiHubPersonalAgentResourceKind.WORKFLOW, "wf-1", "Daily standup"),
            new AiHubPersonalAgentResource(1L, AiHubPersonalAgentResourceKind.FILE, "file-2", "notes.md"));

        List<Object> merged = AiHubRoutingAgent.mergeReferencedResources(agentResources, clientSent);

        assertThat(merged).hasSize(2);
        assertThat(merged.get(0)).isEqualTo(Map.of("kind", "workflow", "id", "wf-1", "name", "Daily standup"));
        assertThat(merged.get(1)).isEqualTo(Map.of("kind", "file", "id", "file-2", "name", "notes.md"));
    }

    @Test
    void testMergeReferencedResourcesWithEmptyAgentResourcesReturnsClientEntriesUnchanged() {
        List<Map<String, Object>> clientSent = List.of(
            Map.of("kind", "file", "id", "file-5", "name", "spec.md"));

        List<Object> merged = AiHubRoutingAgent.mergeReferencedResources(List.of(), clientSent);

        assertThat(merged).containsExactlyElementsOf(clientSent);
    }

    @Test
    void testAiHubPersonalAgentTaskMergesAgentResourcesIntoState() throws AGUIException {
        AiHubTask task = mock(AiHubTask.class);

        when(task.getKind()).thenReturn(AiHubTaskKind.PERSONAL_AGENT);
        when(task.getId()).thenReturn(100L);
        when(task.getUserId()).thenReturn(3L);
        when(task.getAiHubPersonalAgentId()).thenReturn(42L);
        when(taskService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(task));
        when(taskService.getWorkspaceId(task.getId())).thenReturn(7L);

        AiHubPersonalAgent agent = mock(AiHubPersonalAgent.class);

        when(agent.getInstructions()).thenReturn("Be concise.");
        when(agent.getTitle()).thenReturn("Researcher");
        when(agent.hasLlmOverride()).thenReturn(false);

        AiHubPersonalAgentService aiHubPersonalAgentService = mock(AiHubPersonalAgentService.class);

        when(aiHubPersonalAgentService.findOwned(42L, 7L, 3L)).thenReturn(Optional.of(agent));
        when(aiHubPersonalAgentService.listResources(42L)).thenReturn(List.of(
            new AiHubPersonalAgentResource(42L, AiHubPersonalAgentResourceKind.WORKFLOW, "wf-1", "Daily standup")));

        AiHubRoutingAgent routingAgent = new AiHubRoutingAgent(
            AGENT_ID, llmAgent, webhookBridgeAgent, taskService, assetFileFacade, aiHubPersonalAgentService);

        State state = new State();

        routingAgent.runAgent(
            RunAgentParameters.builder()
                .threadId(THREAD_ID)
                .runId("run-1")
                .messages(List.<BaseMessage>of())
                .state(state)
                .build(),
            subscriber)
            .join();

        Object referencedResources = state.get("referencedResources");

        assertThat(referencedResources).isInstanceOf(List.class);

        @SuppressWarnings("unchecked")
        List<Object> resourceList = (List<Object>) referencedResources;

        assertThat(resourceList).hasSize(1);
        assertThat(resourceList.get(0))
            .isEqualTo(Map.of("kind", "workflow", "id", "wf-1", "name", "Daily standup"));
    }

    private static RunAgentParameters parametersOf(String threadId) {
        RunAgentParameters.Builder builder = RunAgentParameters.builder()
            .runId("run-1")
            .messages(List.<BaseMessage>of());

        if (threadId != null) {
            builder.threadId(threadId);
        }

        return builder.build();
    }
}
