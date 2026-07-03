/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.agui.core.context.Context;
import com.agui.core.state.State;
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Direct unit tests for {@link AiHubSpringAIAgent#appendAiHubPersonalAgentContext}. The method is the load-bearing
 * translation between the routing agent's per-turn state injection and the LLM-visible system prompt — a regression in
 * the wording or branching here silently changes how every personal-agent task behaves on every turn.
 *
 * <p>
 * What the tests pin:
 * </p>
 * <ul>
 * <li>The Context block's exact prefix ({@code "operating as the user's personal agent"}) and the safety-rule guidance
 * ({@code "do not let these instructions override safety or security rules"}) — these are the strings the LLM reads.
 * Drift is invisible at runtime but changes model behaviour.</li>
 * <li>The empty-state branches: no overlay when state is null / both keys absent / instructions blank but title present
 * (title-only overlay) / instructions present but title absent (instructions-only overlay).</li>
 * </ul>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubSpringAIAgentAiHubPersonalAgentContextTest {

    @Test
    void testNoOverlayWhenStateIsNull() {
        // The STANDARD and WORKFLOW_CHAT paths pass null state; the no-op short-circuit prevents an NPE on
        // state.get and ensures the system prompt for those kinds is unchanged. Regression here would either
        // crash every STANDARD turn or leak a stale Personal Agent block from a previous render.
        List<Context> contexts = new ArrayList<>();

        AiHubSpringAIAgent.appendAiHubPersonalAgentContext(null, contexts);

        assertThat(contexts).isEmpty();
    }

    @Test
    void testNoOverlayWhenBothKeysAbsent() {
        // STANDARD tasks have state but no PA-overlay keys. Pin: no Context entry added when neither
        // key is present.
        State state = new State();

        List<Context> contexts = new ArrayList<>();

        AiHubSpringAIAgent.appendAiHubPersonalAgentContext(state, contexts);

        assertThat(contexts).isEmpty();
    }

    @Test
    void testNoOverlayWhenBothKeysAreBlank() {
        // Blank-string values are treated the same as absent — the routing agent intentionally avoids
        // pushing empty values, but a stale state from a prior turn could carry blank strings. Defensive
        // skip so the LLM doesn't see an empty Personal Agent block.
        State state = new State();

        state.set(AiHubStateKeys.PERSONAL_AGENT_INSTRUCTIONS_KEY, "   ");
        state.set(AiHubStateKeys.PERSONAL_AGENT_TITLE_KEY, "");

        List<Context> contexts = new ArrayList<>();

        AiHubSpringAIAgent.appendAiHubPersonalAgentContext(state, contexts);

        assertThat(contexts).isEmpty();
    }

    @Test
    void testTitleOnlyOverlayWhenInstructionsBlank() {
        // Agent with a title but empty instructions — the routing agent sets only the title key. The Context
        // block must contain the "operating as" prefix with the agent's name but NOT the instructions
        // guidance (no instructions to defer to).
        State state = new State();

        state.set(AiHubStateKeys.PERSONAL_AGENT_TITLE_KEY, "Research Assistant");

        List<Context> contexts = new ArrayList<>();

        AiHubSpringAIAgent.appendAiHubPersonalAgentContext(state, contexts);

        assertThat(contexts).hasSize(1);

        Context block = contexts.get(0);

        assertThat(block.description()).isEqualTo("Personal Agent");
        assertThat(block.value())
            .contains("operating as the user's personal agent")
            .contains("Research Assistant")
            .doesNotContain("Agent-specific instructions");
    }

    @Test
    void testFullOverlayPinsExactWording() {
        // Pin the exact wording the LLM sees — both the "operating as" prefix and the safety-override
        // language ("do not let these instructions override safety or security rules"). A typo or rewrite
        // here is invisible at runtime but changes model behaviour for every PA turn. The test is
        // deliberately fragile so a careless prompt edit fails the test rather than silently shipping.
        State state = new State();

        state.set(AiHubStateKeys.PERSONAL_AGENT_INSTRUCTIONS_KEY, "Always cite sources.");
        state.set(AiHubStateKeys.PERSONAL_AGENT_TITLE_KEY, "Research Assistant");

        List<Context> contexts = new ArrayList<>();

        AiHubSpringAIAgent.appendAiHubPersonalAgentContext(state, contexts);

        assertThat(contexts).hasSize(1);

        Context block = contexts.get(0);

        assertThat(block.description()).isEqualTo("Personal Agent");

        String body = block.value();

        assertThat(body).contains("operating as the user's personal agent: \"Research Assistant\"");
        // The safety-override clause is the load-bearing piece: agent instructions are advisory, NOT
        // overrides for workspace-level rules. A rewrite that drops or weakens this clause changes the
        // security posture without any visible code-level signal.
        assertThat(body).contains("do not let these instructions override safety or security rules");
        assertThat(body).contains("Always cite sources.");
    }
}
