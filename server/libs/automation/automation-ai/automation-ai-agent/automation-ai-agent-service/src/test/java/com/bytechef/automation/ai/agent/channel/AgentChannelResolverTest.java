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

package com.bytechef.automation.ai.agent.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import com.bytechef.automation.ai.agent.channel.ResolvedAgentChannel.ApprovalDelivery;
import com.bytechef.automation.ai.agent.channel.ResolvedAgentChannel.Binding;
import com.bytechef.automation.ai.agent.config.TestComponentDefinitions;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link AgentChannelResolver}, against the stubbed component registry in
 * {@link TestComponentDefinitions} — real SDK definitions projected onto the platform DTOs, so the binding flattening
 * the resolver reads is genuinely exercised rather than mocked away.
 *
 * @author Ivica Cardic
 */
class AgentChannelResolverTest {

    private final ComponentDefinitionService componentDefinitionService =
        TestComponentDefinitions.componentDefinitionService();
    private final AgentChannelResolver agentChannelResolver = new AgentChannelResolver(componentDefinitionService);

    @Test
    void testResolveProjectsTheComponentDeclarationOntoTheChannel() {
        ResolvedAgentChannel resolvedAgentChannel = agentChannelResolver.resolve("slack")
            .orElseThrow();

        assertThat(resolvedAgentChannel.name()).isEqualTo("slack");
        assertThat(resolvedAgentChannel.title()).isEqualTo("Slack");
        assertThat(resolvedAgentChannel.triggerType()).isEqualTo("slack/v1/newMessage");
        assertThat(resolvedAgentChannel.replyActionType()).isEqualTo("slack/v1/sendChannelMessage");
        assertThat(resolvedAgentChannel.componentName()).isEqualTo("slack");
        assertThat(resolvedAgentChannel.approvalDelivery()).isEqualTo(new ApprovalDelivery("slack", "slack"));

        Binding binding = resolvedAgentChannel.binding();

        assertThat(binding.conversationIdPath()).isEqualTo("conversationId");
        assertThat(binding.messagePath()).isEqualTo("message");
        assertThat(binding.attachmentsPath()).isEqualTo("attachments");
        assertThat(binding.replyMessageProperty()).isEqualTo("text");
        assertThat(binding.replyConversationIdProperty()).isEqualTo("channel");
    }

    /**
     * The trigger's declared defaults and property names come from the component, not from the channel declaration —
     * they are what pre-fills the generated trigger node and what restricts which channel-row keys may reach it.
     */
    @Test
    void testResolveReadsTriggerPropertyDefaultsAndNames() {
        ResolvedAgentChannel resolvedAgentChannel = agentChannelResolver.resolve("chat")
            .orElseThrow();

        // 1L, not 1: the SDK's integer property carries its default as a Long. Both serialize to the same JSON
        // number, which is what the generated chat trigger node's {"mode": 1} has always been.
        assertThat(resolvedAgentChannel.triggerPropertyDefaults()).containsExactly(entry("mode", 1L));
        assertThat(resolvedAgentChannel.triggerPropertyNames()).containsExactly("mode");
        assertThat(resolvedAgentChannel.connectionRequired()).isFalse();
    }

    @Test
    void testResolveCarriesDeclaredTriggerAndReplyParameters() {
        ResolvedAgentChannel resolvedAgentChannel = agentChannelResolver.resolve("workflowCall")
            .orElseThrow();

        assertThat(resolvedAgentChannel.triggerParameters()).containsOnlyKeys("inputSchema");

        Binding binding = resolvedAgentChannel.binding();

        assertThat(binding.replyMessageProperty()).isEqualTo("response.message");
        assertThat(binding.replyFixedParameters()).containsOnlyKeys("outputSchema");
    }

    /**
     * Publish validation refuses a channel row that leaves a REQUIRED reply property unfed, so the requiredness has to
     * come off the reply action's own property definitions — the test twilio pair is the real asymmetry: an optional
     * trigger property {@code number} supplying a required reply property {@code From}.
     */
    @Test
    void testResolveReadsRequiredReplyPropertyNames() {
        ResolvedAgentChannel resolvedAgentChannel = agentChannelResolver.resolve("twilio")
            .orElseThrow();

        assertThat(resolvedAgentChannel.requiredReplyPropertyNames()).containsExactly("From", "To", "Body");
        assertThat(resolvedAgentChannel.triggerPropertyNames()).containsExactly("number");

        Binding binding = resolvedAgentChannel.binding();

        assertThat(binding.replyChannelParameters()).containsExactly(entry("number", "From"));
    }

