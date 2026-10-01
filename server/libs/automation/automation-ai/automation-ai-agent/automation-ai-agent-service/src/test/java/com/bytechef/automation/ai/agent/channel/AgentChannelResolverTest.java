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

    @Test
    void testResolveReadsTriggerPropertyDefaultsAndNames() {
        ResolvedAgentChannel resolvedAgentChannel = agentChannelResolver.resolve("chat")
            .orElseThrow();

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

    @Test
    void testResolveReadsRequiredReplyPropertyNames() {
        ResolvedAgentChannel resolvedAgentChannel = agentChannelResolver.resolve("twilio")
            .orElseThrow();

        assertThat(resolvedAgentChannel.requiredReplyPropertyNames()).containsExactly("From", "To", "Body");
        assertThat(resolvedAgentChannel.triggerPropertyNames()).containsExactly("number");

        Binding binding = resolvedAgentChannel.binding();

        assertThat(binding.replyChannelParameters()).containsExactly(entry("number", "From"));
    }

    @Test
    void testResolveReportsAChannelWithTriggerPropertiesAsConfigurable() {
        ResolvedAgentChannel resolvedAgentChannel = agentChannelResolver.resolve("chat")
            .orElseThrow();

        assertThat(resolvedAgentChannel.connectionRequired()).isFalse();
        assertThat(resolvedAgentChannel.propertiesConfigurable()).isTrue();
    }

    @Test
    void testResolveReportsAChannelWhosePropertiesArePinnedAsNotConfigurable() {
        ResolvedAgentChannel resolvedAgentChannel = agentChannelResolver.resolve("workflowCall")
            .orElseThrow();

        assertThat(resolvedAgentChannel.triggerPropertyNames()).containsExactly("inputSchema");
        assertThat(resolvedAgentChannel.triggerParameters()).containsOnlyKeys("inputSchema");
        assertThat(resolvedAgentChannel.propertiesConfigurable()).isFalse();
    }

    @Test
    void testResolveReportsAPropertylessChannelAsNotConfigurable() {
        ResolvedAgentChannel resolvedAgentChannel = agentChannelResolver.resolve("telegram")
            .orElseThrow();

        assertThat(resolvedAgentChannel.triggerPropertyNames()).isEmpty();
        assertThat(resolvedAgentChannel.propertiesConfigurable()).isFalse();
        assertThat(resolvedAgentChannel.connectionRequired()).isTrue();
    }

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
