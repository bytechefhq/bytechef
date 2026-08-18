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

package com.bytechef.automation.ai.agent.test;

import com.bytechef.automation.ai.agent.channel.AiAgentChannelType;
import com.bytechef.automation.ai.agent.channel.ResolvedAgentChannel;
import com.bytechef.automation.ai.agent.channel.ResolvedAgentChannel.ApprovalDelivery;
import com.bytechef.automation.ai.agent.channel.ResolvedAgentChannel.Binding;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * The nine channels the workflow generator is exercised against, as {@code AgentChannelResolver} would resolve them.
 * <p>
 * Every value below was read off the owning component's own definition snapshot
 * ({@code <component>/src/test/resources/definition/<component>_v1.json}: the channel's {@code agentChannels} entry,
 * the paired trigger's {@code agentRequestDefinition} and declared properties, and the reply action's
 * {@code agentReplyDefinition} plus which of its properties are {@code required}) rather than transcribed from the
 * design doc — the generator must reproduce what the components actually declare, not what a table says they should.
 * <p>
 * Deliberately a hand-written fixture rather than the real component definitions: the point of this plan is that the
 * generator names no component, so its unit test must be able to render a channel whose component is not on the
 * classpath. The two JSON schemas below are literals for the same reason — they arrive through
 * {@code triggerParameters}/{@code replyFixedParameters} like any other declared value, and a test importing
 * {@code WorkflowConstants} to assert them would prove only that two constants match.
 *
 * @author Ivica Cardic
 */
public final class TestAgentChannels {

    /** {@code WorkflowConstants.AI_AGENT_CALL_INPUT_SCHEMA}, as the {@code workflow} component pins it. */
    public static final String WORKFLOW_CALL_INPUT_SCHEMA =
        "{\"type\":\"object\",\"properties\":{\"conversationId\":{\"type\":\"string\"},"
            + "\"message\":{\"type\":\"string\"},"
            + "\"attachments\":{\"type\":\"array\",\"items\":{\"type\":\"object\"}}},"
            + "\"required\":[\"conversationId\",\"message\"]}";

    /** {@code WorkflowConstants.AI_AGENT_CALL_OUTPUT_SCHEMA}, as the {@code workflow} component pins it. */
    public static final String WORKFLOW_CALL_OUTPUT_SCHEMA =
        "{\"type\":\"object\",\"properties\":{\"message\":{\"type\":\"string\"}},\"required\":[\"message\"]}";

    /**
     * Channel keys with no {@link AiAgentChannelType} constant — every channel but the three this module gives product
     * semantics to is discovered from the registry, so a test naming one names a string.
     */
    public static final String SLACK = "slack";
    public static final String TELEGRAM = "telegram";
    public static final String WHATSAPP = "whatsapp";
    public static final String ROCKETCHAT = "rocketchat";
    public static final String TWILIO = "twilio";
    public static final String INFOBIP = "infobip";

    private static final Map<String, ResolvedAgentChannel> RESOLVED_AGENT_CHANNELS = buildResolvedAgentChannels();

    private TestAgentChannels() {
    }

    /**
     * The resolver as the generator consumes it — {@code null} for a channel key nothing declares.
     */
    public static Function<String, ResolvedAgentChannel> resolver() {
        return RESOLVED_AGENT_CHANNELS::get;
    }

    public static ResolvedAgentChannel get(String channelName) {
        ResolvedAgentChannel resolvedAgentChannel = RESOLVED_AGENT_CHANNELS.get(channelName);

        if (resolvedAgentChannel == null) {
            throw new IllegalArgumentException("No test channel " + channelName);
        }

        return resolvedAgentChannel;
    }