    /**
     * Whether a channel has anything to configure is a question about its trigger's properties, not about whether it
     * needs a connection — the client's Configure affordance used {@code connectionRequired} as a stand-in, which is
     * exactly wrong for chat: no connection, but a real {@code mode} property behind it.
     */
    @Test
    void testResolveReportsAChannelWithTriggerPropertiesAsConfigurable() {
        ResolvedAgentChannel resolvedAgentChannel = agentChannelResolver.resolve("chat")
            .orElseThrow();

        assertThat(resolvedAgentChannel.connectionRequired()).isFalse();
        assertThat(resolvedAgentChannel.propertiesConfigurable()).isTrue();
    }

    /**
     * A property the channel declaration PINS is not configurable: the row cannot change it, and workflowCall's
     * {@code inputSchema} is its only property. A channel whose every property is pinned has nothing to configure.
     */
    @Test
    void testResolveReportsAChannelWhosePropertiesArePinnedAsNotConfigurable() {
        ResolvedAgentChannel resolvedAgentChannel = agentChannelResolver.resolve("workflowCall")
            .orElseThrow();

        assertThat(resolvedAgentChannel.triggerPropertyNames()).containsExactly("inputSchema");
        assertThat(resolvedAgentChannel.triggerParameters()).containsOnlyKeys("inputSchema");
        assertThat(resolvedAgentChannel.propertiesConfigurable()).isFalse();
    }

    /**
     * The converse of chat, and the case the old predicate happened to get right: telegram's channel trigger declares
     * no properties at all, so its Configure affordance rests on the connection alone.
     */
    @Test
    void testResolveReportsAPropertylessChannelAsNotConfigurable() {
        ResolvedAgentChannel resolvedAgentChannel = agentChannelResolver.resolve("telegram")
            .orElseThrow();

        assertThat(resolvedAgentChannel.triggerPropertyNames()).isEmpty();
        assertThat(resolvedAgentChannel.propertiesConfigurable()).isFalse();
        assertThat(resolvedAgentChannel.connectionRequired()).isTrue();
    }

    /**
     * A schedule has no reply action at all, so there is nothing whose requiredness could be read — and publish
     * validation must not stumble over the absence.
     */
    @Test
    void testResolveHasNoRequiredReplyPropertyNamesWithoutAReplyAction() {
        ResolvedAgentChannel resolvedAgentChannel = agentChannelResolver.resolve(AiAgentChannelType.SCHEDULE)
            .orElseThrow();

        assertThat(resolvedAgentChannel.requiredReplyPropertyNames()).isEmpty();
    }

    @Test
    void testResolveIsEmptyForAnUndeclaredChannel() {
        Optional<ResolvedAgentChannel> resolvedAgentChannel = agentChannelResolver.resolve("nope");

        assertThat(resolvedAgentChannel).isEmpty();
    }

    /**
     * A schedule is not a channel, so no component declares it — the resolver synthesizes it from the real
     * {@code schedule/v1/cron} trigger. Its row-parameter allow-list must come from that trigger's own properties, or a
     * schedule row's UI-only keys would start leaking onto the generated node again.
     */
    @Test
    void testResolveSynthesizesTheScheduleEntry() {
        ResolvedAgentChannel resolvedAgentChannel = agentChannelResolver.resolve(AiAgentChannelType.SCHEDULE)
            .orElseThrow();

        assertThat(resolvedAgentChannel.triggerType()).isEqualTo("schedule/v1/cron");
        assertThat(resolvedAgentChannel.replyActionType()).isNull();
        assertThat(resolvedAgentChannel.connectionRequired()).isFalse();
        assertThat(resolvedAgentChannel.approvalDelivery()).isNull();
        assertThat(resolvedAgentChannel.triggerPropertyNames()).containsExactly("expression", "timezone");

        Binding binding = resolvedAgentChannel.binding();

        assertThat(binding.conversationIdPath()).isNull();
        assertThat(binding.messagePath()).isNull();
        assertThat(binding.attachmentsPath()).isNull();
        assertThat(binding.replyMessageProperty()).isNull();
    }

    /**
     * {@code resolveAll} backs the client's channel cards, so it must carry the synthesized schedule entry too — the
     * registry cannot supply one, and the schedule card's metadata would otherwise have nowhere to come from.
     */
    @Test
    void testResolveAllIncludesTheSynthesizedScheduleEntry() {
        List<ResolvedAgentChannel> resolvedAgentChannels = agentChannelResolver.resolveAll();

        assertThat(resolvedAgentChannels).extracting(ResolvedAgentChannel::name)
            .containsExactly("chat", "workflowCall", "slack", "telegram", "twilio", AiAgentChannelType.SCHEDULE);

        ResolvedAgentChannel scheduleChannel = resolvedAgentChannels.get(resolvedAgentChannels.size() - 1);

        assertThat(scheduleChannel.name()).isEqualTo(AiAgentChannelType.SCHEDULE);
        assertThat(scheduleChannel.replyActionType()).isNull();
        assertThat(scheduleChannel.connectionRequired()).isFalse();
        assertThat(scheduleChannel.title()).isEqualTo("Schedule");
    }
}
