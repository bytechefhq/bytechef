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

package com.bytechef.automation.ai.agent.util;

import com.bytechef.automation.ai.agent.channel.AiAgentChannelType;
import com.bytechef.automation.ai.agent.channel.ApprovalChannelDefinitions;
import com.bytechef.automation.ai.agent.channel.ApprovalChannelDefinitions.ApprovalChannelDefinition;
import com.bytechef.automation.ai.agent.channel.ChannelDefinition;
import com.bytechef.automation.ai.agent.channel.ChannelDefinition.ApprovalDelivery;
import com.bytechef.automation.ai.agent.channel.ChannelDefinition.EnvelopeMapping;
import com.bytechef.automation.ai.agent.channel.ChannelDefinition.ReplyTask;
import com.bytechef.automation.ai.agent.channel.ChannelDefinitions;
import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.domain.AiAgentChannel;
import com.bytechef.automation.ai.agent.domain.AiAgentElement;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import com.bytechef.platform.workflow.JobInputConstants;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/**
 * Renders an {@code AiAgent} aggregate (agent + channels + elements) into a complete workflow-definition JSON: one
 * trigger per {@code AiAgentChannel}, all fanning into a {@code branch_in} dispatcher, an {@code aiAgent/v1/streamChat}
 * node, and a {@code branch_out} dispatcher that routes the reply back through the channel that asked for it. See
 * {@code docs/superpowers/specs/2026-08-10-agents-design.md}, "Workflow generation".
 *
 * <p>
 * Modeled on {@code KnowledgeBaseSourceWorkflowGenerator} (same module family): builds a {@link LinkedHashMap} tree and
 * serializes it with {@link JsonUtils#write(Object)}. Determinism (byte-identical output for identical inputs) is
 * achieved by never using a hash-ordered {@link Map}, always iterating channels in {@code position} order, and never
 * touching the clock or a random source — the one exception, the schedule channel's chat-memory key, is a
 * content-derived {@link UUID#nameUUIDFromBytes(byte[])} rather than a fresh random UUID.
 *
 * <p>
 * <b>{@code aiAgent_1}'s prompt property.</b> {@code AiAgentConstants.STREAM_CHAT_PROPERTIES} is
 * {@code List.of(FORMAT_PROPERTY, PROMPT_PROPERTY, SYSTEM_PROMPT_PROPERTY, ATTACHMENTS_PROPERTY, MESSAGES_PROPERTY)},
 * and {@code LLMConstants.PROMPT_PROPERTY = string(USER_PROMPT)} where {@code USER_PROMPT = "userPrompt"} — the
 * property key is {@code userPrompt}, not {@code prompt}. {@code ATTACHMENTS_PROPERTY} is also present (key
 * {@code "attachments"}), so both are emitted.
 *
 * <p>
 * <b>{@code aiAgent_1}'s system prompt.</b> {@code LLMConstants.SYSTEM_PROMPT_PROPERTY = string(SYSTEM_PROMPT)} where
 * {@code SYSTEM_PROMPT = "systemPrompt"} (verified in {@code LLMConstants.java}) — per the design doc's "Generated
 * shape" table, {@code systemPrompt: <agent.instructions>}. Emitted only when {@code agent.getInstructions()} is
 * non-blank; a {@code null}/blank instructions value contributes no key, same convention as the {@code clusterElements}
 * groups below (absence, not an empty string, for "nothing configured").
 *
 * <p>
 * <b>{@code aiAgent_1}'s {@code clusterElements}.</b> Built from {@code elements} plus {@code agent.settings} (see the
 * "clusterElements" section below) — a {@code MODEL} element becomes the {@code model} entry; {@code tools[]} is
 * {@code TOOL} rows (the approval gate, if any, inline among them), then the {@code SKILL} aggregate, then
 * {@code SUB_AGENT} rows, then the settings-driven built-ins ({@code askUserQuestion}/{@code autoMemory}/
 * {@code skillManagement}/{@code webSearch} — see {@code AiAgentSettings}, all default ON except {@code webSearch}),
 * then the {@code APPROVAL_TOOL} element last (in that fixed group order, position-ordered within each row-backed
 * group); a {@code KNOWLEDGE_BASE} row becomes the {@code rag} entry (with a nested {@code vectorStore}), and a
 * {@code CHAT_MEMORY} row becomes the {@code chatMemory} entry. A row-backed kind with no rows AND every
 * settings-driven built-in off still contributes no {@code tools} key — but since three built-ins default ON, an agent
 * with {@code elements = List.of()} and default settings yields {@code tools[]} populated with those defaults, not an
 * empty {@code clusterElements: {}} (unlike before this feature).
 *
 * <p>
 * <b>{@code __AGENT_OUTPUT__}'s expression.</b> {@code streamChat}'s output function is
 * {@code ModelUtils.output(inputParameters, null, context)}. Since {@code STREAM_CHAT_PROPERTIES} has no
 * {@code RESPONSE_PROPERTY}, {@code inputParameters} never has a {@code response.responseFormat} path, so
 * {@code getFromPath(..., TEXT)} falls back to its default {@code TEXT}, and {@code output(...)} returns
 * {@code OutputResponse.of(string())} — an *unnamed* string schema. The node's entire output is the assistant's text,
 * with no field path, so {@code __AGENT_OUTPUT__} resolves to the bare node reference {@code ${aiAgent_1}}, not
 * {@code ${aiAgent_1.text}}.
 *
 * <p>
 * <b>HITL approval gate.</b> {@code TOOL} rows with {@code parameters["requiresApproval"] = true} are pulled out of the
 * flat {@code tools[]} array and nested inside ONE {@code aiAgentUtils/v1/approvalGateTool} cluster element (see
 * {@code AiAgentUtilsApprovalGateTool} in {@code components/ai/agent/utils}), placed deterministically at the position
 * the FIRST gated tool would otherwise have occupied among the (still-flat) ungated tools — see
 * {@link #buildToolSequence}. The gate's own {@code clusterElements.tools} carries every gated {@code TOOL} row, built
 * exactly like an ungated one ({@link #buildToolElement}); its {@code clusterElements.approvalChannels} carries one
 * entry per approval-capable AGENT CHANNEL ({@link #buildApprovalDeliveryChannels} →
 * {@link #buildApprovalChannelElement}), omitted entirely when there are none (the runtime defaults an empty/absent
 * channel list to the {@code chat} channel). The gate is emitted only when the {@code APPROVAL_GATE} row is present AND
 * at least one gated tool exists — the row is the agent-level master switch (see {@link #buildToolSequence}), so a
 * {@code requiresApproval} tool with no {@code APPROVAL_GATE} row runs ungated, and a stray {@code APPROVAL_GATE} row
 * with no gated tools contributes nothing. Every node name inside the gate — the gate's own name, each gated tool's
 * name, each channel's name — is drawn from the SAME shared {@code nodeCounters} map as the rest of
 * {@link #buildClusterElements}, so the whole tree stays collision-free; {@link #buildConnectionRefs} mirrors this
 * exact traversal order (gate name, then gated tools, then channels) so a draft's {@code WorkflowTestConfiguration}
 * connections stay in sync (see {@code AiAgentFacadeImpl#syncTestConnections}).
 *
 * <p>
 * <b>HITL approval TOOL.</b> An {@code AiAgentElement.KIND_APPROVAL_TOOL} singleton row emits the platform
 * {@code approval} component's own {@code approval/v1/requestApproval} cluster element
 * ({@code ApprovalRequestApprovalTool} — an LLM-invocable "ask a human" call, distinct from the gate above, which only
 * ever WRAPS other tools) directly into {@code tools[]}, always as a top-level entry and never nested inside the gate —
 * {@code AiAgentUtilsApprovalGateTool.checkGatableChild} rejects that nesting outright (double-suspend). It carries the
 * SAME channel-derived approval destinations as the gate, in its own {@code clusterElements.approvalChannels} (built by
 * the shared {@link #buildApprovalChannelEntries}, so the two never duplicate that logic) — a fresh set of channel node
 * instances, since the tool and the gate are different parents in the tree. See {@link #buildApprovalToolElement}.
 *
 * @author Ivica Cardic
 */
public final class AiAgentWorkflowGenerator {

    private static final String BRANCH_TYPE = "branch/v1";
    private static final String VAR_SET_TYPE = "var/v1/set";
    private static final String CONDITION_TYPE = "condition/v1";
    private static final String TERMINATE_TYPE = "terminate/v1";

    /** Slack Events API bot-message signals — see {@link #buildSlackBotEchoGuard}. */
    private static final String SLACK_BOT_ID_FIELD = "bot_id";
    private static final String SLACK_SUBTYPE_FIELD = "subtype";
    private static final String SLACK_BOT_MESSAGE_SUBTYPE = "bot_message";
    private static final String AI_AGENT_NODE_NAME = "aiAgent_1";
    private static final String AI_AGENT_TYPE = "aiAgent/v1/streamChat";

    private static final String BRANCH_IN_NAME = "branch_in";
    private static final String BRANCH_OUT_NAME = "branch_out";

    private static final String AGENT_OUTPUT_TOKEN = "__AGENT_OUTPUT__";
    private static final String ENVELOPE_TOKEN_PREFIX = "__ENVELOPE__";
    private static final String NODE_TOKEN = "__NODE__";
    private static final String EMPTY_ARRAY_LITERAL = "[]";

    private static final String AGENT_OUTPUT_EXPRESSION = "${" + AI_AGENT_NODE_NAME + "}";

    /** Envelope field keys — see {@link EnvelopeMapping} and the design doc's generated-shape table. */
    private static final String FIELD_TEXT = "text";
    private static final String FIELD_CONVERSATION_ID = "conversationId";
    private static final String FIELD_ATTACHMENTS = "attachments";
    private static final String FIELD_REPLY_TO = "replyTo";
    private static final String FIELD_REPLY_FROM = "replyFrom";
    private static final String FIELD_CHANNEL = "channel";

    /** Stored per-schedule prompt: {@code AiAgentChannel.parameters["prompt"]}, per the interface note. */
    private static final String SCHEDULE_PROMPT_PARAMETER = "prompt";

    /** A schedule's display name, stored on the agent_channel row purely so the UI can label the row. */
    private static final String SCHEDULE_NAME_PARAMETER = "name";

    /**
     * {@code aiAgent_1}'s system-prompt parameter key — {@code LLMConstants.SYSTEM_PROMPT_PROPERTY = string(
     * SYSTEM_PROMPT)} where {@code SYSTEM_PROMPT = "systemPrompt"}. See this class's javadoc, "aiAgent_1's system
     * prompt".
     */
    private static final String SYSTEM_PROMPT_PARAMETER = "systemPrompt";

    /**
     * Predefined {@code inputSchema} for the {@code workflowCall} trigger, per the interface note and
     * {@code WorkflowNewWorkflowCallTrigger} / {@code WorkflowResponseUtils#triggerOutput} (the trigger's output is
     * whatever this schema describes).
     */
    private static final String WORKFLOW_CALL_INPUT_SCHEMA =
        "{\"type\":\"object\",\"properties\":{\"message\":{\"type\":\"string\"},"
            + "\"conversationId\":{\"type\":\"string\"},"
            + "\"attachments\":{\"type\":\"array\",\"items\":{\"type\":\"object\"}}},"
            + "\"required\":[\"message\"]}";

