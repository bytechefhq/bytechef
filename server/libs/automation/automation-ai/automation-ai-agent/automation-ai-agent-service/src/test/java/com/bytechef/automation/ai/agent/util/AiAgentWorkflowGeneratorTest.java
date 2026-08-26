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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import com.bytechef.automation.ai.agent.channel.AiAgentChannelType;
import com.bytechef.automation.ai.agent.channel.ResolvedAgentChannel;
import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.domain.AiAgentChannel;
import com.bytechef.automation.ai.agent.domain.AiAgentElement;
import com.bytechef.automation.ai.agent.test.TestAgentChannels;
import com.bytechef.automation.ai.agent.util.AiAgentWorkflowGenerator.SubAgentRef;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import tools.jackson.core.type.TypeReference;

/**
 * Unit test for {@link AiAgentWorkflowGenerator}. Pins the trigger-naming/ordering rules, the {@code branch_in} /
 * {@code branch_out} dispatcher shapes, the schedule channel's stored-prompt and deterministic chat-memory-key
 * substitution, and byte-identical determinism across repeated generation.
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = JacksonConfiguration.class)
@ExtendWith(ObjectMapperSetupExtension.class)
class AiAgentWorkflowGeneratorTest {

    /**
     * The channels this generator is rendered against — see {@link TestAgentChannels}. Every per-channel value the
     * generator emits arrives through here, which is the point: no component is named in production code any more.
     */
    private static final Function<String, ResolvedAgentChannel> CHANNEL_RESOLVER = TestAgentChannels.resolver();

    private static final UUID AGENT_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String STORED_SCHEDULE_PROMPT = "Summarize today's news";

    private static final Function<Long, SubAgentRef> NO_SUB_AGENTS = referenceId -> {
        throw new AssertionError("Task 8 fixtures carry no SUB_AGENT elements");
    };

    private static final long SUB_AGENT_REFERENCE_ID = 99L;
    private static final UUID SUB_AGENT_AGENT_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    // Trailing "!" and the space exercise slugify's non-[A-Za-z0-9_] collapsing (see
    // AiAgentWorkflowGenerator#slugify's AiAgentUtilsSkillsTool#sanitizeToolName citation) — expected toolName is
    // "Support_Agent".
    private static final String SUB_AGENT_NAME = "Support Agent!";
    private static final String SUB_AGENT_DESCRIPTION = "Handles support questions.";
    private static final String SUB_AGENT_SLUGIFIED_TOOL_NAME = "Support_Agent";

    /**
     * The {@code tools[]} entries an agent with NO {@code AiAgentElement} rows and default settings emits — every
     * built-in defaults ON except {@code webSearch} (see {@code AiAgentSettings}), and {@code skillManagement}
     * contributes five entries in {@code AiAgentWorkflowGenerator.SKILL_MANAGEMENT_ACTION_NAMES}' order. Used to keep
     * every fixture assertion below in one place rather than re-deriving this list per test.
     */
    private static final List<String> DEFAULT_BUILT_IN_TOOL_TYPES = List.of(
        "aiAgentUtils/v1/askUserQuestionTool", "aiAgentUtils/v1/autoMemoryTool", "aiAgentUtils/v1/createAiSkill",
        "aiAgentUtils/v1/updateAiSkill", "aiAgentUtils/v1/deleteAiSkill", "aiAgentUtils/v1/appendFilesToAiSkill",
        "aiAgentUtils/v1/removeFileFromAiSkill");

    private static final Function<Long, SubAgentRef> FULL_ELEMENTS_SUB_AGENT_RESOLVER = referenceId -> {
        if (referenceId == SUB_AGENT_REFERENCE_ID) {
            return new SubAgentRef(SUB_AGENT_NAME, SUB_AGENT_DESCRIPTION, SUB_AGENT_AGENT_UUID);
        }

        throw new AssertionError("Unexpected referenceId " + referenceId);
    };

    @Test
    void testGenerateMatchesSnapshot() {
        AiAgent agent = newAgent();
        List<AiAgentChannel> channels = twoChannelFixtureChannels();

        String definition =
            AiAgentWorkflowGenerator.generate(agent, channels, List.of(), NO_SUB_AGENTS, null, CHANNEL_RESOLVER);

        Map<String, Object> parsed = JsonUtils.read(definition, new TypeReference<>() {});

        JsonFileAssert.assertEquals("definition/agent_workflow_two_channels.json", parsed);
    }

    @Test
    void testGenerateIsDeterministic() {
        AiAgent agent = newAgent();
        List<AiAgentChannel> channels = twoChannelFixtureChannels();

        String first =
            AiAgentWorkflowGenerator.generate(agent, channels, List.of(), NO_SUB_AGENTS, null, CHANNEL_RESOLVER);
        String second =
            AiAgentWorkflowGenerator.generate(agent, channels, List.of(), NO_SUB_AGENTS, null, CHANNEL_RESOLVER);

        assertThat(second).isEqualTo(first);
    }

    /**
     * Pins the AI Hub visibility identity stamp (ticket 732, {@code 2026-08-17-agent-run-hub-visibility}):
     * {@code aiAgent_1} carries {@code aiHubWorkspaceId} / {@code aiHubAgentId} / {@code aiHubCreatorUserId} as sibling
     * keys next to {@code clusterElements}, resolved from {@link AiAgent#getWorkspaceId()}, {@link AiAgent#getId()},
     * and the caller-supplied {@code creatorUserId} argument respectively — {@code AbstractAiAgentChatAction} reads
     * these back as opaque values to report a completed turn.
     */
    @Test
    void testAiAgentNodeCarriesAiHubIdentityStamp() {
        AiAgent agent = newAgent();

        agent.setWorkspaceId(55L);

        List<AiAgentChannel> channels = twoChannelFixtureChannels();

        String definition =
            AiAgentWorkflowGenerator.generate(agent, channels, List.of(), NO_SUB_AGENTS, 77L, CHANNEL_RESOLVER);

        Map<String, Object> parsed = JsonUtils.read(definition, new TypeReference<>() {});
        Map<String, Object> aiAgentNode = tasks(parsed).get(1);

        assertThat(aiAgentNode.get("aiHubWorkspaceId")).isEqualTo(55);
        assertThat(aiAgentNode.get("aiHubAgentId")).isEqualTo(1);
        assertThat(aiAgentNode.get("aiHubCreatorUserId")).isEqualTo(77);
    }

    /**
     * The counterpart to {@link #testAiAgentNodeCarriesAiHubIdentityStamp}: an agent with no workspace (the embedded
     * platform type has no workspace concept) and no resolvable creator carries a partial stamp — {@code aiHubAgentId}
     * only, since {@link AiAgent#getId()} is always known by generation time — rather than a stamp with null-valued
     * keys.
     */
    @Test
    void testAiAgentNodeOmitsUnresolvableIdentityStampFields() {
        AiAgent agent = newAgent();
        List<AiAgentChannel> channels = twoChannelFixtureChannels();

        String definition =
            AiAgentWorkflowGenerator.generate(agent, channels, List.of(), NO_SUB_AGENTS, null, CHANNEL_RESOLVER);

        Map<String, Object> parsed = JsonUtils.read(definition, new TypeReference<>() {});
        Map<String, Object> aiAgentNode = tasks(parsed).get(1);

        assertThat(aiAgentNode).containsKey("aiHubAgentId")
            .doesNotContainKey("aiHubWorkspaceId")
            .doesNotContainKey("aiHubCreatorUserId");
    }

    @Test
    void testTriggersAreOrderedChatFirstWorkflowCallSecondThenByPosition() {
        AiAgent agent = newAgent();
        List<AiAgentChannel> channels = twoChannelFixtureChannels();

        Map<String, Object> parsed = generateAndParse(agent, channels);

        List<Map<String, Object>> triggers = triggers(parsed);

        assertThat(triggers).extracting(trigger -> trigger.get("name"))
            .containsExactly("chat_1", "workflowCall_1", "telegram_1", "schedule_1");
    }

    @Test
    void testBranchInIsFirstTaskWithTriggerNameExpression() {
        AiAgent agent = newAgent();
        List<AiAgentChannel> channels = twoChannelFixtureChannels();

        Map<String, Object> parsed = generateAndParse(agent, channels);

        List<Map<String, Object>> tasks = tasks(parsed);

        assertThat(tasks.get(0)
            .get("name")).isEqualTo("branch_in");
        assertThat(tasks.get(0)
            .get("type")).isEqualTo("branch/v1");

        Map<String, Object> branchInParameters = parameters(tasks.get(0));

        assertThat(branchInParameters.get("expression")).isEqualTo("${__triggerName}");

        List<Map<String, Object>> cases = cases(branchInParameters);

        assertThat(cases).extracting(oneCase -> oneCase.get("key"))
            .containsExactly("chat_1", "workflowCall_1", "telegram_1", "schedule_1");
    }

    @Test
    void testAiAgentNodeIsSecondTaskWithOnlyDefaultBuiltInToolsClusterElements() {
        AiAgent agent = newAgent();
        List<AiAgentChannel> channels = twoChannelFixtureChannels();

        Map<String, Object> parsed = generateAndParse(agent, channels);

        Map<String, Object> aiAgentNode = tasks(parsed).get(1);

        assertThat(aiAgentNode.get("name")).isEqualTo("aiAgent_1");
        assertThat(aiAgentNode.get("type")).isEqualTo("aiAgent/v1/streamChat");

        // No MODEL/TOOL/SKILL/SUB_AGENT/KNOWLEDGE_BASE/CHAT_MEMORY rows and no APPROVAL_TOOL row, but default
        // settings still populate tools[] with the built-ins — see DEFAULT_BUILT_IN_TOOL_TYPES.
        @SuppressWarnings("unchecked")
        Map<String, Object> clusterElements = (Map<String, Object>) aiAgentNode.get("clusterElements");

        assertThat(clusterElements).containsOnlyKeys("tools");
        assertThat(tools(clusterElements)).extracting(tool -> tool.get("type"))
            .containsExactlyElementsOf(DEFAULT_BUILT_IN_TOOL_TYPES);

        Map<String, Object> aiAgentParameters = parameters(aiAgentNode);

        assertThat(aiAgentParameters.get("userPrompt")).isEqualTo("${branch_in.text}");
        assertThat(aiAgentParameters.get("attachments")).isEqualTo("${branch_in.attachments}");
    }

    @Test
    void testAiAgentNodeSystemPromptPresentWhenInstructionsNonBlank() {
        AiAgent agent = newAgent();

        agent.setInstructions("Be concise and polite.");

        List<AiAgentChannel> channels = twoChannelFixtureChannels();

        Map<String, Object> parsed = generateAndParse(agent, channels);
        Map<String, Object> aiAgentNode = tasks(parsed).get(1);

        assertThat(parameters(aiAgentNode).get("systemPrompt")).isEqualTo("Be concise and polite.");
    }

    @Test
    void testAiAgentNodeSystemPromptAbsentWhenInstructionsNull() {
        AiAgent agent = newAgent();
        List<AiAgentChannel> channels = twoChannelFixtureChannels();

        Map<String, Object> parsed = generateAndParse(agent, channels);
        Map<String, Object> aiAgentNode = tasks(parsed).get(1);

        assertThat(parameters(aiAgentNode)).doesNotContainKey("systemPrompt");
    }

    @Test
    void testAiAgentNodeSystemPromptAbsentWhenInstructionsBlank() {
        AiAgent agent = newAgent();

        agent.setInstructions("   ");

        List<AiAgentChannel> channels = twoChannelFixtureChannels();

        Map<String, Object> parsed = generateAndParse(agent, channels);
        Map<String, Object> aiAgentNode = tasks(parsed).get(1);

        assertThat(parameters(aiAgentNode)).doesNotContainKey("systemPrompt");
    }

    /**
     * {@code branch_out} keys on the trigger's node name, one case per channel ROW — not one per channel TYPE, which is
     * what it emitted before this generator became registry-driven. A reply action may take a value configured on the
     * row (twilio's number, see {@link #testReplyTaskWiresRowParameterProperty}), and two rows of one type sharing a
     * single case could only carry one of the two values.
     */
    @Test
    void testBranchOutCaseKeysAreTriggerNodeNames() {
        AiAgent agent = newAgent();
        List<AiAgentChannel> channels = twoChannelFixtureChannels();

        Map<String, Object> parsed = generateAndParse(agent, channels);

        Map<String, Object> branchOut = tasks(parsed).get(2);

        assertThat(branchOut.get("name")).isEqualTo("branch_out");

        Map<String, Object> branchOutParameters = parameters(branchOut);

        assertThat(branchOutParameters.get("expression")).isEqualTo("${__triggerName}");

        List<Map<String, Object>> cases = cases(branchOutParameters);

        assertThat(cases).extracting(oneCase -> oneCase.get("key"))
            .containsExactly("chat_1", "workflowCall_1", "telegram_1", "schedule_1");
    }

    @Test
    void testBranchOutGivesEachRowOfOneChannelTypeItsOwnCase() {
        AiAgent agent = newAgent();

        AiAgentChannel firstTwilioChannel = newChannel(TestAgentChannels.TWILIO, 10L, 0, Map.of("number", "+15550001"));
        AiAgentChannel secondTwilioChannel =
            newChannel(TestAgentChannels.TWILIO, 11L, 1, Map.of("number", "+15550002"));

        Map<String, Object> parsed = generateAndParse(agent, List.of(firstTwilioChannel, secondTwilioChannel));

        assertThat(cases(parameters(tasks(parsed).get(2)))).extracting(oneCase -> oneCase.get("key"))
            .containsExactly("twilio_1", "twilio_2");

        assertThat(parameters(replyTasks(caseByKey(parsed, "twilio_1")).get(0)).get("From")).isEqualTo("+15550001");
        assertThat(parameters(replyTasks(caseByKey(parsed, "twilio_2")).get(0)).get("From")).isEqualTo("+15550002");
    }

    /**
     * The end-to-end shape a Slack channel produces, pinned here rather than left to a manual smoke run: adding one
     * Slack channel to an agent must yield a {@code slack_1} trigger node, a {@code branch_out} case keyed on that node
     * name, and a {@code reply_slack_1} task on the component's own reply action addressed to the conversation the
     * request arrived on. Every part of that comes from the component's declared descriptor now, so a component that
     * stops declaring one would break this rather than silently generating an agent that never answers.
     */
    @Test
    void testSlackChannelGeneratesItsTriggerNodeBranchOutCaseAndReplyTask() {
        AiAgent agent = newAgent();

        AiAgentChannel slackChannel = newChannel(TestAgentChannels.SLACK, 20L, 0, Map.of());

        Map<String, Object> parsed = generateAndParse(agent, List.of(slackChannel));

        Map<String, Object> triggerNode = triggers(parsed).get(0);

        assertThat(triggerNode.get("name")).isEqualTo("slack_1");
        assertThat(triggerNode.get("type")).isEqualTo("slack/v1/newMessage");

        assertThat(cases(parameters(tasks(parsed).get(2)))).extracting(oneCase -> oneCase.get("key"))
            .containsExactly("slack_1");

        Map<String, Object> replyTask = replyTasks(caseByKey(parsed, "slack_1")).get(0);

        assertThat(replyTask.get("name")).isEqualTo("reply_slack_1");
        assertThat(replyTask.get("type")).isEqualTo("slack/v1/sendChannelMessage");
        assertThat(parameters(replyTask)).containsEntry("channel", "${branch_in.conversationId}");
    }

    @Test
    void testChatReplyTaskReferencesAiAgentNodeOutputDirectly() {
        AiAgent agent = newAgent();
        List<AiAgentChannel> channels = twoChannelFixtureChannels();

        Map<String, Object> parsed = generateAndParse(agent, channels);

        Map<String, Object> chatCase = caseByKey(parsed, "chat_1");
        Map<String, Object> replyTask = replyTasks(chatCase).get(0);

        assertThat(replyTask.get("name")).isEqualTo("reply_chat_1");
        assertThat(replyTask.get("type")).isEqualTo("chat/v1/responseToRequest");
        // aiAgent_1's output is an unnamed string (streamChat has no "response" property), so the reply
        // references the bare node, not a ".text" field path.
        assertThat(parameters(replyTask).get("message")).isEqualTo("${aiAgent_1}");
        assertThat(replyTask).doesNotContainKey("connections");
    }

    @Test
    void testWorkflowCallTriggerCarriesPredefinedInputSchema() {
        AiAgent agent = newAgent();
        List<AiAgentChannel> channels = twoChannelFixtureChannels();

        Map<String, Object> parsed = generateAndParse(agent, channels);

        Map<String, Object> workflowCallTrigger = triggers(parsed).get(1);

        assertThat(workflowCallTrigger.get("name")).isEqualTo("workflowCall_1");

        // The schema is not the generator's to know: it arrives verbatim from the channel's declared
        // triggerParameters, which is why the fixture supplies it and this asserts equality rather than substrings.
        assertThat(parameters(workflowCallTrigger).get("inputSchema"))
            .isEqualTo(TestAgentChannels.WORKFLOW_CALL_INPUT_SCHEMA);
    }

    @Test
    void testWorkflowCallReplyTaskCarriesPredefinedOutputSchema() {
        AiAgent agent = newAgent();
        List<AiAgentChannel> channels = twoChannelFixtureChannels();

        Map<String, Object> parsed = generateAndParse(agent, channels);

        Map<String, Object> workflowCallCase = caseByKey(parsed, "workflowCall_1");
        Map<String, Object> replyTask = replyTasks(workflowCallCase).get(0);

        Map<String, Object> replyParameters = parameters(replyTask);

        assertThat(replyParameters.get("outputSchema")).isEqualTo(TestAgentChannels.WORKFLOW_CALL_OUTPUT_SCHEMA);
    }

    /**
     * A reply descriptor may target a nested parameter: {@code workflow}'s reply receives the agent's text at
     * {@code response.message}, where {@code response} is a {@code dynamicProperties} map. The generator must build a
     * nested map, not a key that literally contains a dot.
     */
    @Test
    void testReplyTaskWritesNestedParameterForDottedProperty() {
        AiAgent agent = newAgent();
        List<AiAgentChannel> channels = twoChannelFixtureChannels();

        Map<String, Object> parsed = generateAndParse(agent, channels);

        Map<String, Object> replyParameters = parameters(replyTasks(caseByKey(parsed, "workflowCall_1")).get(0));

        assertThat(replyParameters).doesNotContainKey("response.message");

        @SuppressWarnings("unchecked")
        Map<String, Object> response = (Map<String, Object>) replyParameters.get("response");

        assertThat(response).containsExactly(entry("message", "${aiAgent_1}"));
    }

    @Test
    void testTelegramTriggerAndReplyTaskCarryConnections() {
        AiAgent agent = newAgent();
        List<AiAgentChannel> channels = twoChannelFixtureChannels();

        Map<String, Object> parsed = generateAndParse(agent, channels);

        Map<String, Object> telegramTrigger = triggers(parsed).get(2);

        assertConnectionsBlock(telegramTrigger, "telegram");

        Map<String, Object> telegramCase = caseByKey(parsed, "telegram_1");
        Map<String, Object> replyTask = replyTasks(telegramCase).get(0);

        assertThat(replyTask.get("type")).isEqualTo("telegram/v1/sendMessage");
        assertThat(parameters(replyTask).get("chat_id")).isEqualTo("${branch_in.conversationId}");
        assertThat(parameters(replyTask).get("text")).isEqualTo("${aiAgent_1}");

        assertConnectionsBlock(replyTask, "telegram");
    }

    /**
     * The envelope reads the trigger node through the channel's own request binding, so a legacy nested payload
     * (telegram's {@code message.chat.id}) and a conforming flat one (chat's {@code conversationId}) take the same code
     * path — nothing here knows either component.
     */
    @Test
    void testEnvelopeUsesRequestBindingPaths() {
        AiAgent agent = newAgent();
        List<AiAgentChannel> channels = twoChannelFixtureChannels();

        Map<String, Object> parsed = generateAndParse(agent, channels);

        assertThat(envelopeOf(parsed, "telegram_1"))
            .containsEntry("text", "${telegram_1.message.text}")
            .containsEntry("conversationId", "${telegram_1.message.chat.id}")
            .containsEntry("channel", TestAgentChannels.TELEGRAM);

        assertThat(envelopeOf(parsed, "chat_1"))
            .containsEntry("text", "${chat_1.message}")
            .containsEntry("conversationId", "${chat_1.conversationId}")
            .containsEntry("channel", AiAgentChannelType.CHAT);
    }

    /**
     * Both arms of the attachments binding: a channel whose request descriptor binds a path reads it off the trigger
     * node, and one that binds none (its trigger carries no files) gets a real empty array rather than an expression
     * resolving to nothing.
     */
    @Test
    void testEnvelopeAttachmentsFollowTheBoundPathOrEmptyArray() {
        AiAgent agent = newAgent();
        List<AiAgentChannel> channels = twoChannelFixtureChannels();

        Map<String, Object> parsed = generateAndParse(agent, channels);

        assertThat(envelopeOf(parsed, "chat_1")).containsEntry("attachments", "${chat_1.attachments}");
        assertThat(envelopeOf(parsed, "workflowCall_1"))
            .containsEntry("attachments", "${workflowCall_1.attachments}");
        assertThat(envelopeOf(parsed, "telegram_1")).containsEntry("attachments", List.of());
    }

    /**
     * The envelope carries exactly the four fields the contract defines. {@code replyTo}/{@code replyFrom} are gone:
     * {@code conversationId} doubles as the reply address, and a reply sender that varies per row now comes from the
     * row (see {@link #testReplyTaskWiresRowParameterProperty}) rather than from the inbound payload.
     */
    @Test
    void testEnvelopeCarriesOnlyContractFields() {
        AiAgent agent = newAgent();
        List<AiAgentChannel> channels = twoChannelFixtureChannels();

        Map<String, Object> parsed = generateAndParse(agent, channels);

        for (String nodeName : List.of("chat_1", "workflowCall_1", "telegram_1", "schedule_1")) {
            assertThat(envelopeOf(parsed, nodeName)).containsOnlyKeys(
                "text", "conversationId", "attachments", "channel");
        }
    }

    /**
     * twilio's reply is sent AS the WhatsApp number configured on the channel row — the descriptor maps the row's
     * {@code number} parameter onto the action's {@code From} property — and pins {@code useTemplate} false, without
     * which a free-text agent reply is not expressible at all.
     */
    @Test
    void testReplyTaskWiresRowParameterProperty() {
        AiAgent agent = newAgent();
        AiAgentChannel twilioChannel = newChannel(
            TestAgentChannels.TWILIO, 9L, 0, Map.of("number", "+15550001111"));

        Map<String, Object> parsed = generateAndParse(agent, List.of(twilioChannel));

        Map<String, Object> replyParameters = parameters(replyTasks(caseByKey(parsed, "twilio_1")).get(0));

        assertThat(replyParameters).containsEntry("From", "+15550001111")
            .containsEntry("To", "${branch_in.conversationId}")
            .containsEntry("Body", "${aiAgent_1}");
    }

    @Test
    void testReplyTaskWiresPinnedParameters() {
        AiAgent agent = newAgent();
        AiAgentChannel twilioChannel = newChannel(
            TestAgentChannels.TWILIO, 9L, 0, Map.of("number", "+15550001111"));

        Map<String, Object> parsed = generateAndParse(agent, List.of(twilioChannel));

        assertThat(parameters(replyTasks(caseByKey(parsed, "twilio_1")).get(0))).containsEntry("useTemplate", false);
    }

    /**
     * A row parameter the reply descriptor maps but the row never set contributes nothing, rather than a null-valued
     * parameter the action would then have to defend against.
     */
    @Test
    void testReplyTaskOmitsRowParameterPropertyWhenTheRowDoesNotCarryIt() {
        AiAgent agent = newAgent();
        AiAgentChannel twilioChannel = newChannel(TestAgentChannels.TWILIO, 9L, 0, Map.of());

        Map<String, Object> parsed = generateAndParse(agent, List.of(twilioChannel));

        assertThat(parameters(replyTasks(caseByKey(parsed, "twilio_1")).get(0))).doesNotContainKey("From");
    }

    /**
     * A channel declaration's {@code triggerParameters} are pinned — the SDK calls them "trigger parameters that are
     * always emitted for this channel" — so a row parameter of the same name must not displace one. {@code inputSchema}
     * is a declared property of {@code workflow/v1/newWorkflowCall}, so it passes the row-parameter allow-list; a row
     * carrying it would otherwise replace the contract schema {@code ${workflowCall_1.message}} is derived from.
     * {@code workflowCall} rows are not reachable through the UI, but {@code updateAgentChannel} writes row parameters
     * verbatim.
     */
    @Test
    void testPinnedTriggerParametersWinOverRowParameters() {
        AiAgent agent = newAgent();
        AiAgentChannel workflowCallChannel = newChannel(
            AiAgentChannelType.WORKFLOW_CALL, 9L, 0, Map.of("inputSchema", "{\"type\":\"string\"}"));

        Map<String, Object> parsed = generateAndParse(agent, List.of(workflowCallChannel));

        Map<String, Object> workflowCallTrigger = triggers(parsed).get(0);

        assertThat(parameters(workflowCallTrigger).get("inputSchema"))
            .isEqualTo(TestAgentChannels.WORKFLOW_CALL_INPUT_SCHEMA);
    }

    /**
     * Row parameters reach the trigger node only when the resolved trigger declares a property of that name, which is
     * what lets the schedule row's UI-only keys be excluded generically instead of by a hardcoded list.
     */
    @Test
    void testTriggerParametersExcludeUiOnlyRowKeys() {
        AiAgent agent = newAgent();
        AiAgentChannel scheduleChannel = newChannel(
            AiAgentChannelType.SCHEDULE, 9L, 0,
            Map.of(
                "prompt", STORED_SCHEDULE_PROMPT, "name", "Daily digest", "expression", "0 9 * * *", "timezone",
                "UTC"));

        Map<String, Object> parsed = generateAndParse(agent, List.of(scheduleChannel));

        Map<String, Object> scheduleTrigger = triggers(parsed).get(0);

        assertThat(scheduleTrigger.get("type")).isEqualTo("schedule/v1/cron");
        assertThat(parameters(scheduleTrigger)).containsOnly(
            entry("expression", "0 9 * * *"), entry("timezone", "UTC"));
    }

    @Test
    void testScheduleCaseUsesStoredPromptAndHasNoReplyTask() {
        AiAgent agent = newAgent();
        List<AiAgentChannel> channels = twoChannelFixtureChannels();

        Map<String, Object> parsed = generateAndParse(agent, channels);

        Map<String, Object> branchInParameters = parameters(tasks(parsed).get(0));
        Map<String, Object> scheduleCase = caseInCasesByKey(branchInParameters, "schedule_1");

        List<Map<String, Object>> scheduleTasks = replyTasks(scheduleCase);
        Map<String, Object> envelopeTask = scheduleTasks.get(0);

        assertThat(envelopeTask.get("name")).isEqualTo("envelope_schedule_1");

        @SuppressWarnings("unchecked")
        Map<String, Object> envelope = (Map<String, Object>) parameters(envelopeTask).get("value");

        assertThat(envelope.get("text")).isEqualTo(STORED_SCHEDULE_PROMPT);
        assertThat(envelope.get("channel")).isEqualTo(AiAgentChannelType.SCHEDULE);
        assertThat(envelope.get("conversationId")).isNotNull()
            .isNotEqualTo("=uuid()");

        // A schedule receives nothing, so nothing in its envelope may read its own trigger node — the resolver's
        // synthesized binding carries no paths at all, and this is what proves the generator never consults them.
        assertThat(envelope.values()).noneSatisfy(
            value -> assertThat(String.valueOf(value)).contains("${schedule_1."));

        Map<String, Object> scheduleOutCase = caseByKey(parsed, "schedule_1");

        assertThat(replyTasks(scheduleOutCase)).isEmpty();
    }

    @Test
    void testScheduleConversationIdIsDeterministic() {
        AiAgent agent = newAgent();
        List<AiAgentChannel> channels = twoChannelFixtureChannels();

        Map<String, Object> firstParsed = generateAndParse(agent, channels);
        Map<String, Object> secondParsed = generateAndParse(agent, channels);

        Object firstConversationId = scheduleConversationId(firstParsed);
        Object secondConversationId = scheduleConversationId(secondParsed);

        assertThat(firstConversationId).isEqualTo(secondConversationId);
    }

    @Test
    void testGenerateFullElementsMatchesSnapshot() {
        AiAgent agent = newAgent();
        List<AiAgentChannel> channels = twoChannelFixtureChannels();
        List<AiAgentElement> elements = fullElementsFixtureElements();

        String definition =
            AiAgentWorkflowGenerator.generate(agent, channels, elements, FULL_ELEMENTS_SUB_AGENT_RESOLVER, null,
                CHANNEL_RESOLVER);

        Map<String, Object> parsed = JsonUtils.read(definition, new TypeReference<>() {});

        JsonFileAssert.assertEquals("definition/agent_workflow_full_elements.json", parsed);
    }

    @Test
    void testFullElementsGenerationIsDeterministic() {
        AiAgent agent = newAgent();
        List<AiAgentChannel> channels = twoChannelFixtureChannels();
        List<AiAgentElement> elements = fullElementsFixtureElements();

        String first =
            AiAgentWorkflowGenerator.generate(agent, channels, elements, FULL_ELEMENTS_SUB_AGENT_RESOLVER, null,
                CHANNEL_RESOLVER);
        String second =
            AiAgentWorkflowGenerator.generate(agent, channels, elements, FULL_ELEMENTS_SUB_AGENT_RESOLVER, null,
                CHANNEL_RESOLVER);

        assertThat(second).isEqualTo(first);
    }

    @Test
    void testClusterElementsModelEntryMapsProviderAndModel() {
        Map<String, Object> clusterElements = generateFullElementsClusterElements();

        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) clusterElements.get("model");

        assertThat(model.get("name")).isEqualTo("openAi_1");
        assertThat(model.get("type")).isEqualTo("openAi/v1/model");
        assertThat(parameters(model)).isEqualTo(Map.of("model", "gpt-5-nano"));
        assertClusterElementConnectionsBlock(model, "openAi");
    }

    /**
     * Every save regenerates the whole definition, so an advanced model property the node details panel wrote survives
     * only if buildModelElement re-emits it — the same nested-parameters round-trip a TOOL row already gets.
     */
    @Test
    void testModelElementRoundTripsNestedParameters() {
        AiAgentElement model = new AiAgentElement(1L, AiAgentElement.KIND_MODEL);

        model.setId(1L);
        model.setPosition(0);
        model.setParameters(
            Map.of(
                "provider", "openAi", "model", "gpt-5-nano", "parameters",
                Map.of("temperature", 0.2, "maxTokens", 4096)));

        @SuppressWarnings("unchecked")
        Map<String, Object> modelElement =
            (Map<String, Object>) generateClusterElements(List.of(model)).get("model");

        assertThat(parameters(modelElement))
            .isEqualTo(Map.of("temperature", 0.2, "maxTokens", 4096, "model", "gpt-5-nano"));
    }

    /**
     * The element's own `model` key is authoritative — the picker writes it and publish validation reads it — so a
     * stale copy left inside the nested map must not shadow it.
     */
    @Test
    void testModelElementNestedParametersCannotShadowTheModelKey() {
        AiAgentElement model = new AiAgentElement(1L, AiAgentElement.KIND_MODEL);

        model.setId(1L);
        model.setPosition(0);
        model.setParameters(
            Map.of("provider", "openAi", "model", "gpt-5-nano", "parameters", Map.of("model", "stale-model")));

        @SuppressWarnings("unchecked")
        Map<String, Object> modelElement =
            (Map<String, Object>) generateClusterElements(List.of(model)).get("model");

        assertThat(parameters(modelElement)).containsEntry("model", "gpt-5-nano");
    }

    @Test
    void testClusterElementsToolsAreOrderedToolThenSkillsThenSubAgentThenDefaultBuiltIns() {
        List<Map<String, Object>> tools = tools(generateFullElementsClusterElements());

        assertThat(tools).extracting(tool -> tool.get("type"))
            .containsExactlyElementsOf(
                Stream.concat(
                    Stream.of("slack/v1/sendMessage", "aiAgentUtils/v1/skillsTool", "workflow/v1/callAiAgent"),
                    DEFAULT_BUILT_IN_TOOL_TYPES.stream())
                    .toList());
        assertThat(tools).extracting(tool -> tool.get("name"))
            .containsExactly(
                "slack_1", "aiAgentUtils_1", "workflow_1", "aiAgentUtils_2", "aiAgentUtils_3", "aiAgentUtils_4",
                "aiAgentUtils_5", "aiAgentUtils_6", "aiAgentUtils_7", "aiAgentUtils_8");
    }

    @Test
    void testClusterElementsToolEntryMapsComponentActionAndParametersWithConnections() {
        Map<String, Object> toolEntry = toolByType(generateFullElementsClusterElements(), "slack/v1/sendMessage");

        assertThat(parameters(toolEntry)).isEqualTo(Map.of("channel", "#general"));
        assertClusterElementConnectionsBlock(toolEntry, "slack");
    }

    @Test
    void testClusterElementsSkillsAggregateHasExactlyOneEntryWithTwoSkillIds() {
        List<Map<String, Object>> tools = tools(generateFullElementsClusterElements());

        List<Map<String, Object>> skillsToolEntries = tools.stream()
            .filter(tool -> "aiAgentUtils/v1/skillsTool".equals(tool.get("type")))
            .toList();

        assertThat(skillsToolEntries).hasSize(1);

        Map<String, Object> skillsToolEntry = skillsToolEntries.get(0);

        assertThat(parameters(skillsToolEntry).get("skills"))
            .isEqualTo(List.of(10, 20));
    }

    @Test
    void testClusterElementsSubAgentEntryCarriesResolverProvidedNameDescriptionAndAgentUuid() {
        Map<String, Object> subAgentEntry =
            toolByType(generateFullElementsClusterElements(), "workflow/v1/callAiAgent");

        Map<String, Object> subAgentParameters = parameters(subAgentEntry);

        assertThat(subAgentParameters.get("toolName")).isEqualTo(SUB_AGENT_SLUGIFIED_TOOL_NAME);
        assertThat(subAgentParameters.get("toolDescription")).isEqualTo(SUB_AGENT_DESCRIPTION);
        assertThat(subAgentParameters.get("agentUuid")).isEqualTo(SUB_AGENT_AGENT_UUID.toString());
        assertThat(subAgentEntry).doesNotContainKey("connections");
    }

    /**
     * The generator wires {@code message} to a {@code fromAi(...)} expression rather than leaving it entirely unset —
     * that expression is what {@code AbstractToolFacade#extractFromAiResults} finds to expose {@code message} as an
     * LLM-fillable argument on the tool's function-calling schema (see {@code WorkflowCallAiAgentTool}'s class
     * javadoc).
     */
    @Test
    void testClusterElementsSubAgentEntryWiresMessageToFromAiExpression() {
        Map<String, Object> subAgentEntry =
            toolByType(generateFullElementsClusterElements(), "workflow/v1/callAiAgent");

        Map<String, Object> subAgentParameters = parameters(subAgentEntry);

        assertThat(subAgentParameters.get("message")).asInstanceOf(InstanceOfAssertFactories.STRING)
            .startsWith("=fromAi(")
            .contains("'message'");
    }

    /**
     * Every sub-agent invocation must be pinned to the CALLING conversation, not left for the LLM to fill in (or omit)
     * on the tool call — otherwise every invocation of a given sub-agent, from every distinct parent conversation,
     * pools into one shared chat-memory thread. See {@code AiAgentWorkflowGenerator}'s
     * {@code CALL_AGENT_CONVERSATION_ID_PARAM} javadoc.
     */
    @Test
    void testClusterElementsSubAgentEntryPinsConversationIdToParentEnvelope() {
        Map<String, Object> subAgentEntry =
            toolByType(generateFullElementsClusterElements(), "workflow/v1/callAiAgent");

        Map<String, Object> subAgentParameters = parameters(subAgentEntry);

        assertThat(subAgentParameters.get("conversationId")).isEqualTo("${branch_in.conversationId}");
    }

    @Test
    void testClusterElementsRagEntryNestsKnowledgeBaseVectorStore() {
        Map<String, Object> clusterElements = generateFullElementsClusterElements();

        // An ARRAY: RAG is a multiple cluster element, so an agent carries one entry per knowledge base.
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ragEntries = (List<Map<String, Object>>) clusterElements.get("rag");

        assertThat(ragEntries).hasSize(1);

        Map<String, Object> rag = ragEntries.getFirst();

        assertThat(rag.get("name")).isEqualTo("questionAnswerRag_1");
        assertThat(rag.get("type")).isEqualTo("questionAnswerRag/v1/rag");
        assertThat(parameters(rag)).isEqualTo(Map.of("topK", 4, "similarityThreshold", 0.5));

        @SuppressWarnings("unchecked")
        Map<String, Object> ragClusterElements = (Map<String, Object>) rag.get("clusterElements");
        @SuppressWarnings("unchecked")
        Map<String, Object> vectorStore = (Map<String, Object>) ragClusterElements.get("vectorStore");

        assertThat(vectorStore.get("name")).isEqualTo("knowledgeBase_1");
        assertThat(vectorStore.get("type")).isEqualTo("knowledgeBase/v1/vectorStore");
        assertThat(parameters(vectorStore)).isEqualTo(Map.of("knowledgeBaseId", 7));
    }

    @Test
    void testClusterElementsChatMemoryEntryUsesBranchInConversationIdExpression() {
        Map<String, Object> clusterElements = generateFullElementsClusterElements();

        @SuppressWarnings("unchecked")
        Map<String, Object> chatMemory = (Map<String, Object>) clusterElements.get("chatMemory");

        assertThat(chatMemory.get("name")).isEqualTo("chatMemory_1");
        assertThat(chatMemory.get("type")).isEqualTo("chatMemory/v1/chatMemory");
        assertThat(parameters(chatMemory).get("conversationId")).isEqualTo("${branch_in.conversationId}");
    }

    @Test
    void testClusterElementsHaveOnlyToolsKeyWhenNoElements() {
        Map<String, Object> parsed = generateAndParse(newAgent(), twoChannelFixtureChannels());

        Map<String, Object> aiAgentNode = tasks(parsed).get(1);

        // No MODEL/KNOWLEDGE_BASE/CHAT_MEMORY rows, so those keys are absent — but default settings still populate
        // "tools" with the built-ins (see testAiAgentNodeIsSecondTaskWithOnlyDefaultBuiltInToolsClusterElements).
        @SuppressWarnings("unchecked")
        Map<String, Object> clusterElements = (Map<String, Object>) aiAgentNode.get("clusterElements");

        assertThat(clusterElements).containsOnlyKeys("tools");
    }

    /**
     * Regression test for a cross-group node-name collision: {@code chatMemory} and {@code knowledgeBase} are real,
     * separately-installable components with ordinary TOOL actions (e.g. {@code chatMemory/v1/addMessages}) in addition
     * to their singleton cluster elements ({@code CHAT_MEMORY} row → the {@code chatMemory} entry,
     * {@code KNOWLEDGE_BASE} row → the {@code rag} entry's nested {@code vectorStore}). Before the fix, the singleton
     * entries were named with a fixed {@code <componentName>_1} suffix computed independently of the tools array's name
     * counter, so a TOOL row on the same component collided with its singleton counterpart (both {@code chatMemory_1}).
     * Node names must be unique across the whole {@code clusterElements} tree regardless of which group produced them.
     */
    @Test
    void testClusterElementsToolAndSingletonOnSameComponentGetDistinctNames() {
        Map<String, Object> clusterElements = generateClusterElements(chatMemoryCollisionFixtureElements());

        Map<String, Object> chatMemoryToolEntry = toolByType(clusterElements, "chatMemory/v1/addMessages");

        @SuppressWarnings("unchecked")
        Map<String, Object> chatMemorySingletonEntry = (Map<String, Object>) clusterElements.get("chatMemory");

        assertThat(chatMemoryToolEntry.get("name")).isNotEqualTo(chatMemorySingletonEntry.get("name"));
        assertThat(chatMemoryToolEntry.get("name")).isEqualTo("chatMemory_1");
        assertThat(chatMemorySingletonEntry.get("name")).isEqualTo("chatMemory_2");
    }

    @Test
    void testClusterElementsToolAndSingletonOnSameComponentGenerationIsDeterministic() {
        AiAgent agent = newAgent();
        List<AiAgentChannel> channels = twoChannelFixtureChannels();
        List<AiAgentElement> elements = chatMemoryCollisionFixtureElements();

        String first =
            AiAgentWorkflowGenerator.generate(agent, channels, elements, NO_SUB_AGENTS, null, CHANNEL_RESOLVER);
        String second =
            AiAgentWorkflowGenerator.generate(agent, channels, elements, NO_SUB_AGENTS, null, CHANNEL_RESOLVER);

        assertThat(second).isEqualTo(first);
    }

    /**
     * A TOOL row on the {@code chatMemory} component (a real action, not the cluster element) plus a CHAT_MEMORY row.
     */
    private static List<AiAgentElement> chatMemoryCollisionFixtureElements() {
        AiAgentElement tool = new AiAgentElement(1L, "TOOL");

        tool.setId(1L);
        tool.setPosition(0);
        tool.setParameters(Map.of("componentName", "chatMemory", "componentVersion", 1, "actionName", "addMessages"));

        AiAgentElement chatMemory = new AiAgentElement(1L, "CHAT_MEMORY");

        chatMemory.setId(2L);
        chatMemory.setPosition(1);

        return List.of(tool, chatMemory);
    }

    // --- HITL approval gate --------------------------------------------------------------------------------------

    @Test
    void testGenerateHitlMatchesSnapshot() {
        AiAgent agent = newAgent();
        List<AiAgentChannel> channels = twoChannelFixtureChannels();
        List<AiAgentElement> elements = hitlFixtureElements();

        String definition =
            AiAgentWorkflowGenerator.generate(agent, channels, elements, NO_SUB_AGENTS, null, CHANNEL_RESOLVER);

        Map<String, Object> parsed = JsonUtils.read(definition, new TypeReference<>() {});

        JsonFileAssert.assertEquals("definition/agent_workflow_hitl.json", parsed);
    }

    @Test
    void testHitlGenerationIsDeterministic() {
        AiAgent agent = newAgent();
        List<AiAgentChannel> channels = twoChannelFixtureChannels();
        List<AiAgentElement> elements = hitlFixtureElements();

        String first =
            AiAgentWorkflowGenerator.generate(agent, channels, elements, NO_SUB_AGENTS, null, CHANNEL_RESOLVER);
        String second =
            AiAgentWorkflowGenerator.generate(agent, channels, elements, NO_SUB_AGENTS, null, CHANNEL_RESOLVER);

        assertThat(second).isEqualTo(first);
    }

    @Test
    void testHitlGatedToolIsNestedInsideGateNotInFlatToolsList() {
        List<Map<String, Object>> tools = tools(generateHitlClusterElements());

        // Default settings (askUserQuestion/autoMemory/skillManagement ON, webSearch OFF, no APPROVAL_TOOL row in
        // this fixture) trail the gate — see DEFAULT_BUILT_IN_TOOL_TYPES.
        assertThat(tools).extracting(tool -> tool.get("type"))
            .containsExactlyElementsOf(
                Stream.concat(
                    Stream.of("jira/v1/createIssue", "aiAgentUtils/v1/approvalGateTool"),
                    DEFAULT_BUILT_IN_TOOL_TYPES.stream())
                    .toList());

        Map<String, Object> gate = toolByType(generateHitlClusterElements(), "aiAgentUtils/v1/approvalGateTool");

        @SuppressWarnings("unchecked")
        Map<String, Object> gateClusterElements = (Map<String, Object>) gate.get("clusterElements");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> gatedTools = (List<Map<String, Object>>) gateClusterElements.get("tools");

        assertThat(gatedTools).extracting(tool -> tool.get("type"))
            .containsExactly("stripe/v1/createRefund");
        assertThat(gatedTools.get(0)
            .get("name")).isEqualTo("stripe_1");
    }

    @Test
    void testHitlApprovalChannelsEntriesAndTypes() {
        Map<String, Object> gate = toolByType(generateHitlClusterElements(), "aiAgentUtils/v1/approvalGateTool");

        @SuppressWarnings("unchecked")
        Map<String, Object> gateClusterElements = (Map<String, Object>) gate.get("clusterElements");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> approvalChannels = (List<Map<String, Object>>) gateClusterElements.get(
            "approvalChannels");

        // Derived from the agent's OWN channels (chat + telegram, workflowCall skipped), not from separately
        // configured approval rows: an approval reaches a human wherever the agent already talks to them.
        assertThat(approvalChannels).extracting(channel -> channel.get("name"))
            .containsExactly("chat_1", "telegram_1");
        assertThat(approvalChannels).extracting(channel -> channel.get("type"))
            .containsExactly("chat/v1/chat", "telegram/v1/telegram");

        // The chat channel needs no connection; the telegram channel does.
        assertThat(approvalChannels.get(0)).doesNotContainKey("connections");
        assertClusterElementConnectionsBlock(approvalChannels.get(1), "telegram");
    }

    @Test
    void testHitlGateExpiryParameter() {
        Map<String, Object> gate = toolByType(generateHitlClusterElements(), "aiAgentUtils/v1/approvalGateTool");

        Map<String, Object> gateParameters = parameters(gate);

        assertThat(gateParameters.get("name")).isEqualTo("Requires approval");
        assertThat(gateParameters.get("approvalExpiresIn")).isEqualTo(4);
        assertThat(gateParameters.get("approvalExpiresInUnit")).isEqualTo("HOURS");
    }

    @Test
    void testHitlConnectionRefsIncludeNestedToolAndSlackChannel() {
        AiAgent agent = newAgent();
        List<AiAgentChannel> channels = twoChannelFixtureChannels();
        List<AiAgentElement> elements = hitlFixtureElements();

        List<AiAgentWorkflowGenerator.ConnectionRef> connectionRefs =
            AiAgentWorkflowGenerator.buildConnectionRefs(agent, channels, elements, CHANNEL_RESOLVER);

        // The telegram CHANNEL is a trigger, so it is keyed (its own node name, its component name). Every element
        // is a cluster element hanging off aiAgent_1, so each is keyed (the cluster root, the element's node name).
        assertThat(connectionRefs).extracting(AiAgentWorkflowGenerator.ConnectionRef::workflowNodeName)
            .containsExactly("telegram_1", "aiAgent_1", "aiAgent_1", "aiAgent_1");

        // The chat approval channel needs no connection, so it contributes no ref — only the gated tool and the
        // slack channel (plus the pre-existing telegram trigger) do.
        assertThat(connectionRefs).extracting(AiAgentWorkflowGenerator.ConnectionRef::workflowConnectionKey)
            .containsExactly("telegram", "jira_1", "stripe_1", "telegram_1");
    }

    // --- APPROVAL_TOOL (LLM-invocable requestApproval tool) -------------------------------------------------------

    @Test
    void testGenerateApprovalToolMatchesSnapshot() {
        AiAgent agent = newAgent();
        List<AiAgentChannel> channels = twoChannelFixtureChannels();
        List<AiAgentElement> elements = approvalToolFixtureElements();

        String definition =
            AiAgentWorkflowGenerator.generate(agent, channels, elements, NO_SUB_AGENTS, null, CHANNEL_RESOLVER);

        Map<String, Object> parsed = JsonUtils.read(definition, new TypeReference<>() {});

        JsonFileAssert.assertEquals("definition/agent_workflow_approval_tool.json", parsed);
    }

    @Test
    void testApprovalToolGenerationIsDeterministic() {
        AiAgent agent = newAgent();
        List<AiAgentChannel> channels = twoChannelFixtureChannels();
        List<AiAgentElement> elements = approvalToolFixtureElements();

        String first =
            AiAgentWorkflowGenerator.generate(agent, channels, elements, NO_SUB_AGENTS, null, CHANNEL_RESOLVER);
        String second =
            AiAgentWorkflowGenerator.generate(agent, channels, elements, NO_SUB_AGENTS, null, CHANNEL_RESOLVER);

        assertThat(second).isEqualTo(first);
    }

    @Test
    void testApprovalToolIsEmittedAsTopLevelToolWithItsOwnApprovalChannels() {
        Map<String, Object> clusterElements = generateClusterElements(approvalToolFixtureElements());

        Map<String, Object> approvalTool = toolByType(clusterElements, "approval/v1/requestApproval");

        assertThat(approvalTool.get("name")).isEqualTo("approval_1");
        assertThat(approvalTool.get("parameters")).isEqualTo(Map.of());
        // Last of every group — see buildTools' javadoc — trailing the default-ON built-ins.
        assertThat(tools(clusterElements)).last()
            .isEqualTo(approvalTool);

        @SuppressWarnings("unchecked")
        Map<String, Object> approvalToolClusterElements = (Map<String, Object>) approvalTool.get("clusterElements");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> approvalChannels =
            (List<Map<String, Object>>) approvalToolClusterElements.get("approvalChannels");

        assertThat(approvalChannels).extracting(channel -> channel.get("name"))
            .containsExactly("chat_1", "telegram_1");
        assertThat(approvalChannels).extracting(channel -> channel.get("type"))
            .containsExactly("chat/v1/chat", "telegram/v1/telegram");
    }

    @Test
    void testApprovalToolConnectionRefsIncludeItsOwnChannelsNotTheGates() {
        AiAgent agent = newAgent();
        List<AiAgentChannel> channels = twoChannelFixtureChannels();
        List<AiAgentElement> elements = approvalToolFixtureElements();

        List<AiAgentWorkflowGenerator.ConnectionRef> connectionRefs =
            AiAgentWorkflowGenerator.buildConnectionRefs(agent, channels, elements, CHANNEL_RESOLVER);

        // telegram trigger, then the approval tool's own slack channel (chat needs no connection). No gate exists in
        // this fixture, so nothing else contributes a ref.
        assertThat(connectionRefs).extracting(AiAgentWorkflowGenerator.ConnectionRef::workflowNodeName)
            .containsExactly("telegram_1", "aiAgent_1");
        assertThat(connectionRefs).extracting(AiAgentWorkflowGenerator.ConnectionRef::workflowConnectionKey)
            .containsExactly("telegram", "telegram_1");
    }

    // --- Gate + APPROVAL_TOOL together -----------------------------------------------------------------------

    /**
     * The gate (wrapping a {@code requiresApproval=true} TOOL) and the {@code APPROVAL_TOOL} element are two
     * independent mechanisms that can coexist on the same agent — this pins that they do so correctly: the gated tool
     * stays nested inside the gate, the {@code requestApproval} entry sits in the FLAT {@code tools[]} array (never
     * nested inside the gate — {@code AiAgentUtilsApprovalGateTool.checkGatableChild} would reject that), and each gets
     * its OWN fresh {@code approvalChannels} node instances from the same underlying {@code APPROVAL_CHANNEL} rows
     * (distinct node names, since both draw from the same shared {@code chat}/{@code slack} counters).
     */
    @Test
    void testGateAndApprovalToolCoexistApprovalToolStaysFlatNeverNestedInGate() {
        Map<String, Object> clusterElements = generateClusterElements(gateAndApprovalToolFixtureElements());

        List<Map<String, Object>> tools = tools(clusterElements);

        // Gate first (at the gated tool's own position — no ungated tools in this fixture), then the default
        // built-ins, then the approval tool last — see buildTools' javadoc.
        assertThat(tools).extracting(tool -> tool.get("type"))
            .containsExactlyElementsOf(
                Stream.concat(
                    Stream.of("aiAgentUtils/v1/approvalGateTool"),
                    Stream.concat(DEFAULT_BUILT_IN_TOOL_TYPES.stream(), Stream.of("approval/v1/requestApproval")))
                    .toList());

        Map<String, Object> gate = toolByType(clusterElements, "aiAgentUtils/v1/approvalGateTool");

        @SuppressWarnings("unchecked")
        Map<String, Object> gateClusterElements = (Map<String, Object>) gate.get("clusterElements");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> gatedTools = (List<Map<String, Object>>) gateClusterElements.get("tools");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> gateApprovalChannels =
            (List<Map<String, Object>>) gateClusterElements.get("approvalChannels");

        assertThat(gatedTools).extracting(tool -> tool.get("type"))
            .containsExactly("stripe/v1/createRefund");
        assertThat(gateApprovalChannels).extracting(channel -> channel.get("name"))
            .containsExactly("chat_1", "telegram_1");
        // The gate never contains the requestApproval tool itself — only ordinary gated TOOL rows can be nested.
        assertThat(gatedTools).extracting(tool -> tool.get("type"))
            .doesNotContain("approval/v1/requestApproval");

        Map<String, Object> approvalTool = toolByType(clusterElements, "approval/v1/requestApproval");

        assertThat(tools).last()
            .isEqualTo(approvalTool);

        @SuppressWarnings("unchecked")
        Map<String, Object> approvalToolClusterElements =
            (Map<String, Object>) approvalTool.get("clusterElements");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> approvalToolChannels =
            (List<Map<String, Object>>) approvalToolClusterElements.get("approvalChannels");

        // Fresh node instances — chat_2/telegram_2, not the gate's chat_1/telegram_1 — even though both lists are
        // derived from the exact same two agent channels.
        assertThat(approvalToolChannels).extracting(channel -> channel.get("name"))
            .containsExactly("chat_2", "telegram_2");
    }

    @Test
    void testGateAndApprovalToolCoexistGenerationIsDeterministic() {
        AiAgent agent = newAgent();
        List<AiAgentChannel> channels = twoChannelFixtureChannels();
        List<AiAgentElement> elements = gateAndApprovalToolFixtureElements();

        String first =
            AiAgentWorkflowGenerator.generate(agent, channels, elements, NO_SUB_AGENTS, null, CHANNEL_RESOLVER);
        String second =
            AiAgentWorkflowGenerator.generate(agent, channels, elements, NO_SUB_AGENTS, null, CHANNEL_RESOLVER);

        assertThat(second).isEqualTo(first);
    }

    /**
     * The {@code APPROVAL_GATE} master switch is what makes a {@code requiresApproval} TOOL actually gate. With the row
     * absent — the default for a new agent — the same tool is emitted as an ordinary flat entry, and its flag is left
     * untouched on the row so switching the gate back on restores the gating.
     */
    @Test
    void testGatedToolRunsUngatedWithoutApprovalGateElement() {
        List<AiAgentElement> elements = new ArrayList<>(gateAndApprovalToolFixtureElements());

        elements.removeIf(element -> AiAgentElement.KIND_APPROVAL_GATE.equals(element.getKind()));

        List<Map<String, Object>> tools = tools(generateClusterElements(elements));

        assertThat(tools).extracting(tool -> tool.get("type"))
            .doesNotContain("aiAgentUtils/v1/approvalGateTool")
            .contains("stripe/v1/createRefund");
    }

    /**
     * One gated {@code TOOL} plus the {@code APPROVAL_GATE} row that enables gating at all, an {@code APPROVAL_TOOL}
     * element, and two {@code APPROVAL_CHANNEL} rows (chat, then slack) shared by both.
     */
    private static List<AiAgentElement> gateAndApprovalToolFixtureElements() {
        AiAgentElement gatedTool = new AiAgentElement(1L, AiAgentElement.KIND_TOOL);

        gatedTool.setId(1L);
        gatedTool.setPosition(0);
        gatedTool.setParameters(
            Map.of(
                "componentName", "stripe", "componentVersion", 1, "actionName", "createRefund", "parameters",
                Map.of("amount", 100), "requiresApproval", true));

        AiAgentElement approvalGate = new AiAgentElement(1L, AiAgentElement.KIND_APPROVAL_GATE);

        approvalGate.setId(5L);
        approvalGate.setPosition(4);

        AiAgentElement approvalTool = new AiAgentElement(1L, AiAgentElement.KIND_APPROVAL_TOOL);

        approvalTool.setId(2L);
        approvalTool.setPosition(1);

        return List.of(gatedTool, approvalGate, approvalTool);
    }

    // --- Maximal / everything-enabled fixture -----------------------------------------------------------------

    /**
     * Every element kind, every built-in setting ON (including {@code webSearch}, with a design-time connection id), a
     * gated TOOL forcing the gate, and an {@code APPROVAL_TOOL} element — the densest fixture this generator supports,
     * and where a shared node-name-counter regression would surface first: the flat {@code slack} TOOL row, the gate's
     * {@code slack} approval channel, and the {@code APPROVAL_TOOL}'s own {@code slack} approval channel all draw from
     * the SAME {@code slack} counter (see {@link #testMaximalFixtureNodeNamesReflectSharedCounters}), as do the
     * multiple {@code chat}/{@code aiAgentUtils} consumers elsewhere in the tree.
     */
    @Test
    void testMaximalFixtureGenerationIsDeterministic() {
        AiAgent agent = maximalFixtureAgent();
        List<AiAgentChannel> channels = twoChannelFixtureChannels();
        List<AiAgentElement> elements = maximalFixtureElements();

        String first =
            AiAgentWorkflowGenerator.generate(agent, channels, elements, FULL_ELEMENTS_SUB_AGENT_RESOLVER, null,
                CHANNEL_RESOLVER);
        String second =
            AiAgentWorkflowGenerator.generate(agent, channels, elements, FULL_ELEMENTS_SUB_AGENT_RESOLVER, null,
                CHANNEL_RESOLVER);

        assertThat(second).isEqualTo(first);
    }

    @Test
    void testGenerateMaximalFixtureMatchesSnapshot() {
        AiAgent agent = maximalFixtureAgent();
        List<AiAgentChannel> channels = twoChannelFixtureChannels();
        List<AiAgentElement> elements = maximalFixtureElements();

        String definition =
            AiAgentWorkflowGenerator.generate(agent, channels, elements, FULL_ELEMENTS_SUB_AGENT_RESOLVER, null,
                CHANNEL_RESOLVER);

        Map<String, Object> parsed = JsonUtils.read(definition, new TypeReference<>() {});

        JsonFileAssert.assertEquals("definition/agent_workflow_maximal.json", parsed);
    }

    /**
     * Pins the exact node names a regression in the shared {@code nodeCounters} map would scramble: {@code slack} is
     * consumed THREE times (the flat TOOL row, the gate's approval channel, the {@code APPROVAL_TOOL}'s approval
     * channel) and {@code aiAgentUtils} TEN times (the gate, the SKILL aggregate, every built-in, {@code webSearch}).
     */
    @Test
    void testMaximalFixtureNodeNamesReflectSharedCounters() {
        Map<String, Object> clusterElements =
            generateClusterElements(
                maximalFixtureAgent(), maximalFixtureElements(), FULL_ELEMENTS_SUB_AGENT_RESOLVER);

        List<Map<String, Object>> tools = tools(clusterElements);

        assertThat(tools).extracting(tool -> tool.get("name"))
            .containsExactly(
                "slack_1", "aiAgentUtils_1", "aiAgentUtils_2", "workflow_1", "aiAgentUtils_3", "aiAgentUtils_4",
                "aiAgentUtils_5", "aiAgentUtils_6", "aiAgentUtils_7", "aiAgentUtils_8", "aiAgentUtils_9",
                "brave_1", "approval_1");

        Map<String, Object> gate = toolByType(clusterElements, "aiAgentUtils/v1/approvalGateTool");

        assertThat(gate.get("name")).isEqualTo("aiAgentUtils_1");

        @SuppressWarnings("unchecked")
        Map<String, Object> gateClusterElements = (Map<String, Object>) gate.get("clusterElements");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> gateApprovalChannels =
            (List<Map<String, Object>>) gateClusterElements.get("approvalChannels");

        assertThat(gateApprovalChannels).extracting(channel -> channel.get("name"))
            .containsExactly("chat_1", "telegram_1");

        Map<String, Object> approvalTool = toolByType(clusterElements, "approval/v1/requestApproval");

        @SuppressWarnings("unchecked")
        Map<String, Object> approvalToolClusterElements =
            (Map<String, Object>) approvalTool.get("clusterElements");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> approvalToolChannels =
            (List<Map<String, Object>>) approvalToolClusterElements.get("approvalChannels");

        assertThat(approvalToolChannels).extracting(channel -> channel.get("name"))
            .containsExactly("chat_2", "telegram_2");

        Map<String, Object> webSearchTool = toolByType(clusterElements, "brave/v1/webSearch");

        assertThat(webSearchTool.get("name")).isEqualTo("brave_1");
        assertClusterElementConnectionsBlock(webSearchTool, "brave");
    }

    private static AiAgent maximalFixtureAgent() {
        AiAgent agent = newAgent();

        agent.setSettings(
            Map.of(
                "builtInTools",
                Map.of(
                    "askUserQuestion", true, "autoMemory", true, "skills", true, "skillManagement", true,
                    "webSearch", true, "webSearchConnectionId", 99)));

        return agent;
    }

    /**
     * MODEL + ungated {@code slack} TOOL + gated {@code stripe} TOOL (forcing the gate) + an {@code APPROVAL_GATE}
     * expiry override + two {@code SKILL} rows + one {@code SUB_AGENT} + a {@code KNOWLEDGE_BASE} + {@code CHAT_MEMORY}
     * + an {@code APPROVAL_TOOL} element + two {@code APPROVAL_CHANNEL} rows (chat, then slack — shared by the gate and
     * the approval tool).
     */
    private static List<AiAgentElement> maximalFixtureElements() {
        AiAgentElement model = new AiAgentElement(1L, AiAgentElement.KIND_MODEL);

        model.setId(1L);
        model.setPosition(0);
        model.setParameters(Map.of("provider", "openAi", "model", "gpt-5-nano"));

        AiAgentElement ungatedTool = new AiAgentElement(1L, AiAgentElement.KIND_TOOL);

        ungatedTool.setId(2L);
        ungatedTool.setPosition(1);
        ungatedTool.setParameters(
            Map.of(
                "componentName", "slack", "componentVersion", 1, "actionName", "sendMessage", "parameters",
                Map.of("channel", "#general")));

        AiAgentElement gatedTool = new AiAgentElement(1L, AiAgentElement.KIND_TOOL);

        gatedTool.setId(3L);
        gatedTool.setPosition(2);
        gatedTool.setParameters(
            Map.of(
                "componentName", "stripe", "componentVersion", 1, "actionName", "createRefund", "parameters",
                Map.of("amount", 100), "requiresApproval", true));

        AiAgentElement approvalGateTool = new AiAgentElement(1L, AiAgentElement.KIND_APPROVAL_GATE);

        approvalGateTool.setId(4L);
        approvalGateTool.setPosition(3);
        approvalGateTool.setParameters(Map.of("approvalExpiresIn", 4, "approvalExpiresInUnit", "HOURS"));

        AiAgentElement skill1 = new AiAgentElement(1L, AiAgentElement.KIND_SKILL);

        skill1.setId(5L);
        skill1.setPosition(4);
        skill1.setReferenceId(10L);

        AiAgentElement skill2 = new AiAgentElement(1L, AiAgentElement.KIND_SKILL);

        skill2.setId(6L);
        skill2.setPosition(5);
        skill2.setReferenceId(20L);

        AiAgentElement subAgent = new AiAgentElement(1L, AiAgentElement.KIND_SUB_AGENT);

        subAgent.setId(7L);
        subAgent.setPosition(6);
        subAgent.setReferenceId(SUB_AGENT_REFERENCE_ID);

        AiAgentElement knowledgeBase = new AiAgentElement(1L, AiAgentElement.KIND_KNOWLEDGE_BASE);

        knowledgeBase.setId(8L);
        knowledgeBase.setPosition(7);
        knowledgeBase.setReferenceId(7L);
        knowledgeBase.setParameters(Map.of("topK", 4, "similarityThreshold", 0.5));

        AiAgentElement chatMemory = new AiAgentElement(1L, AiAgentElement.KIND_CHAT_MEMORY);

        chatMemory.setId(9L);
        chatMemory.setPosition(8);

        AiAgentElement approvalTool = new AiAgentElement(1L, AiAgentElement.KIND_APPROVAL_TOOL);

        approvalTool.setId(10L);
        approvalTool.setPosition(9);

        return List.of(
            model, ungatedTool, gatedTool, approvalGateTool, skill1, skill2, subAgent, knowledgeBase, chatMemory,
            approvalTool);
    }

    // --- Built-in tool settings -------------------------------------------------------------------------------

    @Test
    void testDefaultSettingsEmitAskUserQuestionAutoMemoryAndSkillManagementButNotWebSearch() {
        List<Map<String, Object>> tools = tools(generateFullElementsClusterElements());

        assertThat(tools).extracting(tool -> tool.get("type"))
            .containsAll(DEFAULT_BUILT_IN_TOOL_TYPES)
            .doesNotContain("brave/v1/webSearch");
    }

    @Test
    void testWebSearchEnabledEmitsBraveWebSearchToolWiredToTheBraveConnection() {
        AiAgent agent = newAgent();

        agent.setSettings(Map.of("builtInTools", Map.of("webSearch", true)));

        Map<String, Object> clusterElements = generateClusterElements(agent, List.of());

        Map<String, Object> webSearchTool = toolByType(clusterElements, "brave/v1/webSearch");

        assertThat(webSearchTool.get("parameters")).isEqualTo(Map.of());
        assertClusterElementConnectionsBlock(webSearchTool, "brave");
    }

    @Test
    void testWebSearchConnectionRefIsKeyedByTheBraveToolElementNode() {
        AiAgent agent = newAgent();

        agent.setSettings(Map.of("builtInTools", Map.of("webSearch", true, "webSearchConnectionId", 55)));

        List<AiAgentWorkflowGenerator.ConnectionRef> connectionRefs =
            AiAgentWorkflowGenerator.buildConnectionRefs(agent, List.of(), List.of(), CHANNEL_RESOLVER);

        // askUserQuestion(aiAgentUtils_1)/autoMemory(aiAgentUtils_2)/skillManagement(aiAgentUtils_3..7) reserve node
        // names but add no ref (see buildConnectionRefs' javadoc). webSearch draws from the brave counter rather
        // than that one — it is the brave component's own tool element, not an aiAgentUtils element.
        assertThat(connectionRefs).extracting(
            AiAgentWorkflowGenerator.ConnectionRef::workflowNodeName,
            AiAgentWorkflowGenerator.ConnectionRef::workflowConnectionKey,
            AiAgentWorkflowGenerator.ConnectionRef::ownerKind)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(
                    "aiAgent_1", "brave_1",
                    AiAgentWorkflowGenerator.ConnectionRefOwnerKind.AGENT_SETTINGS));
    }

    /**
     * An agent stored before {@code webSearchProvider} existed carries only {@code webSearch: true} — it must keep
     * generating the brave tool it generated then, which the two tests above already pin. This pins the other half:
     * writing the default value explicitly changes nothing.
     */
    @Test
    void testExplicitBraveProviderGeneratesTheSameToolAsAnAbsentProvider() {
        AiAgent implicitAgent = newAgent();

        implicitAgent.setSettings(Map.of("builtInTools", Map.of("webSearch", true)));

        AiAgent explicitAgent = newAgent();

        explicitAgent.setSettings(
            Map.of("builtInTools", Map.of("webSearch", true, "webSearchProvider", "BRAVE")));

        assertThat(generateClusterElements(explicitAgent, List.of()))
            .isEqualTo(generateClusterElements(implicitAgent, List.of()));
    }

    @Test
    void testFirecrawlProviderEmitsFirecrawlSearchToolWiredToTheFirecrawlConnection() {
        AiAgent agent = newAgent();

        agent.setSettings(
            Map.of("builtInTools", Map.of("webSearch", true, "webSearchProvider", "FIRECRAWL")));

        Map<String, Object> clusterElements = generateClusterElements(agent, List.of());

        Map<String, Object> webSearchTool = toolByType(clusterElements, "firecrawl/v1/search");

        assertThat(webSearchTool.get("name")).isEqualTo("firecrawl_1");
        assertThat(webSearchTool.get("parameters")).isEqualTo(Map.of());
        assertClusterElementConnectionsBlock(webSearchTool, "firecrawl");

        assertThat(tools(clusterElements)).extracting(tool -> tool.get("type"))
            .doesNotContain("brave/v1/webSearch");
    }

    @Test
    void testFirecrawlProviderConnectionRefIsKeyedByTheFirecrawlToolElementNode() {
        AiAgent agent = newAgent();

        agent.setSettings(
            Map.of(
                "builtInTools",
                Map.of("webSearch", true, "webSearchProvider", "FIRECRAWL", "webSearchConnectionId", 55)));

        List<AiAgentWorkflowGenerator.ConnectionRef> connectionRefs =
            AiAgentWorkflowGenerator.buildConnectionRefs(agent, List.of(), List.of(), CHANNEL_RESOLVER);

        assertThat(connectionRefs).extracting(
            AiAgentWorkflowGenerator.ConnectionRef::workflowNodeName,
            AiAgentWorkflowGenerator.ConnectionRef::workflowConnectionKey,
            AiAgentWorkflowGenerator.ConnectionRef::ownerKind)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(
                    "aiAgent_1", "firecrawl_1",
                    AiAgentWorkflowGenerator.ConnectionRefOwnerKind.AGENT_SETTINGS));
    }

    /**
     * Native web search is a model property, not a tool: the provider searches inside the completion, so there is no
     * tool element to emit and — the part that matters for deployment — no connection ref to resolve.
     */
    @Test
    void testNativeProviderEmitsNoToolElementAndSetsTheModelWebSearchParameter() {
        AiAgentElement model = new AiAgentElement(1L, AiAgentElement.KIND_MODEL);

        model.setId(1L);
        model.setPosition(0);
        model.setParameters(Map.of("provider", "anthropic", "model", "claude-sonnet-4-5"));

        AiAgent agent = newAgent();

        agent.setSettings(Map.of("builtInTools", Map.of("webSearch", true, "webSearchProvider", "NATIVE")));

        Map<String, Object> clusterElements = generateClusterElements(agent, List.of(model));

        assertThat(tools(clusterElements)).extracting(tool -> tool.get("type"))
            .doesNotContain("brave/v1/webSearch", "firecrawl/v1/search");

        @SuppressWarnings("unchecked")
        Map<String, Object> modelElement = (Map<String, Object>) clusterElements.get("model");

        assertThat(parameters(modelElement))
            .isEqualTo(Map.of("webSearch", true, "model", "claude-sonnet-4-5"));
    }

    @Test
    void testNativeProviderAddsNoConnectionRef() {
        AiAgent agent = newAgent();

        agent.setSettings(
            Map.of(
                "builtInTools",
                Map.of("webSearch", true, "webSearchProvider", "NATIVE", "webSearchConnectionId", 55)));

        List<AiAgentWorkflowGenerator.ConnectionRef> connectionRefs =
            AiAgentWorkflowGenerator.buildConnectionRefs(agent, List.of(), List.of(), CHANNEL_RESOLVER);

        assertThat(connectionRefs).isEmpty();
    }

    /**
     * The settings map is free-form JSON, so a typo must degrade to the documented default rather than make an
     * otherwise valid agent ungeneratable.
     */
    @Test
    void testUnknownProviderFallsBackToBrave() {
        AiAgent agent = newAgent();

        agent.setSettings(
            Map.of("builtInTools", Map.of("webSearch", true, "webSearchProvider", "GOOGLE")));

        Map<String, Object> clusterElements = generateClusterElements(agent, List.of());

        assertThat(toolByType(clusterElements, "brave/v1/webSearch").get("name")).isEqualTo("brave_1");
    }

    /**
     * Turning native web search off must not leave the previous generation's true behind — every save regenerates the
     * whole definition, so the key has to be absent, not stale.
     */
    @Test
    void testModelWebSearchParameterIsAbsentWhenWebSearchIsOff() {
        AiAgentElement model = new AiAgentElement(1L, AiAgentElement.KIND_MODEL);

        model.setId(1L);
        model.setPosition(0);
        model.setParameters(Map.of("provider", "anthropic", "model", "claude-sonnet-4-5"));

        @SuppressWarnings("unchecked")
        Map<String, Object> modelElement =
            (Map<String, Object>) generateClusterElements(List.of(model)).get("model");

        assertThat(parameters(modelElement)).doesNotContainKey("webSearch");
    }

    @Test
    void testStreamResponseOffSwitchesTheAiAgentNodeToTheChatAction() {
        AiAgent agent = newAgent();

        agent.setSettings(Map.of("streamResponse", false));

        Map<String, Object> parsed = generateAndParse(agent, twoChannelFixtureChannels());

        Map<String, Object> aiAgentNode = tasks(parsed).get(1);

        assertThat(aiAgentNode.get("type")).isEqualTo("aiAgent/v1/chat");

        // The action swap must not touch the output contract every reply node reads: neither action is
        // given a `response` parameter, so both still emit an unnamed string.
        assertThat(parameters(aiAgentNode)).doesNotContainKey("response");
    }

    @Test
    void testStreamResponseDefaultsToTheStreamChatActionWhenTheKeyIsAbsent() {
        AiAgent agent = newAgent();

        agent.setSettings(Map.of("builtInTools", Map.of("webSearch", false)));

        Map<String, Object> parsed = generateAndParse(agent, twoChannelFixtureChannels());

        assertThat(tasks(parsed).get(1)
            .get("type")).isEqualTo("aiAgent/v1/streamChat");
    }

    @Test
    void testSkillRowsAloneEmitSkillsToolRegardlessOfSettings() {
        AiAgent agent = newAgent();

        // There is deliberately no `skills` built-in toggle: attaching a SKILL row IS the opt-in, so a
        // stale/hostile settings map cannot suppress a skill the user explicitly added.
        agent.setSettings(Map.of("builtInTools", Map.of("skills", false)));

        AiAgentElement skill = new AiAgentElement(1L, AiAgentElement.KIND_SKILL);

        skill.setId(1L);
        skill.setPosition(0);
        skill.setReferenceId(10L);

        Map<String, Object> clusterElements = generateClusterElements(agent, List.of(skill));

        assertThat(tools(clusterElements)).extracting(tool -> tool.get("type"))
            .contains("aiAgentUtils/v1/skillsTool")
            .containsAll(DEFAULT_BUILT_IN_TOOL_TYPES);
    }

    @Test
    void testNoSkillRowsOmitsSkillsTool() {
        AiAgent agent = newAgent();

        Map<String, Object> clusterElements = generateClusterElements(agent, List.of());

        assertThat(tools(clusterElements)).extracting(tool -> tool.get("type"))
            .doesNotContain("aiAgentUtils/v1/skillsTool");
    }

    @Test
    void testAllBuiltInsOffAndNoElementsYieldsEmptyClusterElements() {
        AiAgent agent = newAgent();

        agent.setSettings(
            Map.of(
                "builtInTools",
                Map.of(
                    "askUserQuestion", false, "autoMemory", false, "skillManagement", false, "webSearch", false)));

        String definition =
            AiAgentWorkflowGenerator.generate(agent, twoChannelFixtureChannels(), List.of(), NO_SUB_AGENTS, null,
                CHANNEL_RESOLVER);
        Map<String, Object> parsed = JsonUtils.read(definition, new TypeReference<>() {});

        Map<String, Object> aiAgentNode = tasks(parsed).get(1);

        assertThat(aiAgentNode.get("clusterElements")).isEqualTo(Map.of());
    }

    @Test
    void testBuiltInSettingsGenerationIsDeterministic() {
        AiAgent agent = newAgent();

        agent.setSettings(
            Map.of("builtInTools", Map.of("webSearch", true, "webSearchConnectionId", 7, "skills", false)));

        List<AiAgentChannel> channels = twoChannelFixtureChannels();

        String first =
            AiAgentWorkflowGenerator.generate(agent, channels, List.of(), NO_SUB_AGENTS, null, CHANNEL_RESOLVER);
        String second =
            AiAgentWorkflowGenerator.generate(agent, channels, List.of(), NO_SUB_AGENTS, null, CHANNEL_RESOLVER);

        assertThat(second).isEqualTo(first);
    }

    /**
     * A singleton {@code APPROVAL_TOOL} row plus two {@code APPROVAL_CHANNEL} rows (chat, then slack) — no gate, no
     * gated tool, so the tool is the only approval-related entry in {@code tools[]}.
     */
    private static List<AiAgentElement> approvalToolFixtureElements() {
        AiAgentElement approvalTool = new AiAgentElement(1L, AiAgentElement.KIND_APPROVAL_TOOL);

        approvalTool.setId(1L);
        approvalTool.setPosition(0);

        return List.of(approvalTool);
    }

    /**
     * One ungated TOOL (jira), one gated TOOL (stripe, {@code requiresApproval=true}), an APPROVAL_GATE row with a
     * 4-HOURS expiry override, and two APPROVAL_CHANNEL rows (chat, then slack).
     */
    private static List<AiAgentElement> hitlFixtureElements() {
        AiAgentElement ungatedTool = new AiAgentElement(1L, AiAgentElement.KIND_TOOL);

        ungatedTool.setId(1L);
        ungatedTool.setPosition(0);
        ungatedTool.setParameters(
            Map.of(
                "componentName", "jira", "componentVersion", 1, "actionName", "createIssue", "parameters",
                Map.of("project", "OPS")));

        AiAgentElement gatedTool = new AiAgentElement(1L, AiAgentElement.KIND_TOOL);

        gatedTool.setId(2L);
        gatedTool.setPosition(1);
        gatedTool.setParameters(
            Map.of(
                "componentName", "stripe", "componentVersion", 1, "actionName", "createRefund", "parameters",
                Map.of("amount", 100), "requiresApproval", true));

        AiAgentElement approvalGateTool = new AiAgentElement(1L, AiAgentElement.KIND_APPROVAL_GATE);

        approvalGateTool.setId(3L);
        approvalGateTool.setPosition(2);
        approvalGateTool.setParameters(Map.of("approvalExpiresIn", 4, "approvalExpiresInUnit", "HOURS"));

        return List.of(ungatedTool, gatedTool, approvalGateTool);
    }

    private static Map<String, Object> generateHitlClusterElements() {
        AiAgent agent = newAgent();
        List<AiAgentChannel> channels = twoChannelFixtureChannels();
        List<AiAgentElement> elements = hitlFixtureElements();

        String definition =
            AiAgentWorkflowGenerator.generate(agent, channels, elements, NO_SUB_AGENTS, null, CHANNEL_RESOLVER);
        Map<String, Object> parsed = JsonUtils.read(definition, new TypeReference<>() {});

        Map<String, Object> aiAgentNode = tasks(parsed).get(1);

        @SuppressWarnings("unchecked")
        Map<String, Object> clusterElements = (Map<String, Object>) aiAgentNode.get("clusterElements");

        return clusterElements;
    }

    private static Map<String, Object> generateClusterElements(List<AiAgentElement> elements) {
        return generateClusterElements(newAgent(), elements);
    }

    private static Map<String, Object> generateClusterElements(AiAgent agent, List<AiAgentElement> elements) {
        return generateClusterElements(agent, elements, NO_SUB_AGENTS);
    }

    private static Map<String, Object> generateClusterElements(
        AiAgent agent, List<AiAgentElement> elements, Function<Long, SubAgentRef> subAgentResolver) {

        List<AiAgentChannel> channels = twoChannelFixtureChannels();

        String definition =
            AiAgentWorkflowGenerator.generate(agent, channels, elements, subAgentResolver, null, CHANNEL_RESOLVER);
        Map<String, Object> parsed = JsonUtils.read(definition, new TypeReference<>() {});

        Map<String, Object> aiAgentNode = tasks(parsed).get(1);

        @SuppressWarnings("unchecked")
        Map<String, Object> clusterElements = (Map<String, Object>) aiAgentNode.get("clusterElements");

        return clusterElements;
    }

    private static Map<String, Object> generateFullElementsClusterElements() {
        AiAgent agent = newAgent();
        List<AiAgentChannel> channels = twoChannelFixtureChannels();
        List<AiAgentElement> elements = fullElementsFixtureElements();

        String definition =
            AiAgentWorkflowGenerator.generate(agent, channels, elements, FULL_ELEMENTS_SUB_AGENT_RESOLVER, null,
                CHANNEL_RESOLVER);
        Map<String, Object> parsed = JsonUtils.read(definition, new TypeReference<>() {});

        Map<String, Object> aiAgentNode = tasks(parsed).get(1);

        @SuppressWarnings("unchecked")
        Map<String, Object> clusterElements = (Map<String, Object>) aiAgentNode.get("clusterElements");

        return clusterElements;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> tools(Map<String, Object> clusterElements) {
        return (List<Map<String, Object>>) clusterElements.get("tools");
    }

    private static Map<String, Object> toolByType(Map<String, Object> clusterElements, String type) {
        return tools(clusterElements).stream()
            .filter(tool -> type.equals(tool.get("type")))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No tool with type " + type));
    }

    /**
     * One row per {@code AiAgentElement} kind (Task 9 brief's fixture): MODEL + one TOOL + two SKILL + one SUB_AGENT +
     * KNOWLEDGE_BASE + CHAT_MEMORY, position-ordered as listed. No {@code AUTO_MEMORY} row — that kind is retired; the
     * generated {@code autoMemoryTool} entry this fixture still expects (see {@code agent_workflow_full_elements.json})
     * now comes from {@code newAgent()}'s default settings ({@code AiAgentSettings#isAutoMemoryEnabled} defaults ON)
     * instead of a stored row.
     */
    private static List<AiAgentElement> fullElementsFixtureElements() {
        AiAgentElement model = new AiAgentElement(1L, "MODEL");

        model.setId(1L);
        model.setPosition(0);
        model.setParameters(Map.of("provider", "openAi", "model", "gpt-5-nano"));

        AiAgentElement tool = new AiAgentElement(1L, "TOOL");

        tool.setId(2L);
        tool.setPosition(1);
        tool.setParameters(
            Map.of(
                "componentName", "slack", "componentVersion", 1, "actionName", "sendMessage", "parameters",
                Map.of("channel", "#general")));

        AiAgentElement skill1 = new AiAgentElement(1L, "SKILL");

        skill1.setId(3L);
        skill1.setPosition(2);
        skill1.setReferenceId(10L);

        AiAgentElement skill2 = new AiAgentElement(1L, "SKILL");

        skill2.setId(4L);
        skill2.setPosition(3);
        skill2.setReferenceId(20L);

        AiAgentElement subAgent = new AiAgentElement(1L, AiAgentElement.KIND_SUB_AGENT);

        subAgent.setId(5L);
        subAgent.setPosition(4);
        subAgent.setReferenceId(SUB_AGENT_REFERENCE_ID);

        AiAgentElement knowledgeBase = new AiAgentElement(1L, "KNOWLEDGE_BASE");

        knowledgeBase.setId(6L);
        knowledgeBase.setPosition(5);
        knowledgeBase.setReferenceId(7L);
        knowledgeBase.setParameters(Map.of("topK", 4, "similarityThreshold", 0.5));

        AiAgentElement chatMemory = new AiAgentElement(1L, "CHAT_MEMORY");

        chatMemory.setId(7L);
        chatMemory.setPosition(6);

        return List.of(model, tool, skill1, skill2, subAgent, knowledgeBase, chatMemory);
    }

    private static Object scheduleConversationId(Map<String, Object> parsed) {
        Map<String, Object> branchInParameters = parameters(tasks(parsed).get(0));
        Map<String, Object> scheduleCase = caseInCasesByKey(branchInParameters, "schedule_1");

        Map<String, Object> envelopeTask = replyTasks(scheduleCase).get(0);

        @SuppressWarnings("unchecked")
        Map<String, Object> envelope = (Map<String, Object>) parameters(envelopeTask).get("value");

        return envelope.get("conversationId");
    }

    /**
     * A TASK or TRIGGER node's connections block is keyed by component name — it is a workflow node in its own right,
     * so it follows the same convention a plain task does.
     */
    private static void assertConnectionsBlock(Map<String, Object> node, String componentName) {
        assertConnectionsBlock(node, componentName, componentName);
    }

    /**
     * A CLUSTER ELEMENT's connections block is keyed by the element's OWN node name, not its component name. That key
     * is what every runtime consumer resolves the connection under
     * ({@code componentConnections.get(clusterElement.getWorkflowNodeName())}), and keying it by component name instead
     * made {@code ClusterRootComponentConnectionFactory} emit a second, differently-keyed entry for the same element —
     * which is what asked for the same connection twice in the deployment dialog.
     */
    private static void assertClusterElementConnectionsBlock(Map<String, Object> node, String componentName) {
        assertConnectionsBlock(node, String.valueOf(node.get("name")), componentName);
    }

    private static void assertConnectionsBlock(
        Map<String, Object> node, String connectionKey, String componentName) {

        @SuppressWarnings("unchecked")
        Map<String, Object> connections = (Map<String, Object>) node.get("connections");

        assertThat(connections).containsKey(connectionKey);

        @SuppressWarnings("unchecked")
        Map<String, Object> connection = (Map<String, Object>) connections.get(connectionKey);

        assertThat(connection.get("componentName")).isEqualTo(componentName);
        assertThat(connection.get("componentVersion")).isEqualTo(1);
    }

    private static Map<String, Object> generateAndParse(AiAgent agent, List<AiAgentChannel> channels) {
        String definition =
            AiAgentWorkflowGenerator.generate(agent, channels, List.of(), NO_SUB_AGENTS, null, CHANNEL_RESOLVER);

        return JsonUtils.read(definition, new TypeReference<>() {});
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> triggers(Map<String, Object> parsed) {
        return (List<Map<String, Object>>) parsed.get("triggers");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> tasks(Map<String, Object> parsed) {
        return (List<Map<String, Object>>) parsed.get("tasks");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parameters(Map<String, Object> node) {
        return (Map<String, Object>) node.get("parameters");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> cases(Map<String, Object> dispatcherParameters) {
        return (List<Map<String, Object>>) dispatcherParameters.get("cases");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> replyTasks(Map<String, Object> oneCase) {
        return (List<Map<String, Object>>) oneCase.get("tasks");
    }

    private static Map<String, Object> caseByKey(Map<String, Object> parsed, String triggerNodeName) {
        return caseInCasesByKey(parameters(tasks(parsed).get(2)), triggerNodeName);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> envelopeOf(Map<String, Object> parsed, String triggerNodeName) {
        Map<String, Object> branchInCase = caseInCasesByKey(parameters(tasks(parsed).get(0)), triggerNodeName);
        Map<String, Object> envelopeSetTask = replyTasks(branchInCase).get(0);

        return (Map<String, Object>) parameters(envelopeSetTask).get("value");
    }

    private static AiAgentChannel newChannel(
        String channelType, long id, int position, Map<String, Object> parameters) {

        AiAgentChannel channel = new AiAgentChannel(1L, channelType);

        channel.setId(id);
        channel.setPosition(position);
        channel.setConnectionId(200L);
        channel.setParameters(parameters);

        return channel;
    }

    private static Map<String, Object> caseInCasesByKey(Map<String, Object> dispatcherParameters, String key) {
        return cases(dispatcherParameters).stream()
            .filter(oneCase -> key.equals(oneCase.get("key")))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No case with key " + key));
    }

    private static AiAgent newAgent() {
        AiAgent agent = new AiAgent();

        agent.setId(1L);
        agent.setName("news-agent");
        agent.setTitle("News Agent");
        agent.setUuid(AGENT_UUID);

        return agent;
    }

    private static List<AiAgentChannel> twoChannelFixtureChannels() {
        AiAgentChannel chatChannel = new AiAgentChannel(1L, AiAgentChannelType.CHAT);

        chatChannel.setId(1L);
        chatChannel.setPosition(0);

        AiAgentChannel workflowCallChannel = new AiAgentChannel(1L, AiAgentChannelType.WORKFLOW_CALL);

        workflowCallChannel.setId(2L);
        workflowCallChannel.setPosition(1);

        AiAgentChannel telegramChannel = new AiAgentChannel(1L, TestAgentChannels.TELEGRAM);

        telegramChannel.setId(3L);
        telegramChannel.setPosition(2);
        telegramChannel.setConnectionId(100L);

        AiAgentChannel scheduleChannel = new AiAgentChannel(1L, AiAgentChannelType.SCHEDULE);

        scheduleChannel.setId(4L);
        scheduleChannel.setPosition(3);
        scheduleChannel.setParameters(
            Map.of("prompt", STORED_SCHEDULE_PROMPT, "expression", "0 9 * * *", "timezone", "UTC"));

        return List.of(chatChannel, workflowCallChannel, telegramChannel, scheduleChannel);
    }
}