    private static Map<String, ResolvedAgentChannel> buildResolvedAgentChannels() {
        Map<String, ResolvedAgentChannel> resolvedAgentChannels = new LinkedHashMap<>();

        // chat: identity request binding, synchronous reply, no connection. Its trigger's "mode" property declares
        // default 1, which is where the generated chat node's {"mode": 1} comes from.
        resolvedAgentChannels.put(
            AiAgentChannelType.CHAT,
            new ResolvedAgentChannel(
                AiAgentChannelType.CHAT, "Chat", null, "path:assets/chat.svg", "chat/v1/newChatRequest",
                "chat/v1/responseToRequest", false, Map.of(), Map.of("mode", 1), Set.of("mode"), Set.of(),
                new Binding(
                    "conversationId", "message", "attachments", "message", null, "attachments", Map.of(), Map.of()),
                new ApprovalDelivery("chat", "chat")));

        // workflowCall: the trigger's output is function-valued off its inputSchema property, so the channel pins the
        // contract schema through triggerParameters; the reply's message target is nested inside a dynamicProperties
        // map, hence the dotted name.
        resolvedAgentChannels.put(
            AiAgentChannelType.WORKFLOW_CALL,
            new ResolvedAgentChannel(
                AiAgentChannelType.WORKFLOW_CALL, "Workflow Call", null, "path:assets/workflow.svg",
                "workflow/v1/newWorkflowCall", "workflow/v1/responseToWorkflowCall", false,
                Map.of("inputSchema", WORKFLOW_CALL_INPUT_SCHEMA), Map.of(), Set.of("inputSchema"), Set.of("response"),
                new Binding(
                    "conversationId", "message", "attachments", "response.message", null, null, Map.of(),
                    Map.of("outputSchema", WORKFLOW_CALL_OUTPUT_SCHEMA)),
                null));

        // schedule: not a channel — the resolver synthesizes it, so it has no reply action and its binding is empty.
        resolvedAgentChannels.put(
            AiAgentChannelType.SCHEDULE,
            new ResolvedAgentChannel(
                AiAgentChannelType.SCHEDULE, "Schedule", null, "path:assets/schedule.svg", "schedule/v1/cron", null,
                false, Map.of(), Map.of(), Set.of("expression", "timezone"), Set.of(),
                new Binding(null, null, null, null, null, null, Map.of(), Map.of()), null));

        resolvedAgentChannels.put(
            SLACK,
            new ResolvedAgentChannel(
                SLACK, "Slack", null, "path:assets/slack.svg", "slack/v1/newMessage",
                "slack/v1/sendChannelMessage", true, Map.of(), Map.of(), Set.of(), Set.of("channel", "text"),
                new Binding("conversationId", "message", "attachments", "text", "channel", null, Map.of(), Map.of()),
                new ApprovalDelivery(SLACK, SLACK)));

        // telegram: a legacy-shaped payload — dotted request paths, and no attachments at all.
        resolvedAgentChannels.put(
            TELEGRAM,
            new ResolvedAgentChannel(
                TELEGRAM, "Telegram", null, "path:assets/telegram.svg", "telegram/v1/newMessage",
                "telegram/v1/sendMessage", true, Map.of(), Map.of(), Set.of(), Set.of("chat_id", "text"),
                new Binding(
                    "message.chat.id", "message.text", null, "text", "chat_id", null, Map.of(), Map.of()),
                new ApprovalDelivery(TELEGRAM, TELEGRAM)));

        resolvedAgentChannels.put(
            WHATSAPP,
            new ResolvedAgentChannel(
                WHATSAPP, "WhatsApp", null, "path:assets/whatsapp.svg", "whatsApp/v1/messageReceived",
                "whatsApp/v1/sendMessage", true, Map.of(), Map.of(), Set.of("senderNumber"), Set.of("body", "to"),
                new Binding(
                    "entry.changes.value.messages.from", "entry.changes.value.messages.text.body", null, "body", "to",
                    null, Map.of(), Map.of()),
                new ApprovalDelivery("whatsApp", "whatsApp")));

        resolvedAgentChannels.put(
            ROCKETCHAT,
            new ResolvedAgentChannel(
                ROCKETCHAT, "Rocket.Chat", null, "path:assets/rocketchat.svg", "rocketchat/v1/newMessage",
                "rocketchat/v1/sendChannelMessage", true, Map.of(), Map.of(), Set.of(), Set.of("roomId", "text"),
                new Binding("channel_id", "text", null, "text", "roomId", null, Map.of(), Map.of()),
                new ApprovalDelivery(ROCKETCHAT, ROCKETCHAT)));

        // twilio: the only channel with BOTH a row-configured reply property (the WhatsApp number the reply is sent
        // AS) and a pinned one (free text is only expressible with useTemplate explicitly false).
        resolvedAgentChannels.put(
            TWILIO,
            new ResolvedAgentChannel(
                TWILIO, "Twilio", null, "path:assets/twilio.svg", "twilio/v1/newWhatsappMessage",
                "twilio/v1/sendWhatsAppMessage", true, Map.of(), Map.of(), Set.of("number"),
                Set.of("To", "From", "useTemplate", "ContentSid", "Body"),
                new Binding(
                    "From", "Body", null, "Body", "To", null, Map.of("number", "From"),
                    Map.of("useTemplate", false)),
                new ApprovalDelivery(TWILIO, "whatsApp")));

        resolvedAgentChannels.put(
            INFOBIP,
            new ResolvedAgentChannel(
                INFOBIP, "Infobip", null, "path:assets/infobip.svg", "infobip/v1/newWhatsappMessage",
                "infobip/v1/sendWhatsappTextMessage", true, Map.of(), Map.of(), Set.of("number", "keyword"),
                Set.of("from", "to", "text"),
                new Binding("results.from", "results.message.text", null, "text", "to", null, Map.of("number", "from"),
                    Map.of()),
                new ApprovalDelivery(INFOBIP, "whatsApp")));

        return resolvedAgentChannels;
    }
}