    /**
     * Predefined {@code outputSchema} for {@code workflow/v1/responseToWorkflowCall}'s {@code response} dynamic
     * properties, mirroring {@link #WORKFLOW_CALL_INPUT_SCHEMA}'s {@code message} field name (see
     * {@code ChannelDefinitions.workflowCall()}'s source comment: "building that JSON-schema string is the generator's
     * job").
     */
    private static final String WORKFLOW_CALL_OUTPUT_SCHEMA =
        "{\"type\":\"object\",\"properties\":{\"message\":{\"type\":\"string\"}},\"required\":[\"message\"]}";

    private static final String INPUT_SCHEMA_PARAMETER = "inputSchema";
    private static final String OUTPUT_SCHEMA_PARAMETER = "outputSchema";

    private AiAgentWorkflowGenerator() {
    }

    /**
     * Builds the complete workflow-definition JSON for {@code agent}. {@code elements} and {@code subAgentResolver}
     * feed {@code aiAgent_1}'s {@code clusterElements} — see this class's javadoc.
     */
    public static String generate(
        AiAgent agent, List<AiAgentChannel> channels, List<AiAgentElement> elements,
        Function<Long, SubAgentRef> subAgentResolver) {

        Objects.requireNonNull(agent, "agent");
        Objects.requireNonNull(channels, "channels");
        Objects.requireNonNull(elements, "elements");
        Objects.requireNonNull(subAgentResolver, "subAgentResolver");

        List<ChannelNode> channelNodes = buildChannelNodes(channels);

        Map<String, Object> definition = new LinkedHashMap<>();

        definition.put("label", agent.getTitle());
        definition.put("description", "Auto-generated by Agents. Edits via the Agent Studio UI.");
        definition.put("inputs", List.of());
        definition.put("triggers", buildTriggers(channelNodes));
        definition.put(
            "tasks",
            List.of(
                buildBranchIn(agent, channelNodes), buildAiAgentNode(agent, channels, elements, subAgentResolver),
                buildBranchOut(channelNodes)));

        return JsonUtils.write(definition);
    }

    /**
     * Resolves a {@code SUB_AGENT} {@link AiAgentElement}'s {@code referenceId} to the target agent's identity —
     * provided by the caller ({@code AiAgentFacadeImpl#resolveSubAgentRef}; a stub in tests). {@code agentUuid} is the
     * target {@code AiAgent}'s own uuid, fed into the generated {@code workflow/v1/callAiAgent} tool's
     * {@code agentUuid} parameter — {@code CallableAiAgentDataSource#resolveAgent} maps it to the target's published
     * workflow uuid at dispatch time, so this generator no longer needs to resolve (or embed) that workflow uuid
     * itself.
     */
    public record SubAgentRef(String name, String description, UUID agentUuid) {
    }

    /**
     * Discriminates a {@link ConnectionRef}'s {@code ownerId} space: {@code CHANNEL}/{@code ELEMENT} are
     * {@link AiAgentChannel#getId()}/{@link AiAgentElement#getId()} (independent, numerically-colliding id spaces),
     * {@code AGENT_SETTINGS} is the owning {@link AiAgent#getId()} itself — used for the {@code webSearch} built-in's
     * {@code webSearchConnectionId}, which lives in {@code AiAgent.settings} rather than on any channel/element row.
     */
    public enum ConnectionRefOwnerKind {
        CHANNEL, ELEMENT, AGENT_SETTINGS
    }

    /**
     * One channel/element/settings row that {@link #generate} gives a {@code connections} block, identifying the
     * workflow node name and connection key that block appears under — see {@link ConnectionRefOwnerKind}.
     * <p>
     * The two fields follow the platform's own convention, which differs by node kind and is NOT interchangeable:
     * <ul>
     * <li>A <b>channel</b> is a trigger, i.e. a workflow node in its own right, so it is keyed
     * ({@code workflowNodeName} = the trigger's node name, {@code workflowConnectionKey} = its component name) — the
     * same shape a plain task gets.</li>
     * <li>An <b>element</b> is a cluster element hanging off {@value #AI_AGENT_NODE_NAME}, which is not a workflow
     * node. It is keyed ({@code workflowNodeName} = the cluster ROOT's node name, {@code workflowConnectionKey} = the
     * ELEMENT's own generated node name), because that is what both readers expect: every runtime consumer resolves its
     * connection with {@code componentConnections.get(clusterElement.getWorkflowNodeName())}, and the simple editor's
     * {@code useAiAgentTools} looks the test configuration up by the root node name and matches each tool against
     * {@code workflowConnectionKey}. Writing the element's node name into {@code workflowNodeName} instead — as this
     * did before — files the connection under a node nobody ever queries.</li>
     * </ul>
     */
    public record ConnectionRef(
        long ownerId, ConnectionRefOwnerKind ownerKind, String workflowNodeName, String workflowConnectionKey) {
    }

    /**
     * Maps every channel/element/settings-driven node whose generated output carries a {@code connections} block (see
     * {@link #generate}) to the workflow node name and connection key that block is keyed under — used to keep a
     * {@code WorkflowTestConfiguration}'s saved connections in sync with the draft workflow (test runs source
     * connections from there, not from {@code AiAgentChannel}/{@code AiAgentElement}/{@code AiAgent.settings}
     * directly). Mirrors {@link #buildTools}'/{@link #buildClusterElements}'s own node-naming/counter logic exactly,
     * rather than sharing code with them, so the mapping can never silently drift from {@link #generate}'s actual
     * output — see the "Known follow-ups" note on this invariant in {@code docs/agents/agents.md}.
     *
     * <p>
     * Unlike the pre-built-ins version of this method, EVERY group {@link #buildTools} may emit — including groups that
     * never carry a {@code connections} block themselves (the {@code SKILL} aggregate, {@code SUB_AGENT} rows,
     * {@code askUserQuestion}/{@code autoMemory}/{@code skillManagement} built-ins) — has its node name explicitly
     * reserved here too (via {@code nextNodeName} with no {@link ConnectionRef} added). This is required, not
     * defensive: several of those no-connection groups and the connection-needing {@code webSearch} built-in /
     * {@code APPROVAL_TOOL} node all share the {@code aiAgentUtils}/{@code approval} node-name counters, so
     * {@code webSearch}'s or the approval tool's own node name depends on exactly how many earlier same-component nodes
     * were named before it — skipping any of them here (the old shortcut, safe only because nothing connection-needing
     * used to follow {@code SKILL}/{@code SUB_AGENT}/{@code AUTO_MEMORY} in the counter) would silently compute the
     * wrong node name for those refs.
     */
    public static List<ConnectionRef> buildConnectionRefs(
        AiAgent agent, List<AiAgentChannel> channels, List<AiAgentElement> elements) {

        Objects.requireNonNull(agent, "agent");
        Objects.requireNonNull(channels, "channels");
        Objects.requireNonNull(elements, "elements");

        List<ConnectionRef> connectionRefs = new ArrayList<>();

        for (ChannelNode channelNode : buildChannelNodes(channels)) {
            ChannelDefinition definition = channelNode.definition();

            if (definition.connectionRequired()) {
                connectionRefs.add(
                    new ConnectionRef(
                        channelNode.channel()
                            .getId(),
                        ConnectionRefOwnerKind.CHANNEL, channelNode.nodeName(),
                        componentNameOf(definition.triggerType())));
            }
        }

        Map<String, Integer> nodeCounters = new LinkedHashMap<>();

        List<AiAgentElement> modelElements = elementsOfKind(elements, KIND_MODEL);

        if (!modelElements.isEmpty()) {
            AiAgentElement modelElement = modelElements.get(0);
            String provider = MapUtils.getRequiredString(modelElement.getParameters(), MODEL_PARAM_PROVIDER);

            connectionRefs.add(
                new ConnectionRef(
                    modelElement.getId(), ConnectionRefOwnerKind.ELEMENT, AI_AGENT_NODE_NAME,
                    nextNodeName(nodeCounters, provider)));
        }

        List<ApprovalDeliveryChannel> approvalDeliveryChannels = buildApprovalDeliveryChannels(channels);

        boolean approvalGateEnabled = singleElementOfKind(elements, KIND_APPROVAL_GATE) != null;

        for (ToolSequenceEntry entry : buildToolSequence(elementsOfKind(elements, KIND_TOOL), approvalGateEnabled)) {
            List<AiAgentElement> gatedGroup = entry.gatedGroup();

            if (gatedGroup != null) {
                // Mirrors buildApprovalGateElement's exact traversal order: the gate's own node name is consumed
                // first (it never carries a connections block, so no ref is added for it), then every gated TOOL row
                // (always gets a ref, same as an ungated TOOL — see buildToolElement), then every approval delivery
                // channel (a ref only when its component needs a connection — see buildApprovalChannelEntries).
                nextNodeName(nodeCounters, AI_AGENT_UTILS_COMPONENT);

                for (AiAgentElement gatedTool : gatedGroup) {
                    String componentName = MapUtils.getRequiredString(
                        gatedTool.getParameters(), TOOL_PARAM_COMPONENT_NAME);
                    String nodeName = nextNodeName(nodeCounters, componentName);

                    connectionRefs.add(
                        new ConnectionRef(
                            gatedTool.getId(), ConnectionRefOwnerKind.ELEMENT, AI_AGENT_NODE_NAME, nodeName));
                }

                addApprovalChannelConnectionRefs(approvalDeliveryChannels, nodeCounters, connectionRefs);
            } else {
                AiAgentElement toolElement = Objects.requireNonNull(entry.ungatedTool());
                String componentName = MapUtils.getRequiredString(
                    toolElement.getParameters(), TOOL_PARAM_COMPONENT_NAME);
                String nodeName = nextNodeName(nodeCounters, componentName);

                connectionRefs.add(
                    new ConnectionRef(
                        toolElement.getId(), ConnectionRefOwnerKind.ELEMENT, AI_AGENT_NODE_NAME, nodeName));
            }
        }

        Map<String, ?> settings = agent.getSettings();

        List<AiAgentElement> skillElements = elementsOfKind(elements, KIND_SKILL);

        if (!skillElements.isEmpty()) {
            nextNodeName(nodeCounters, AI_AGENT_UTILS_COMPONENT);
        }

        for (int i = 0, subAgentCount = elementsOfKind(elements, AiAgentElement.KIND_SUB_AGENT)
            .size(); i < subAgentCount; i++) {

            nextNodeName(nodeCounters, WORKFLOW_COMPONENT);
        }

        if (AiAgentSettings.isAskUserQuestionEnabled(settings)) {
            nextNodeName(nodeCounters, AI_AGENT_UTILS_COMPONENT);
        }

        if (AiAgentSettings.isAutoMemoryEnabled(settings)) {
            nextNodeName(nodeCounters, AI_AGENT_UTILS_COMPONENT);
        }

        if (AiAgentSettings.isSkillManagementEnabled(settings)) {
            for (int i = 0; i < SKILL_MANAGEMENT_ACTION_NAMES.size(); i++) {
                nextNodeName(nodeCounters, AI_AGENT_UTILS_COMPONENT);
            }
        }

        if (AiAgentSettings.isWebSearchEnabled(settings)) {
            // The brave counter, not aiAgentUtils: webSearch is the brave component's own tool element.
            String nodeName = nextNodeName(nodeCounters, BRAVE_COMPONENT);

            connectionRefs.add(
                new ConnectionRef(
                    agent.getId(), ConnectionRefOwnerKind.AGENT_SETTINGS, AI_AGENT_NODE_NAME, nodeName));
        }

        if (singleElementOfKind(elements, AiAgentElement.KIND_APPROVAL_TOOL) != null) {
            nextNodeName(nodeCounters, APPROVAL_COMPONENT);

            addApprovalChannelConnectionRefs(approvalDeliveryChannels, nodeCounters, connectionRefs);
        }

        return connectionRefs;
    }

    /**
     * Reserves a node name for every {@code approvalChannelElements} row and adds a {@link ConnectionRef} for those
     * whose component needs a connection — shared by the approval gate's own channel list and the {@code APPROVAL_TOOL}
     * element's channel list (see {@link #buildApprovalChannelEntries}, this method's production-code counterpart).
     */
    private static void addApprovalChannelConnectionRefs(
        List<ApprovalDeliveryChannel> deliveryChannels, Map<String, Integer> nodeCounters,
        List<ConnectionRef> connectionRefs) {

        for (ApprovalDeliveryChannel deliveryChannel : deliveryChannels) {
            String componentName = deliveryChannel.componentName();
            String nodeName = nextNodeName(nodeCounters, componentName);

            if (ApprovalChannelDefinitions.getApprovalChannelDefinition(componentName)
                .connectionRequired()) {

                // Owner is the agent_channel row itself, so the delivery node reuses the very connection the
                // channel's trigger already resolved — an approval on Slack goes out over the same workspace the
                // conversation came in on, with nothing extra to configure.
                connectionRefs.add(
                    new ConnectionRef(
                        deliveryChannel.channelId(), ConnectionRefOwnerKind.CHANNEL, AI_AGENT_NODE_NAME, nodeName));
            }
        }
    }

    /**
     * The agent's own channels, projected onto the {@code APPROVAL_CHANNELS} elements an approval is delivered through.
     * Channels that cannot carry an approval are dropped — {@code schedule} has nobody to ask and
     * {@code workflowCall}'s caller is another workflow — and an agent left with none falls back to the chat channel at
     * run time, which is the pre-existing behaviour for an empty list.
     */
    private static List<ApprovalDeliveryChannel> buildApprovalDeliveryChannels(List<AiAgentChannel> channels) {
        List<ApprovalDeliveryChannel> deliveryChannels = new ArrayList<>();

        for (AiAgentChannel channel : channels) {
            ChannelDefinition channelDefinition = ChannelDefinitions.getChannelDefinition(channel.getChannelType());

            ApprovalDelivery approvalDelivery = channelDefinition.approvalDelivery();

            if (approvalDelivery == null) {
                continue;
            }

            deliveryChannels.add(
                new ApprovalDeliveryChannel(
                    channel.getId(), approvalDelivery.componentName(), approvalDelivery.elementName()));
        }

        return deliveryChannels;
    }

    /**
     * One approval destination derived from an agent channel: which cluster element carries the request, and which
     * {@code agent_channel} row owns the connection it goes out on.
     */
    private record ApprovalDeliveryChannel(long channelId, String componentName, String elementName) {
    }

    private static String componentNameOf(String nodeType) {
        return nodeType.split("/")[0];
    }

    // --- Trigger ordering / naming --------------------------------------------------------------------------

    /**
     * Orders channels for trigger generation — chat first, workflowCall second, then every remaining channel by
     * {@code position} — then assigns each a {@code <channelType>_<n>} node name, {@code n} counting per channelType in
     * that order.
     */
    private static List<ChannelNode> buildChannelNodes(List<AiAgentChannel> channels) {
        List<AiAgentChannel> chatChannels = channelsOfType(channels, AiAgentChannelType.CHAT);
        List<AiAgentChannel> workflowCallChannels = channelsOfType(channels, AiAgentChannelType.WORKFLOW_CALL);
        List<AiAgentChannel> remainingChannels = channels.stream()
            .filter(
                channel -> !AiAgentChannelType.CHAT.equals(channel.getChannelType())
                    && !AiAgentChannelType.WORKFLOW_CALL.equals(channel.getChannelType()))
            .sorted(Comparator.comparingInt(AiAgentChannel::getPosition))
            .toList();

        List<AiAgentChannel> orderedChannels = new ArrayList<>(chatChannels);

        orderedChannels.addAll(workflowCallChannels);
        orderedChannels.addAll(remainingChannels);

        Map<String, Integer> countByChannelType = new LinkedHashMap<>();
        List<ChannelNode> channelNodes = new ArrayList<>();

        for (AiAgentChannel channel : orderedChannels) {
            String channelType = channel.getChannelType();
            int n = countByChannelType.merge(channelType, 1, Integer::sum);
            String nodeName = channelType + "_" + n;

            channelNodes.add(new ChannelNode(channel, nodeName, ChannelDefinitions.getChannelDefinition(channelType)));
        }

        return channelNodes;
    }

    private static List<AiAgentChannel> channelsOfType(List<AiAgentChannel> channels, String channelType) {
        return channels.stream()
            .filter(channel -> channelType.equals(channel.getChannelType()))
            .sorted(Comparator.comparingInt(AiAgentChannel::getPosition))
            .toList();
    }

    // --- Triggers ----------------------------------------------------------------------------------------------

    private static List<Map<String, Object>> buildTriggers(List<ChannelNode> channelNodes) {
        List<Map<String, Object>> triggers = new ArrayList<>();

        for (ChannelNode channelNode : channelNodes) {
            triggers.add(buildTrigger(channelNode));
        }

        return triggers;
    }

    private static Map<String, Object> buildTrigger(ChannelNode channelNode) {
        AiAgentChannel channel = channelNode.channel();
        ChannelDefinition definition = channelNode.definition();

        Map<String, Object> trigger = new LinkedHashMap<>();

        trigger.put("name", channelNode.nodeName());
        trigger.put("type", definition.triggerType());
        trigger.put("parameters", buildTriggerParameters(channel, definition));

        if (definition.connectionRequired()) {
            trigger.put(WorkflowExtConstants.CONNECTIONS, buildConnections(definition.triggerType()));
        }

        return trigger;
    }

    private static Map<String, Object> buildTriggerParameters(AiAgentChannel channel, ChannelDefinition definition) {
        Map<String, Object> parameters = new LinkedHashMap<>(definition.defaultTriggerParameters());

        // Per-instance config (schedule's cron/timezone, messaging filters) layered on top from the owning
        // agent_channel row. SCHEDULE_PROMPT_PARAMETER is excluded — it feeds branch_in's envelope text, not a
        // property any trigger declares — and so is SCHEDULE_NAME_PARAMETER, which exists only to label the row
        // in the UI and would otherwise be emitted as an unknown trigger property.
        for (Map.Entry<String, ?> entry : channel.getParameters()
            .entrySet()) {

            if (!SCHEDULE_PROMPT_PARAMETER.equals(entry.getKey())
                && !SCHEDULE_NAME_PARAMETER.equals(entry.getKey())) {
                parameters.put(entry.getKey(), entry.getValue());
            }
        }

        if (AiAgentChannelType.WORKFLOW_CALL.equals(channel.getChannelType())) {
            parameters.put(INPUT_SCHEMA_PARAMETER, WORKFLOW_CALL_INPUT_SCHEMA);
        }

        return parameters;
    }

    // --- branch_in -----------------------------------------------------------------------------------------------

    private static Map<String, Object> buildBranchIn(AiAgent agent, List<ChannelNode> channelNodes) {
        Map<String, Object> parameters = new LinkedHashMap<>();

        parameters.put("expression", "${" + JobInputConstants.TRIGGER_NAME_INPUT + "}");

        List<Map<String, Object>> cases = new ArrayList<>();

        for (ChannelNode channelNode : channelNodes) {
            Map<String, Object> envelopeSetTask = buildEnvelopeSetTask(agent, channelNode);

            Map<String, Object> oneCase = new LinkedHashMap<>();

            oneCase.put("key", channelNode.nodeName());
            oneCase.put(
                "tasks",
                AiAgentChannelType.SLACK.equals(channelNode.channel()
                    .getChannelType())
                        ? List.of(buildSlackBotEchoGuard(channelNode, envelopeSetTask))
                        : List.of(envelopeSetTask));

            cases.add(oneCase);
        }

        parameters.put("cases", cases);

        Map<String, Object> branchIn = new LinkedHashMap<>();

        branchIn.put("name", BRANCH_IN_NAME);
        branchIn.put("type", BRANCH_TYPE);
        branchIn.put("parameters", parameters);

        return branchIn;
    }

    private static Map<String, Object> buildEnvelopeSetTask(AiAgent agent, ChannelNode channelNode) {
        Map<String, Object> envelopeSetTask = new LinkedHashMap<>();

        envelopeSetTask.put("name", "envelope_" + channelNode.nodeName());
        envelopeSetTask.put("type", VAR_SET_TYPE);
        envelopeSetTask.put("parameters", Map.of("value", buildEnvelope(agent, channelNode)));

        return envelopeSetTask;
    }

    /**
     * Guards the slack case against Slack's own webhook redelivering the bot's OWN reply as a new inbound event:
     * {@code slack/v1/sendChannelMessage} posts as the configured bot, and {@code slack/v1/anyEvent} has no built-in
     * filter to exclude a bot's own messages from the events it delivers, so without this guard every agent reply would
     * loop back in as a new inbound turn, get answered again, get redelivered again — an unbounded
     * agent-replies-to-itself loop (and, since each iteration is a real LLM call, an unbounded cost loop too).
     *
     * <p>
     * <b>Detection.</b> {@code SlackAnyEventTrigger} declares no output schema — {@code webhookRequest} returns Slack's
     * raw Events API {@code event} object verbatim — so the trigger node's own output carries whatever fields that
     * event has. Per the Slack Events API, a message posted by a bot (including via {@code chat.postMessage}, what the
     * reply task uses) carries a {@code bot_id} field; a message from a "legacy" bot integration instead carries
     * {@code subtype == "bot_message"}. Checking both covers either shape.
     *
     * <p>
     * <b>Guard mechanism: {@code terminate/v1}, not a silent {@code branch_out} case.</b> A nested {@code condition/v1}
     * wraps the channel's normal envelope task: the FALSE arm (not a bot event) is the ordinary
     * {@code envelope_slack_<n>} {@code var/v1/set} task; the TRUE arm (a bot event) is a {@code terminate/v1} task,
     * which stops the whole job cleanly before {@code aiAgent_1} ever runs (see {@code TerminateTaskDispatcher}: it
     * marks every ancestor task execution — the condition, and {@code branch_in} itself — {@code CANCELLED} and
     * publishes a job-stop event). This was chosen over the cheaper-looking alternative of letting the envelope task
     * run as normal but making {@code branch_out}'s {@code channel} field a SpEL conditional that emits an unmatched
     * sentinel (e.g. {@code "slackSilent"}) for a bot event, so {@code branch_out} falls through its default (no case,
     * no reply): that alternative is strictly worse because {@code aiAgent_1} — a real, billed LLM call — would still
     * run on every bot-echoed event, merely suppressing the reply rather than avoiding the cost. {@code terminate/v1}
     * avoids the LLM call entirely, so it is the primary approach; the {@code branch_out}-sentinel shape is documented
     * here only as the fallback if {@code terminate/v1} were ever found unsuitable inside a nested {@code branch_in}
     * case (it has not been).
     */
    private static Map<String, Object> buildSlackBotEchoGuard(ChannelNode channelNode, Map<String, Object> normalTask) {
        String nodeName = channelNode.nodeName();

        Map<String, Object> terminateTask = new LinkedHashMap<>();

        terminateTask.put("name", "terminateBotEvent_" + nodeName);
        terminateTask.put("type", TERMINATE_TYPE);
        terminateTask.put(
            "parameters",
            Map.of(
                "message",
                "Ignored a Slack event that originated from this agent's own bot reply (echo-loop guard)."));

        Map<String, Object> conditionParameters = new LinkedHashMap<>();

        // NOT a ${...} template: SpelEvaluator's TEXT-accessor grammar (used everywhere else in this generator,
        // e.g. envelope fields) only allows a single bare property path inside ${...} (see
        // SpelEvaluator.INVALID_ACCESSOR_PATTERN — no operators, comparisons, or literals). A boolean SpEL formula
        // needs the OTHER supported form instead: a leading "=" with no ${}/{} wrapper at all (SpelEvaluator's
        // FORMULA_EXPRESSION_PATTERN), the same convention condition/v1's own "Raw Expression" UI mode uses.
        //
        // Bracket ['key'] indexing, not dot property access: SlackAnyEventTrigger declares no output schema, so
        // bot_id is very often simply ABSENT from the map (present only on a bot-posted event). ByteChef's custom
        // SpEL MapPropertyAccessor (which is what makes dot-notation like "slack_1.text" work elsewhere in this
        // generator) does not resolve an absent key to null the way plain Map#get does; SpEL's built-in bracket
        // indexer does, so this is the only form that is safe against a missing bot_id/subtype key.
        conditionParameters.put("rawExpression", true);
        conditionParameters.put(
            "expression",
            "=" + nodeName + "['" + SLACK_BOT_ID_FIELD + "'] != null or " + nodeName + "['" + SLACK_SUBTYPE_FIELD
                + "'] == '" + SLACK_BOT_MESSAGE_SUBTYPE + "'");
        conditionParameters.put("caseTrue", List.of(terminateTask));
        conditionParameters.put("caseFalse", List.of(normalTask));

        Map<String, Object> conditionTask = new LinkedHashMap<>();

        conditionTask.put("name", "botGuard_" + nodeName);
        conditionTask.put("type", CONDITION_TYPE);
        conditionTask.put("parameters", conditionParameters);

        return conditionTask;
    }

    private static Map<String, Object> buildEnvelope(AiAgent agent, ChannelNode channelNode) {
        AiAgentChannel channel = channelNode.channel();
        EnvelopeMapping mapping = channelNode.definition()
            .envelopeMapping();

        Map<String, Object> envelope = new LinkedHashMap<>();

        if (AiAgentChannelType.SCHEDULE.equals(channel.getChannelType())) {
            // Overrides EnvelopeMapping.text() (null in the registry — no incoming message to derive it from) and
            // EnvelopeMapping.conversationId() ("=uuid()", a fresh non-deterministic id each run — incompatible with
            // the byte-identical-output requirement). Per the interface note: text is the channel's stored prompt,
            // conversationId is a content-derived UUID so regenerating the same agent yields the same value.
            envelope.put(FIELD_TEXT, MapUtils.getRequiredString(channel.getParameters(), SCHEDULE_PROMPT_PARAMETER));
            envelope.put(FIELD_CONVERSATION_ID, scheduleConversationId(agent, channel));
        } else {
            envelope.put(FIELD_TEXT, substitute(mapping.text(), channelNode.nodeName()));
            envelope.put(FIELD_CONVERSATION_ID, substitute(mapping.conversationId(), channelNode.nodeName()));
        }

        envelope.put(FIELD_ATTACHMENTS, substituteAttachments(mapping.attachments(), channelNode.nodeName()));
        envelope.put(FIELD_REPLY_TO, substitute(mapping.replyTo(), channelNode.nodeName()));

        // Only channels whose reply action takes a sender address carry this; omitting it otherwise keeps every
        // pre-existing channel's generated envelope byte-identical.
        if (mapping.replyFrom() != null) {
            envelope.put(FIELD_REPLY_FROM, substitute(mapping.replyFrom(), channelNode.nodeName()));
        }
        envelope.put(FIELD_CHANNEL, channel.getChannelType());

        return envelope;
    }

    /**
     * Deterministic per-schedule chat-memory key, per the interface note: content-derived from the agent's uuid and the
     * channel's position, so regenerating an unchanged agent yields the same value while each schedule row still gets
     * its own key.
     */
    private static String scheduleConversationId(AiAgent agent, AiAgentChannel channel) {
        String seed = agent.getUuid() + "_" + channel.getPosition();

        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8))
            .toString();
    }

    private static Object substitute(String expression, String nodeName) {
        if (expression == null) {
            return null;
        }

        return expression.replace(NODE_TOKEN, nodeName);
    }

    /**
     * {@code EnvelopeMapping.attachments()} is either a real expression (chat) or the literal
     * {@value #EMPTY_ARRAY_LITERAL} (every other channel — see {@link EnvelopeMapping#attachments()}'s doc). The
     * literal means an actual empty array, not the two-character string, so it is emitted as {@link List#of()}.
     */
    private static Object substituteAttachments(String expression, String nodeName) {
        if (EMPTY_ARRAY_LITERAL.equals(expression)) {
            return List.of();
        }

        return substitute(expression, nodeName);
    }

    // --- aiAgent_1 -----------------------------------------------------------------------------------------------

    private static Map<String, Object> buildAiAgentNode(
        AiAgent agent, List<AiAgentChannel> channels, List<AiAgentElement> elements,
        Function<Long, SubAgentRef> subAgentResolver) {

        Map<String, Object> parameters = new LinkedHashMap<>();

        parameters.put("format", "SIMPLE");
        parameters.put("userPrompt", "${" + BRANCH_IN_NAME + "." + FIELD_TEXT + "}");

        String instructions = agent.getInstructions();

        if (instructions != null && !instructions.isBlank()) {
            parameters.put(SYSTEM_PROMPT_PARAMETER, instructions);
        }

        parameters.put(FIELD_ATTACHMENTS, "${" + BRANCH_IN_NAME + "." + FIELD_ATTACHMENTS + "}");

        Map<String, Object> aiAgentNode = new LinkedHashMap<>();

        aiAgentNode.put("name", AI_AGENT_NODE_NAME);
        aiAgentNode.put("type", AI_AGENT_TYPE);
        aiAgentNode.put("parameters", parameters);
        aiAgentNode.put("clusterElements", buildClusterElements(agent, channels, elements, subAgentResolver));

        return aiAgentNode;
    }

    // --- aiAgent_1 clusterElements ---------------------------------------------------------------------------------

    private static final String KIND_MODEL = "MODEL";
    private static final String KIND_TOOL = "TOOL";
    private static final String KIND_SKILL = "SKILL";
    private static final String KIND_KNOWLEDGE_BASE = "KNOWLEDGE_BASE";
    private static final String KIND_CHAT_MEMORY = "CHAT_MEMORY";
    private static final String KIND_APPROVAL_GATE = "APPROVAL_GATE";

    private static final String CLUSTER_KEY_MODEL = "model";
    private static final String CLUSTER_KEY_TOOLS = "tools";
    private static final String CLUSTER_KEY_RAG = "rag";
    private static final String CLUSTER_KEY_CHAT_MEMORY = "chatMemory";
    private static final String CLUSTER_KEY_VECTOR_STORE = "vectorStore";

    /**
     * {@code MODEL} element parameters, per the interface note ({@code AiAgent.parameters carry {provider, model}}).
     */
    private static final String MODEL_PARAM_PROVIDER = "provider";
    private static final String MODEL_PARAM_MODEL = "model";

    /**
     * Nested map of the model node's remaining properties (temperature, maxTokens, …), mirroring
     * {@link #TOOL_PARAM_PARAMETERS} on a {@code TOOL} row — see {@link #buildModelElement}.
     */
    private static final String MODEL_PARAM_PARAMETERS = "parameters";

    /**
     * Every LLM provider component names its model cluster element {@code "model"} — verified against
     * {@code OpenAiChatModel.CLUSTER_ELEMENT_DEFINITION} ({@code ComponentDsl.clusterElement("model")} in
     * {@code server/libs/modules/components/ai/llm/open-ai/.../cluster/OpenAiChatModel.java}), whose only required
     * property is {@code OpenAiConstants.CHAT_MODEL_PROPERTY = string(MODEL)} — key {@code "model"}.
     */
    private static final String MODEL_ELEMENT_NAME = "model";

    // TOOL element parameters, per the interface note.
    private static final String TOOL_PARAM_COMPONENT_NAME = "componentName";
    private static final String TOOL_PARAM_COMPONENT_VERSION = "componentVersion";
    private static final String TOOL_PARAM_ACTION_NAME = "actionName";
    private static final String TOOL_PARAM_PARAMETERS = "parameters";

    /**
     * Optional boolean {@code TOOL} row parameter: when {@code true}, this tool is pulled out of the flat
     * {@code tools[]} array and nested inside the agent's single generated {@code approvalGateTool} — see this class's
     * "HITL approval gate" javadoc. Shared with {@code AiAgentFacadeImpl}'s publish validation via
     * {@link AiAgentElement#PARAM_REQUIRES_APPROVAL}.
     */
    private static final String TOOL_PARAM_REQUIRES_APPROVAL = AiAgentElement.PARAM_REQUIRES_APPROVAL;

    /**
     * {@code aiAgentUtils}'s {@code approvalGateTool} cluster element — verified against
     * {@code AiAgentUtilsApprovalGateTool} ({@code clusterElement("approvalGateTool")}, required property
     * {@code "name"}, optional integer property {@code ToolConstants.APPROVAL_EXPIRES_IN = "approvalExpiresIn"} and
     * optional string property {@code ToolConstants.APPROVAL_EXPIRES_IN_UNIT = "approvalExpiresInUnit"}), children
     * {@code TOOLS} (key {@code "tools"} — reuses {@link #CLUSTER_KEY_TOOLS}) and {@code APPROVAL_CHANNELS} (key
     * {@code "approvalChannels"}, {@code ApprovalChannelFunction.APPROVAL_CHANNELS}). The generator always supplies the
     * literal gate name below — there is no per-agent way to customize it (yet).
     */
    private static final String APPROVAL_GATE_ELEMENT_NAME = "approvalGateTool";
    private static final String APPROVAL_GATE_NAME_PARAM = "name";
    private static final String APPROVAL_GATE_NAME_VALUE = "Requires approval";
    private static final String APPROVAL_EXPIRES_IN_PARAM = "approvalExpiresIn";
    private static final String APPROVAL_EXPIRES_IN_UNIT_PARAM = "approvalExpiresInUnit";
    private static final String CLUSTER_KEY_APPROVAL_CHANNELS = "approvalChannels";

    /**
     * {@code aiAgentUtils}'s SKILL aggregate cluster element — verified against {@code AiAgentUtilsSkillsTool}
     * ({@code clusterElement("skillsTool")}, properties {@code array("skills").items(integer("skillId")...)}), in
     * {@code server/libs/modules/components/ai/agent/utils/.../cluster/}, and {@code AiAgentUtilsComponentHandler}'s
     * {@code component("aiAgentUtils")}. Emitted whenever the agent has at least one {@code SKILL} row — attaching a
     * skill is itself the opt-in, so there is no separate settings toggle to keep in sync — see {@link #buildTools}.
     */
    private static final String AI_AGENT_UTILS_COMPONENT = "aiAgentUtils";
    private static final String SKILLS_TOOL_ELEMENT_NAME = "skillsTool";
    private static final String SKILLS_PARAM = "skills";

    /**
     * Settings-driven built-in {@code aiAgentUtils} elements (see {@code AiAgentSettings}), each a bare
     * {@code aiAgentUtils/v1/<elementName>} entry with empty {@code parameters} — every property each one declares is
     * optional/LLM-fillable, same convention as the pre-existing {@code askUserQuestionTool}/{@code autoMemoryTool}
     * entries below.
     *
     * <p>
     * {@code askUserQuestionTool} — verified against {@code AiAgentUtilsAskUserQuestionTool}
     * ({@code clusterElement("askUserQuestionTool")}, no declared properties — it reads suspend/SSE context, not input
     * parameters). {@code autoMemoryTool} — {@code AiAgentUtilsAutoMemoryTool}
     * ({@code clusterElement("autoMemoryTool")}, no properties; replaces the retired {@code AUTO_MEMORY} element kind).
     * The {@code webSearch} built-in is deliberately NOT in this list — it is not an {@code aiAgentUtils} element at
     * all. See {@link #buildWebSearchToolElement}.
     */
    private static final String ASK_USER_QUESTION_TOOL_ELEMENT_NAME = "askUserQuestionTool";
    private static final String AUTO_MEMORY_TOOL_ELEMENT_NAME = "autoMemoryTool";

    /**
     * The five {@code AiSkill} management actions, each wrapped by {@code ComponentDsl.tool(actionDefinition)} in
     * {@code AiAgentUtilsComponentHandler} (lines ~113-117) — that helper names the resulting cluster element after
     * {@code actionDefinition.getName()} verbatim (see {@code ComponentDsl.tool}), so the element name IS the action
     * name. Verified action names: {@code AiAgentUtilsCreateAiSkillAction} → {@code "createAiSkill"},
     * {@code AiAgentUtilsUpdateAiSkillAction} → {@code "updateAiSkill"}, {@code AiAgentUtilsDeleteAiSkillAction} →
     * {@code "deleteAiSkill"}, {@code AiAgentUtilsAppendFilesToAiSkillAction} → {@code "appendFilesToAiSkill"},
     * {@code AiAgentUtilsRemoveFileFromAiSkillAction} → {@code "removeFileFromAiSkill"}. This generator's own
     * deterministic order (create, update, delete, append, remove) — NOT the component handler's, which registers these
     * five alphabetically (append, create, delete, remove, update; see both its {@code clusterElements} list and its
     * {@code .actions(...)} list, lines ~113-117); the two orders are independent, since a cluster element's position
     * in {@code AiAgentUtilsComponentHandler}'s list has no bearing on what order THIS generator chooses to emit its
     * own TOOL entries in. Emission is gated by {@code AiAgentSettings#isSkillManagementEnabled} (default ON) — see
     * {@link #buildTools}.
     */
    private static final List<String> SKILL_MANAGEMENT_ACTION_NAMES = List.of(
        "createAiSkill", "updateAiSkill", "deleteAiSkill", "appendFilesToAiSkill", "removeFileFromAiSkill");

    /**
     * The {@code brave} component's own web-search tool — {@code BraveComponentHandler} registers
     * {@code BraveWebSearchAction} ({@code action("webSearch")}) as a cluster element via
     * {@code ComponentDsl.tool(...)}, which names the element after the action. See {@link #buildWebSearchToolElement}.
     */
    private static final String BRAVE_COMPONENT = "brave";
    private static final int BRAVE_COMPONENT_VERSION = 1;
    private static final String BRAVE_WEB_SEARCH_ELEMENT_NAME = "webSearch";

    /**
     * The platform {@code approval} component's LLM-invocable tool — verified against
     * {@code ApprovalRequestApprovalTool} ({@code clusterElement("requestApproval")}, {@code TOOLS} type, properties
     * inherited from {@code ApprovalRequestApprovalAction} — {@code formTitle}, {@code formDescription},
     * {@code inputs}, {@code expiresIn}, {@code expiresInUnit}, all optional) and {@code ApprovalComponentHandler}'s
     * {@code component(APPROVAL)} (default version 1). {@code AiAgentUtilsApprovalGateTool.checkGatableChild} rejects
     * nesting this element inside a gate, so it is ALWAYS emitted as a top-level tool — see {@link #buildTools}.
     */
    private static final String APPROVAL_COMPONENT = "approval";
    private static final String APPROVAL_TOOL_ELEMENT_NAME = "requestApproval";

    /**
     * {@code workflow}'s {@code callAiAgent} cluster element — verified against {@code WorkflowCallAiAgentTool}
     * ({@code clusterElement("callAiAgent")}, properties {@code ToolConstants.TOOL_NAME = "toolName"},
     * {@code ToolConstants.TOOL_DESCRIPTION = "toolDescription"}, {@code "agentUuid"}, {@code "message"}, and
     * {@code "conversationId"}) and {@code WorkflowComponentHandler}'s {@code component(WorkflowConstants.WORKFLOW)}
     * where {@code WORKFLOW = "workflow"}. Unlike the generic {@code callWorkflow} element (arbitrary sub-workflow,
     * dynamic input schema), {@code callAiAgent} targets the fixed {@code {message, conversationId}} contract every
     * AiAgent-generated workflow's {@code workflowCall} channel declares (see {@link #WORKFLOW_CALL_INPUT_SCHEMA}), so
     * this generator emits {@code agentUuid} directly instead of resolving a workflow uuid itself —
     * {@code CallableAiAgentDataSource#resolveAgent} does that mapping at dispatch time.
     */
    private static final String WORKFLOW_COMPONENT = "workflow";
    private static final String CALL_AI_AGENT_ELEMENT_NAME = "callAiAgent";
    private static final String TOOL_NAME_PARAM = "toolName";
    private static final String TOOL_DESCRIPTION_PARAM = "toolDescription";
    private static final String AGENT_UUID_PARAM = "agentUuid";
    private static final String MESSAGE_PARAM = "message";

    /**
     * {@code callAiAgent}'s {@code message} parameter (see the class javadoc above). Wired to a {@code fromAi(...)}
     * expression so the LLM fills it via its own function-calling schema at tool-call time —
     * {@code AbstractToolFacade#extractFromAiResults} scans this cluster element's whole configured parameter tree for
     * embedded {@code fromAi(...)} calls when building that schema (verified against {@code AiAgentToolFacade}), so a
     * top-level string value is sufficient; it does not need to sit inside a nested object.
     */
    private static final String MESSAGE_FROM_AI_EXPRESSION =
        "=fromAi('message', 'The message to send to the sub-agent.')";

    /**
     * {@code callAiAgent}'s {@code conversationId} parameter. Pre-seeding it here — rather than leaving it for the LLM
     * to fill, the way {@code message} is filled via the tool's own function-calling schema — pins every sub-agent
     * invocation from a given parent conversation to the SAME chat-memory thread: without this, the target
     * sub-workflow's {@code workflowCall} trigger's {@code conversationId} field (and therefore its {@code chatMemory}
     * cluster element's {@code ${branch_in.conversationId}} key, see {@link #CHAT_MEMORY_CONVERSATION_ID_EXPRESSION})
     * is left to whatever the LLM happens to supply — unset most of the time, silently pooling every invocation of that
     * sub-agent, across every distinct parent conversation, into one shared memory thread.
     */
    private static final String CALL_AI_AGENT_CONVERSATION_ID_PARAM = "conversationId";

    /**
     * {@code questionAnswerRag}'s {@code rag} cluster element — verified against {@code QuestionAnswerRag}
     * ({@code clusterElement("rag")}, properties {@code filterExpression}, {@code similarityThreshold}, {@code topK})
     * and {@code QuestionAnswerRagComponentHandler.QUESTION_ANSWER_RAG = "questionAnswerRag"}.
     */
    private static final String QUESTION_ANSWER_RAG_COMPONENT = "questionAnswerRag";
    private static final String RAG_ELEMENT_NAME = "rag";
    private static final String RAG_PARAM_TOP_K = "topK";
    private static final String RAG_PARAM_SIMILARITY_THRESHOLD = "similarityThreshold";

    /**
     * {@code knowledgeBase}'s {@code vectorStore} cluster element (the {@code rag} entry's nested vector store) —
     * verified against {@code KnowledgeBaseVectorStore.of(...)} ({@code clusterElement(VECTOR_STORE)} where
     * {@code VectorStoreComponentDefinition.VECTOR_STORE = "vectorStore"}, required property
     * {@code KnowledgeBaseVectorStoreConstants.KNOWLEDGE_BASE_ID = "knowledgeBaseId"}) and
     * {@code KnowledgeBaseVectorStoreConstants.KNOWLEDGE_BASE = "knowledgeBase"}.
     */
    private static final String KNOWLEDGE_BASE_COMPONENT = "knowledgeBase";
    private static final String KNOWLEDGE_BASE_ID_PARAM = "knowledgeBaseId";

    /**
     * {@code chatMemory}'s {@code chatMemory} cluster element (built-in chat memory) — verified against
     * {@code ChatMemory.of(...)} ({@code clusterElement("chatMemory")}, required property
     * {@code ChatMemoryConstants.CONVERSATION_ID = "conversationId"}) and {@code ChatMemoryComponentHandler}'s
     * {@code component(ChatMemoryConstants.CHAT_MEMORY)} where {@code CHAT_MEMORY = "chatMemory"}. Per the interface
     * note, {@code conversationId} is always the literal expression referencing {@code branch_in}'s envelope field —
     * not read from the element's parameters.
     */
    private static final String CHAT_MEMORY_COMPONENT = "chatMemory";
    private static final String CONVERSATION_ID_PARAM = "conversationId";
    private static final String CHAT_MEMORY_CONVERSATION_ID_EXPRESSION =
        "${" + BRANCH_IN_NAME + "." + FIELD_CONVERSATION_ID + "}";

    /**
     * Builds {@code aiAgent_1}'s {@code clusterElements}. Every emitted node name — the {@code model} entry,
     * {@code tools[]} entries, the {@code rag} entry and its nested {@code vectorStore}, and the {@code chatMemory}
     * entry — is assigned from a single {@code nodeCounters} map shared across this whole call, keyed by component name
     * (mirrors {@link #buildChannelNodes}'s per-channelType counter). This is required, not cosmetic: several of these
     * are real, separately-installable components too (e.g. {@code chatMemory} and {@code knowledgeBase} both expose
     * ordinary TOOL actions in addition to their singleton cluster element), so a TOOL row naming
     * {@code componentName = "chatMemory"} would collide with the fixed-suffix {@code CHAT_MEMORY} entry if each kind
     * counted independently. Processing order — model, then tools (TOOL rows/gate, then the SKILL aggregate, then
     * SUB_AGENT rows, then the settings-driven built-ins, then the APPROVAL_TOOL element — see {@link #buildTools}),
     * then rag/vectorStore, then chatMemory — is fixed, so the counter sequence (and therefore every generated name) is
     * deterministic across repeated calls with the same input.
     */
    private static Map<String, Object> buildClusterElements(
        AiAgent agent, List<AiAgentChannel> channels, List<AiAgentElement> elements,
        Function<Long, SubAgentRef> subAgentResolver) {

        Map<String, Integer> nodeCounters = new LinkedHashMap<>();
        Map<String, Object> clusterElements = new LinkedHashMap<>();

        List<AiAgentElement> modelElements = elementsOfKind(elements, KIND_MODEL);

        if (!modelElements.isEmpty()) {
            // Exactly one MODEL element is expected per agent; a duplicate (should never occur) falls back to the
            // first by position, same as every other singleton kind below.
            clusterElements.put(CLUSTER_KEY_MODEL, buildModelElement(modelElements.get(0), nodeCounters));
        }

        List<Map<String, Object>> tools = buildTools(
            agent.getSettings(), channels, elements, subAgentResolver, nodeCounters);

        if (!tools.isEmpty()) {
            clusterElements.put(CLUSTER_KEY_TOOLS, tools);
        }

        List<AiAgentElement> knowledgeBaseElements = elementsOfKind(elements, KIND_KNOWLEDGE_BASE);

        // An ARRAY, and every KNOWLEDGE_BASE row rather than just the first: RAG is a multiple cluster element, so
        // an agent carries one rag entry per knowledge base, each with its own retrieval parameters.
        if (!knowledgeBaseElements.isEmpty()) {
            clusterElements.put(
                CLUSTER_KEY_RAG,
                knowledgeBaseElements.stream()
                    .map(knowledgeBaseElement -> buildRagElement(knowledgeBaseElement, nodeCounters))
                    .toList());
        }

        List<AiAgentElement> chatMemoryElements = elementsOfKind(elements, KIND_CHAT_MEMORY);

        if (!chatMemoryElements.isEmpty()) {
            clusterElements.put(CLUSTER_KEY_CHAT_MEMORY, buildChatMemoryElement(nodeCounters));
        }

        return clusterElements;
    }

    private static List<AiAgentElement> elementsOfKind(List<AiAgentElement> elements, String kind) {
        return elements.stream()
            .filter(element -> kind.equals(element.getKind()))
            .sorted(Comparator.comparingInt(AiAgentElement::getPosition))
            .toList();
    }

    /**
     * The model node's {@code parameters} are the element's own nested {@code parameters} map with {@code model}
     * layered on top — the same round-trip {@link #buildToolElement} does, and for the same reason: every save
     * regenerates the whole definition, so anything the node details panel wrote that this method does not re-emit is
     * silently dropped by the next regeneration. Emitting only {@code model} discarded every advanced model property
     * (temperature, maxTokens, responseFormat, …).
     *
     * <p>
     * {@code model} is written last so a stale copy inside the nested map can never shadow the element's own
     * authoritative {@code model} key, which the picker and publish validation both read.
     * </p>
     */
    private static Map<String, Object> buildModelElement(AiAgentElement element, Map<String, Integer> nodeCounters) {
        Map<String, ?> parameters = element.getParameters();

        String provider = MapUtils.getRequiredString(parameters, MODEL_PARAM_PROVIDER);
        String model = MapUtils.getRequiredString(parameters, MODEL_PARAM_MODEL);
        String type = "%s/v1/%s".formatted(provider, MODEL_ELEMENT_NAME);

        Map<String, Object> modelParameters = new LinkedHashMap<>(
            MapUtils.getMap(parameters, MODEL_PARAM_PARAMETERS, Map.of()));

        modelParameters.put(MODEL_PARAM_MODEL, model);

        String nodeName = nextNodeName(nodeCounters, provider);

        Map<String, Object> modelElement = new LinkedHashMap<>();

        modelElement.put("name", nodeName);
        modelElement.put("type", type);
        modelElement.put("parameters", modelParameters);
        modelElement.put(WorkflowExtConstants.CONNECTIONS, buildClusterElementConnections(type, nodeName));

        return modelElement;
    }

    /**
     * Builds {@code tools[]} in a fixed group order: {@code TOOL} rows (the ONE approval gate, if any gated tool
     * exists, sits inline at the position of the first gated row — see {@link #buildToolSequence}; this placement is
     * deliberately UNCHANGED by the built-ins/approval-tool work below, to keep the pinned HITL fixture's gate-position
     * invariant intact), then the {@code SKILL} aggregate (one entry, omitted only when there are no {@code SKILL}
     * rows), then {@code SUB_AGENT} rows, then the settings-driven built-ins in {@code AiAgentSettings}' table order
     * ({@code askUserQuestion}, {@code autoMemory}, {@code skillManagement}'s five tools, {@code webSearch}), then —
     * last of all, and NEVER inside the gate (see {@code AiAgentElement
     * .KIND_APPROVAL_TOOL}'s javadoc) — the {@code APPROVAL_TOOL} element. Every group is position-ordered within
     * itself. {@code nodeCounters} is shared with the rest of {@code buildClusterElements} — see that method's javadoc
     * for why.
     */
    private static List<Map<String, Object>> buildTools(
        Map<String, ?> settings, List<AiAgentChannel> channels, List<AiAgentElement> elements,
        Function<Long, SubAgentRef> subAgentResolver, Map<String, Integer> nodeCounters) {

        List<Map<String, Object>> tools = new ArrayList<>();

        List<ApprovalDeliveryChannel> approvalDeliveryChannels = buildApprovalDeliveryChannels(channels);
        AiAgentElement approvalGateElement = singleElementOfKind(elements, KIND_APPROVAL_GATE);

        for (ToolSequenceEntry entry : buildToolSequence(
            elementsOfKind(elements, KIND_TOOL), approvalGateElement != null)) {
            List<AiAgentElement> gatedGroup = entry.gatedGroup();

            if (gatedGroup != null) {
                tools.add(
                    buildApprovalGateElement(gatedGroup, approvalDeliveryChannels, approvalGateElement, nodeCounters));
            } else {
                tools.add(buildToolElement(Objects.requireNonNull(entry.ungatedTool()), nodeCounters));
            }
        }

        List<AiAgentElement> skillElements = elementsOfKind(elements, KIND_SKILL);

        if (!skillElements.isEmpty()) {
            tools.add(buildSkillsElement(skillElements, nodeCounters));
        }

        for (AiAgentElement element : elementsOfKind(elements, AiAgentElement.KIND_SUB_AGENT)) {
            tools.add(buildSubAgentElement(element, nodeCounters, subAgentResolver));
        }

        if (AiAgentSettings.isAskUserQuestionEnabled(settings)) {
            tools.add(buildBuiltInToolElement(ASK_USER_QUESTION_TOOL_ELEMENT_NAME, nodeCounters));
        }

        if (AiAgentSettings.isAutoMemoryEnabled(settings)) {
            tools.add(buildBuiltInToolElement(AUTO_MEMORY_TOOL_ELEMENT_NAME, nodeCounters));
        }

        if (AiAgentSettings.isSkillManagementEnabled(settings)) {
            for (String actionName : SKILL_MANAGEMENT_ACTION_NAMES) {
                tools.add(buildBuiltInToolElement(actionName, nodeCounters));
            }
        }

        if (AiAgentSettings.isWebSearchEnabled(settings)) {
            tools.add(buildWebSearchToolElement(nodeCounters));
        }

        if (singleElementOfKind(elements, AiAgentElement.KIND_APPROVAL_TOOL) != null) {
            tools.add(buildApprovalToolElement(approvalDeliveryChannels, nodeCounters));
        }

        return tools;
    }

    /**
     * A bare {@code aiAgentUtils/v1/<elementName>} entry with empty {@code parameters} — shared shape for every
     * settings-driven built-in that needs no {@code connections} block; see
     * {@link #ASK_USER_QUESTION_TOOL_ELEMENT_NAME}'s javadoc for the per-element verification citations.
     */
    private static Map<String, Object> buildBuiltInToolElement(String elementName, Map<String, Integer> nodeCounters) {
        String type = "%s/v1/%s".formatted(AI_AGENT_UTILS_COMPONENT, elementName);

        Map<String, Object> element = new LinkedHashMap<>();

        element.put("name", nextNodeName(nodeCounters, AI_AGENT_UTILS_COMPONENT));
        element.put("type", type);
        element.put("parameters", Map.of());

        return element;
    }

    /**
     * {@code brave/v1/webSearch} — the {@code brave} component's OWN tool cluster element, on its own component, with
     * its own connection.
     * <p>
     * It deliberately does not use {@code aiAgentUtils/v1/braveWebSearchTool}. That element sits on
     * {@code aiAgentUtils}, which has no connection of its own, so it could only reach a Brave connection through a
     * hand-wired {@code connections} block pointing at a different component than the node's own type — and it reads
     * that connection under {@code "braveApiKey"} while {@code BraveConnection} stores
     * {@code Authorization.API_TOKEN = "api_token"}, so the key never arrived and its {@code apply} silently returned
     * zero tools. {@code BraveComponentHandler} already registers {@code BraveWebSearchAction} as a tool cluster
     * element, so the connection, the key names, and the deployment/test-config wiring all follow the ordinary
     * component path with no special case.
     */
    private static Map<String, Object> buildWebSearchToolElement(Map<String, Integer> nodeCounters) {
        String type = "%s/v%d/%s".formatted(
            BRAVE_COMPONENT, BRAVE_COMPONENT_VERSION, BRAVE_WEB_SEARCH_ELEMENT_NAME);
        String nodeName = nextNodeName(nodeCounters, BRAVE_COMPONENT);

        Map<String, Object> element = new LinkedHashMap<>();

        element.put("name", nodeName);
        element.put("type", type);
        element.put("parameters", Map.of());
        element.put(WorkflowExtConstants.CONNECTIONS, buildClusterElementConnections(type, nodeName));

        return element;
    }

    /**
     * Builds the {@code APPROVAL_TOOL} element's {@code approval/v1/requestApproval} entry — no builder-level
     * {@code parameters} (see {@code AiAgentElement.KIND_APPROVAL_TOOL}'s javadoc), plus the SAME channel-derived
     * approval destinations the gate nests (a fresh set of channel cluster-element instances, reusing
     * {@link #buildApprovalChannelEntries} rather than sharing node identity with the gate's own copy — every
     * {@code clusterElements} entry is a distinct tree node regardless of how many parents reference the same
     * underlying {@code AiAgentChannel} row). Omits {@code clusterElements} entirely (not an empty
     * {@code approvalChannels} array) when the agent has no approval-capable channel, same convention as the gate.
     */
    private static Map<String, Object> buildApprovalToolElement(
        List<ApprovalDeliveryChannel> approvalDeliveryChannels, Map<String, Integer> nodeCounters) {

        String type = "%s/v1/%s".formatted(APPROVAL_COMPONENT, APPROVAL_TOOL_ELEMENT_NAME);

        Map<String, Object> approvalTool = new LinkedHashMap<>();

        approvalTool.put("name", nextNodeName(nodeCounters, APPROVAL_COMPONENT));
        approvalTool.put("type", type);
        approvalTool.put("parameters", Map.of());

        List<Map<String, Object>> channelEntries = buildApprovalChannelEntries(
            approvalDeliveryChannels, nodeCounters);

        if (!channelEntries.isEmpty()) {
            approvalTool.put("clusterElements", Map.of(CLUSTER_KEY_APPROVAL_CHANNELS, channelEntries));
        }

        return approvalTool;
    }

    private static String nextNodeName(Map<String, Integer> nodeCounters, String componentName) {
        int n = nodeCounters.merge(componentName, 1, Integer::sum);

        return componentName + "_" + n;
    }

    private static Map<String, Object> buildToolElement(AiAgentElement element, Map<String, Integer> nodeCounters) {
        Map<String, ?> parameters = element.getParameters();

        String componentName = MapUtils.getRequiredString(parameters, TOOL_PARAM_COMPONENT_NAME);
        int componentVersion = MapUtils.getRequiredInteger(parameters, TOOL_PARAM_COMPONENT_VERSION);
        String actionName = MapUtils.getRequiredString(parameters, TOOL_PARAM_ACTION_NAME);
        String type = "%s/v%d/%s".formatted(componentName, componentVersion, actionName);

        Map<String, ?> toolParameters = MapUtils.getMap(parameters, TOOL_PARAM_PARAMETERS, Map.of());

        String nodeName = nextNodeName(nodeCounters, componentName);

        Map<String, Object> toolElement = new LinkedHashMap<>();

        toolElement.put("name", nodeName);
        toolElement.put("type", type);
        toolElement.put("parameters", toolParameters);
        toolElement.put(WorkflowExtConstants.CONNECTIONS, buildClusterElementConnections(type, nodeName));

        return toolElement;
    }

    // --- HITL approval gate ----------------------------------------------------------------------------------

    private static boolean isRequiresApproval(AiAgentElement toolElement) {
        Object requiresApproval = toolElement.getParameters()
            .get(TOOL_PARAM_REQUIRES_APPROVAL);

        return Boolean.TRUE.equals(requiresApproval);
    }

    /**
     * One step of a position-ordered {@code TOOL} traversal: either a single ungated tool ({@link #ungatedTool} set,
     * {@link #gatedGroup} {@code null}), or the ONE point where the approval gate belongs ({@link #ungatedTool}
     * {@code null}, {@link #gatedGroup} set) — see {@link #buildToolSequence}.
     */
    private record ToolSequenceEntry(@Nullable AiAgentElement ungatedTool, @Nullable List<AiAgentElement> gatedGroup) {
    }

    /**
     * Walks {@code toolElements} (already position-ordered by {@link #elementsOfKind}) and pulls every
     * {@code requiresApproval=true} row out of line into ONE shared list, referenced by a single
     * {@link ToolSequenceEntry} inserted at the position of the FIRST gated row. Every gated row found AFTER that point
     * is appended to the very same list (a mutable reference held by the entry already added to the returned sequence,
     * not copied) rather than getting its own entry — so, however many gated tools there are and wherever they sit
     * relative to the ungated ones, the gate ends up in exactly one deterministic place: where the first gated tool
     * would otherwise have been. Both {@link #buildTools} (which turns this into the real {@code approvalGateTool}
     * cluster element) and {@link #buildConnectionRefs} (which only needs the node-naming traversal order) share this
     * method so they can never drift apart.
     *
     * <p>
     * {@code approvalGateEnabled} is the agent-level master switch — the presence of an
     * {@code AiAgentElement.KIND_APPROVAL_GATE} row. With it off, every tool is emitted ungated regardless of its own
     * {@code requiresApproval} flag: the flags stay on their TOOL rows untouched, so switching the gate back on
     * restores exactly the previous gating rather than making the user re-mark each tool.
     * </p>
     */
    private static List<ToolSequenceEntry> buildToolSequence(
        List<AiAgentElement> toolElements, boolean approvalGateEnabled) {

        List<ToolSequenceEntry> sequence = new ArrayList<>();
        List<AiAgentElement> gatedGroup = null;

        for (AiAgentElement toolElement : toolElements) {
            if (approvalGateEnabled && isRequiresApproval(toolElement)) {
                if (gatedGroup == null) {
                    gatedGroup = new ArrayList<>();

                    sequence.add(new ToolSequenceEntry(null, gatedGroup));
                }

                gatedGroup.add(toolElement);
            } else {
                sequence.add(new ToolSequenceEntry(toolElement, null));
            }
        }

        return sequence;
    }

    private static @Nullable AiAgentElement singleElementOfKind(List<AiAgentElement> elements, String kind) {
        List<AiAgentElement> matches = elementsOfKind(elements, kind);

        return matches.isEmpty() ? null : matches.get(0);
    }

    /**
     * Builds the single {@code aiAgentUtils/v1/approvalGateTool} entry for {@code gatedTools} (already position-ordered
     * — see {@link #buildToolSequence}). Node-name consumption order — the gate's own name, then every gated tool, then
     * every approval channel — is fixed and mirrored exactly by {@link #buildConnectionRefs}.
     */
    private static Map<String, Object> buildApprovalGateElement(
        List<AiAgentElement> gatedTools, List<ApprovalDeliveryChannel> approvalDeliveryChannels,
        @Nullable AiAgentElement approvalGateElement, Map<String, Integer> nodeCounters) {

        String gateNodeName = nextNodeName(nodeCounters, AI_AGENT_UTILS_COMPONENT);

        Map<String, Object> gateParameters = new LinkedHashMap<>();

        gateParameters.put(APPROVAL_GATE_NAME_PARAM, APPROVAL_GATE_NAME_VALUE);

        if (approvalGateElement != null) {
            Map<String, ?> approvalGateParameters = approvalGateElement.getParameters();

            Object expiresIn = approvalGateParameters.get(APPROVAL_EXPIRES_IN_PARAM);

            if (expiresIn != null) {
                gateParameters.put(APPROVAL_EXPIRES_IN_PARAM, expiresIn);
            }

            Object expiresInUnit = approvalGateParameters.get(APPROVAL_EXPIRES_IN_UNIT_PARAM);

            if (expiresInUnit != null) {
                gateParameters.put(APPROVAL_EXPIRES_IN_UNIT_PARAM, expiresInUnit);
            }
        }

        List<Map<String, Object>> gatedToolEntries = new ArrayList<>();

        for (AiAgentElement gatedTool : gatedTools) {
            gatedToolEntries.add(buildToolElement(gatedTool, nodeCounters));
        }

        Map<String, Object> gateClusterElements = new LinkedHashMap<>();

        gateClusterElements.put(CLUSTER_KEY_TOOLS, gatedToolEntries);

        List<Map<String, Object>> channelEntries = buildApprovalChannelEntries(
            approvalDeliveryChannels, nodeCounters);

        if (!channelEntries.isEmpty()) {
            gateClusterElements.put(CLUSTER_KEY_APPROVAL_CHANNELS, channelEntries);
        }

        Map<String, Object> approvalGateTool = new LinkedHashMap<>();

        approvalGateTool.put("name", gateNodeName);
        approvalGateTool.put("type", "%s/v1/%s".formatted(AI_AGENT_UTILS_COMPONENT, APPROVAL_GATE_ELEMENT_NAME));
        approvalGateTool.put("parameters", gateParameters);
        approvalGateTool.put("clusterElements", gateClusterElements);

        return approvalGateTool;
    }

    /**
     * Builds one {@code clusterElements.approvalChannels} entry per row, in position order — shared by the approval
     * gate ({@link #buildApprovalGateElement}) and the {@code APPROVAL_TOOL} element
     * ({@link #buildApprovalToolElement}) so the two never duplicate this logic. Each call produces a FRESH set of node
     * names (via the shared {@code nodeCounters}), even when both callers are given the very same
     * {@code approvalChannelElements} list in the same {@code generate} call — the gate and the tool are different
     * parents in the {@code clusterElements} tree, so each needs its own child node instances.
     */
    private static List<Map<String, Object>> buildApprovalChannelEntries(
        List<ApprovalDeliveryChannel> deliveryChannels, Map<String, Integer> nodeCounters) {

        List<Map<String, Object>> channelEntries = new ArrayList<>();

        for (ApprovalDeliveryChannel deliveryChannel : deliveryChannels) {
            channelEntries.add(buildApprovalChannelElement(deliveryChannel, nodeCounters));
        }

        return channelEntries;
    }

    private static Map<String, Object> buildApprovalChannelElement(
        ApprovalDeliveryChannel deliveryChannel, Map<String, Integer> nodeCounters) {

        String componentName = deliveryChannel.componentName();
        ApprovalChannelDefinition definition = ApprovalChannelDefinitions.getApprovalChannelDefinition(componentName);
        String type = "%s/v%d/%s".formatted(componentName, 1, deliveryChannel.elementName());

        Map<String, ?> channelParameters = Map.of();

        String nodeName = nextNodeName(nodeCounters, componentName);

        Map<String, Object> channelElement = new LinkedHashMap<>();

        channelElement.put("name", nodeName);
        channelElement.put("type", type);
        channelElement.put("parameters", channelParameters);

        if (definition.connectionRequired()) {
            channelElement.put(WorkflowExtConstants.CONNECTIONS, buildClusterElementConnections(type, nodeName));
        }

        return channelElement;
    }

    private static Map<String, Object> buildSkillsElement(
        List<AiAgentElement> skillElements, Map<String, Integer> nodeCounters) {

        // A FLAT list of ids, not [{skillId: n}] objects: AiAgentUtilsSkillsTool declares
        // array("skills").items(integer("skillId")), so the item IS the id, and both readers —
        // AiAgentUtilsSkillsTool.perform and SkillComponentConnectionFactory.create — call
        // getList(SKILLS, Long.class). Emitting objects here made every save of an agent with a skill fail
        // with "Cannot deserialize value of type java.lang.Long from Object value".
        List<Long> skills = new ArrayList<>();

        for (AiAgentElement element : skillElements) {
            skills.add(Objects.requireNonNull(element.getReferenceId(), "SKILL element referenceId"));
        }

        String type = "%s/v1/%s".formatted(AI_AGENT_UTILS_COMPONENT, SKILLS_TOOL_ELEMENT_NAME);

        Map<String, Object> skillsElement = new LinkedHashMap<>();

        skillsElement.put("name", nextNodeName(nodeCounters, AI_AGENT_UTILS_COMPONENT));
        skillsElement.put("type", type);
        skillsElement.put("parameters", Map.of(SKILLS_PARAM, skills));

        return skillsElement;
    }

    private static Map<String, Object> buildSubAgentElement(
        AiAgentElement element, Map<String, Integer> nodeCounters, Function<Long, SubAgentRef> subAgentResolver) {

        Long referenceId = Objects.requireNonNull(element.getReferenceId(), "SUB_AGENT element referenceId");
        SubAgentRef subAgentRef = subAgentResolver.apply(referenceId);

        Map<String, Object> parameters = new LinkedHashMap<>();

        parameters.put(TOOL_NAME_PARAM, slugify(subAgentRef.name()));
        parameters.put(TOOL_DESCRIPTION_PARAM, subAgentRef.description());
        parameters.put(
            AGENT_UUID_PARAM, subAgentRef.agentUuid()
                .toString());
        parameters.put(MESSAGE_PARAM, MESSAGE_FROM_AI_EXPRESSION);

        // Pins the sub-agent invocation to the CALLER's own conversation (see CALL_AI_AGENT_CONVERSATION_ID_PARAM's
        // javadoc) — ${branch_in.conversationId} is this (parent) workflow's own envelope field, evaluated at the
        // parent's aiAgent_1 node, so it always resolves to the id of whichever channel is running right now.
        parameters.put(
            CALL_AI_AGENT_CONVERSATION_ID_PARAM, "${" + BRANCH_IN_NAME + "." + FIELD_CONVERSATION_ID + "}");

        String type = "%s/v1/%s".formatted(WORKFLOW_COMPONENT, CALL_AI_AGENT_ELEMENT_NAME);

        Map<String, Object> subAgentElement = new LinkedHashMap<>();

        subAgentElement.put("name", nextNodeName(nodeCounters, WORKFLOW_COMPONENT));
        subAgentElement.put("type", type);
        subAgentElement.put("parameters", parameters);

        return subAgentElement;
    }

    /**
     * Sanitizes a sub-agent's display name into a tool name safe for the LLM's function-calling schema — same
     * convention as {@code AiAgentUtilsSkillsTool.sanitizeToolName}: characters outside {@code [A-Za-z0-9_]} collapse
     * to a single underscore, and leading/trailing underscores are stripped.
     */
    private static String slugify(String name) {
        return name.replaceAll("[^A-Za-z0-9_]", "_")
            .replaceAll("_+", "_")
            .replaceAll("^_|_$", "");
    }

    private static Map<String, Object> buildRagElement(AiAgentElement element, Map<String, Integer> nodeCounters) {
        Map<String, ?> parameters = element.getParameters();
        Map<String, Object> ragParameters = new LinkedHashMap<>();

        Integer topK = MapUtils.getInteger(parameters, RAG_PARAM_TOP_K);

        if (topK != null) {
            ragParameters.put(RAG_PARAM_TOP_K, topK);
        }

        Double similarityThreshold = MapUtils.getDouble(parameters, RAG_PARAM_SIMILARITY_THRESHOLD);

        if (similarityThreshold != null) {
            ragParameters.put(RAG_PARAM_SIMILARITY_THRESHOLD, similarityThreshold);
        }

        String type = "%s/v1/%s".formatted(QUESTION_ANSWER_RAG_COMPONENT, RAG_ELEMENT_NAME);

        Map<String, Object> ragElement = new LinkedHashMap<>();

        ragElement.put("name", nextNodeName(nodeCounters, QUESTION_ANSWER_RAG_COMPONENT));
        ragElement.put("type", type);
        ragElement.put("parameters", ragParameters);
        ragElement.put(
            "clusterElements", Map.of(CLUSTER_KEY_VECTOR_STORE, buildVectorStoreElement(element, nodeCounters)));

        return ragElement;
    }

    private static Map<String, Object> buildVectorStoreElement(
        AiAgentElement knowledgeBaseElement, Map<String, Integer> nodeCounters) {

        Long knowledgeBaseId = Objects.requireNonNull(
            knowledgeBaseElement.getReferenceId(), "KNOWLEDGE_BASE element referenceId");

        String type = "%s/v1/%s".formatted(KNOWLEDGE_BASE_COMPONENT, CLUSTER_KEY_VECTOR_STORE);

        Map<String, Object> vectorStoreElement = new LinkedHashMap<>();

        vectorStoreElement.put("name", nextNodeName(nodeCounters, KNOWLEDGE_BASE_COMPONENT));
        vectorStoreElement.put("type", type);
        vectorStoreElement.put("parameters", Map.of(KNOWLEDGE_BASE_ID_PARAM, knowledgeBaseId));

        return vectorStoreElement;
    }

    private static Map<String, Object> buildChatMemoryElement(Map<String, Integer> nodeCounters) {
        String type = "%s/v1/%s".formatted(CHAT_MEMORY_COMPONENT, CHAT_MEMORY_COMPONENT);

        Map<String, Object> chatMemoryElement = new LinkedHashMap<>();

        chatMemoryElement.put("name", nextNodeName(nodeCounters, CHAT_MEMORY_COMPONENT));
        chatMemoryElement.put("type", type);
        chatMemoryElement.put("parameters", Map.of(CONVERSATION_ID_PARAM, CHAT_MEMORY_CONVERSATION_ID_EXPRESSION));

        return chatMemoryElement;
    }

    // --- branch_out ----------------------------------------------------------------------------------------------

    private static Map<String, Object> buildBranchOut(List<ChannelNode> channelNodes) {
        Map<String, Object> parameters = new LinkedHashMap<>();

        parameters.put("expression", "${" + BRANCH_IN_NAME + "." + FIELD_CHANNEL + "}");
        parameters.put("cases", buildBranchOutCases(channelNodes));

        Map<String, Object> branchOut = new LinkedHashMap<>();

        branchOut.put("name", BRANCH_OUT_NAME);
        branchOut.put("type", BRANCH_TYPE);
        branchOut.put("parameters", parameters);

        return branchOut;
    }

    /**
     * One case per distinct channelType (channels sharing a type — e.g. two telegram channels — share one reply case:
     * reply routing only needs the channel kind + {@code replyTo}, both already in the envelope).
     */
    private static List<Map<String, Object>> buildBranchOutCases(List<ChannelNode> channelNodes) {
        Map<String, ChannelDefinition> definitionByChannelType = new LinkedHashMap<>();

        for (ChannelNode channelNode : channelNodes) {
            definitionByChannelType.putIfAbsent(
                channelNode.channel()
                    .getChannelType(),
                channelNode.definition());
        }

        List<Map<String, Object>> cases = new ArrayList<>();

        for (Map.Entry<String, ChannelDefinition> entry : definitionByChannelType.entrySet()) {
            cases.add(buildBranchOutCase(entry.getKey(), entry.getValue()));
        }

        return cases;
    }

    private static Map<String, Object> buildBranchOutCase(String channelType, ChannelDefinition definition) {
        Map<String, Object> oneCase = new LinkedHashMap<>();

        oneCase.put("key", channelType);

        ReplyTask replyTask = definition.replyTask();

        // schedule has no ReplyTask (design doc: "empty case — no reply; the run's value is tool side effects").
        oneCase.put(
            "tasks", replyTask == null ? List.of() : List.of(buildReplyTask(channelType, definition, replyTask)));

        return oneCase;
    }

    private static Map<String, Object> buildReplyTask(
        String channelType, ChannelDefinition definition,
        ReplyTask replyTask) {

        Map<String, Object> parameters = new LinkedHashMap<>();

        if (AiAgentChannelType.WORKFLOW_CALL.equals(channelType)) {
            parameters.put(OUTPUT_SCHEMA_PARAMETER, WORKFLOW_CALL_OUTPUT_SCHEMA);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> substitutedParameters = (Map<String, Object>) substituteReplyValue(
            replyTask.parameters());

        parameters.putAll(substitutedParameters);

        Map<String, Object> replyTaskMap = new LinkedHashMap<>();

        replyTaskMap.put("name", "reply_" + channelType);
        replyTaskMap.put("type", replyTask.type());
        replyTaskMap.put("parameters", parameters);

        if (definition.connectionRequired()) {
            replyTaskMap.put(WorkflowExtConstants.CONNECTIONS, buildConnections(replyTask.type()));
        }

        return replyTaskMap;
    }

    /**
     * Recursively substitutes {@code __AGENT_OUTPUT__} and {@code __ENVELOPE__<field>} tokens (see
     * {@link ChannelDefinition}'s class doc) in a {@link ReplyTask#parameters()} tree.
     */
    private static Object substituteReplyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> substituted = new LinkedHashMap<>();

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                substituted.put((String) entry.getKey(), substituteReplyValue(entry.getValue()));
            }

            return substituted;
        }

        if (value instanceof String stringValue) {
            if (AGENT_OUTPUT_TOKEN.equals(stringValue)) {
                return AGENT_OUTPUT_EXPRESSION;
            }

            if (stringValue.startsWith(ENVELOPE_TOKEN_PREFIX)) {
                String field = stringValue.substring(ENVELOPE_TOKEN_PREFIX.length());

                return "${" + BRANCH_IN_NAME + "." + field + "}";
            }

            return stringValue;
        }

        return value;
    }

    // --- connections ---------------------------------------------------------------------------------------------

    /**
     * {@code nodeType} is {@code <componentName>/v<version>/<operation>} (every {@code ChannelDefinition} trigger and
     * reply type follows this convention — see {@code ChannelDefinitions}' source citations).
     */
    /**
     * A {@code connections} block for a TASK or TRIGGER node, keyed by component name — the platform's convention for a
     * node that is a workflow node in its own right. Cluster elements use
     * {@link #buildClusterElementConnections(String, String)} instead; the two keys are not interchangeable.
     */
    private static Map<String, Object> buildConnections(String nodeType) {
        return buildConnections(nodeType, componentNameOf(nodeType));
    }

    /**
     * A {@code connections} block for a CLUSTER ELEMENT, keyed by the element's own node name.
     * <p>
     * The key matters twice over. It is what {@code ComponentConnection.of(extensions, ...)} hands the deployment
     * dialog as the connection key, and it is what every runtime consumer looks the connection up under
     * ({@code componentConnections.get(clusterElement.getWorkflowNodeName())}). Keying by component name instead — as
     * this did before — produced a SECOND entry alongside the one {@code ClusterRootComponentConnectionFactory} derives
     * from the element's node name: same component, same node, different key, so its {@code HashSet} could not collapse
     * them and the dialog asked for the same connection twice. Keying by the node name makes the two entries identical
     * records, and the set dedupes them.
     * <p>
     * The block stays rather than being dropped because it is load-bearing for built-ins whose element component
     * differs from the connection's: the {@code webSearch} tool is an {@code aiAgentUtils} element needing a
     * {@code brave} connection, and {@code aiAgentUtils} has no connection of its own, so the factory derives nothing
     * for it and this block is its only source.
     */
    private static Map<String, Object> buildClusterElementConnections(String nodeType, String elementNodeName) {
        return buildConnections(nodeType, elementNodeName);
    }

    private static Map<String, Object> buildConnections(String nodeType, String connectionKey) {
        String[] segments = nodeType.split("/");
        String componentName = componentNameOf(nodeType);
        int componentVersion = Integer.parseInt(segments[1].substring(1));

        Map<String, Object> connection = new LinkedHashMap<>();

        connection.put(WorkflowExtConstants.COMPONENT_NAME, componentName);
        connection.put(WorkflowExtConstants.COMPONENT_VERSION, componentVersion);

        return Map.of(connectionKey, connection);
    }

    private record ChannelNode(AiAgentChannel channel, String nodeName, ChannelDefinition definition) {
    }
}
