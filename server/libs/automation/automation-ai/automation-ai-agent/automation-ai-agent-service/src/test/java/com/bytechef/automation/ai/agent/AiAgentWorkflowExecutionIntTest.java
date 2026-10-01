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

package com.bytechef.automation.ai.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.automation.ai.agent.channel.AiAgentChannelType;
import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.domain.AiAgentChannel;
import com.bytechef.automation.ai.agent.test.TestAgentChannels;
import com.bytechef.automation.ai.agent.util.AiAgentWorkflowGenerator;
import com.bytechef.automation.ai.agent.util.AiAgentWorkflowGenerator.SubAgentRef;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.platform.constant.JobInputConstants;
import com.bytechef.platform.workflow.task.dispatcher.test.annotation.TaskDispatcherIntTest;
import com.bytechef.platform.workflow.task.dispatcher.test.task.handler.TestVarTaskHandler;
import com.bytechef.platform.workflow.task.dispatcher.test.workflow.TaskDispatcherJobTestExecutor;
import com.bytechef.platform.workflow.task.dispatcher.test.workflow.TaskDispatcherJobTestExecutor.TaskDispatcherJobExecution;
import com.bytechef.task.dispatcher.branch.BranchTaskDispatcher;
import com.bytechef.task.dispatcher.branch.completion.BranchTaskCompletionHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.core.type.TypeReference;

/**
 * @author Ivica Cardic
 */
@TaskDispatcherIntTest
@TestPropertySource(
    properties = "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration")
class AiAgentWorkflowExecutionIntTest {

    private static final Evaluator EVALUATOR = SpelEvaluator.create();

    private static final String WORKFLOW_ID =
        EncodingUtils.base64EncodeToString("agent_workflow_branch_in".getBytes(StandardCharsets.UTF_8));

    private static final Function<Long, SubAgentRef> NO_SUB_AGENTS = referenceId -> {
        throw new AssertionError("The chat+telegram fixture carries no SUB_AGENT elements");
    };

    @Autowired
    private TaskDispatcherJobTestExecutor taskDispatcherJobTestExecutor;

    @Autowired
    private TaskFileStorage taskFileStorage;

    @Test
    void testGeneratedBranchInMatchesExecutionFixture() {
        Map<String, Object> generated = generateTwoChannelDefinition();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tasks = (List<Map<String, Object>>) generated.get("tasks");

        Map<String, Object> generatedBranchIn = tasks.get(0);

        assertThat(generatedBranchIn.get("name")).isEqualTo("branch_in");

        Map<String, Object> fixture = readFixtureWorkflow();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fixtureTasks = (List<Map<String, Object>>) fixture.get("tasks");

        assertThat(fixtureTasks.get(0)).isEqualTo(generatedBranchIn);
    }

    @Test
    void testChatChannelEnvelopeIsRoutedAndEvaluated() {
        Map<String, Object> envelope = executeBranchIn(
            Map.of(
                JobInputConstants.TRIGGER_NAME_INPUT, "chat_1",
                "chat_1", Map.of(
                    "message", "hi",
                    "conversationId", "c1",
                    "attachments", List.of())));

        assertThat(envelope).containsEntry("channel", "chat")
            .containsEntry("text", "hi")
            .containsEntry("conversationId", "c1")
            .containsEntry("attachments", List.of());
    }

    @Test
    void testTelegramChannelEnvelopeIsRoutedAndEvaluated() {
        Map<String, Object> envelope = executeBranchIn(
            Map.of(
                JobInputConstants.TRIGGER_NAME_INPUT, "telegram_1",
                "telegram_1", Map.of(
                    "message", Map.of(
                        "text", "hello bot",
                        "chat", Map.of("id", 555)))));

        assertThat(envelope).containsEntry("channel", "telegram")
            .containsEntry("text", "hello bot")
            .containsEntry("conversationId", 555)
            .containsEntry("attachments", List.of());
    }

    private Map<String, Object> executeBranchIn(Map<String, Object> inputs) {
        return executeBranchIn(WORKFLOW_ID, inputs);
    }

    private Map<String, Object> executeBranchIn(String workflowId, Map<String, Object> inputs) {
        TaskDispatcherJobExecution jobExecution = taskDispatcherJobTestExecutor.execute(
            workflowId, inputs, this::getTaskCompletionHandlerFactories, this::getTaskDispatcherResolverFactories,
            this::getTaskHandlerMap);

        Job job = jobExecution.job();

        assertThat(jobExecution.getExecutionErrors()).isEmpty();

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(job.getOutputs());

        @SuppressWarnings("unchecked")
        Map<String, Object> envelope = (Map<String, Object>) outputs.get("result");

        return envelope;
    }

    private static Map<String, Object> generateTwoChannelDefinition() {
        String definition = AiAgentWorkflowGenerator.generate(
            newAgent(), twoChannelFixtureChannels(), List.of(), NO_SUB_AGENTS, null, TestAgentChannels.resolver());

        return JsonUtils.read(definition, new TypeReference<>() {});
    }

    private static Map<String, Object> readFixtureWorkflow() {
        return readFixtureWorkflow("/workflows/agent_workflow_branch_in.json");
    }

    private static Map<String, Object> readFixtureWorkflow(String classpathResource) {
        try (InputStream inputStream = AiAgentWorkflowExecutionIntTest.class.getResourceAsStream(classpathResource)) {

            String json = new String(
                Objects.requireNonNull(inputStream, classpathResource)
                    .readAllBytes(),
                StandardCharsets.UTF_8);

            return JsonUtils.read(json, new TypeReference<>() {});
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static AiAgent newAgent() {
        AiAgent agent = new AiAgent();

        agent.setId(1L);
        agent.setName("branch-routing-agent");
        agent.setTitle("Branch Routing Agent");
        agent.setUuid(UUID.fromString("33333333-3333-3333-3333-333333333333"));

        return agent;
    }

    private static List<AiAgentChannel> twoChannelFixtureChannels() {
        AiAgentChannel chatChannel = new AiAgentChannel(1L, AiAgentChannelType.CHAT);

        chatChannel.setId(1L);
        chatChannel.setPosition(0);

        AiAgentChannel telegramChannel = new AiAgentChannel(1L, TestAgentChannels.TELEGRAM);

        telegramChannel.setId(2L);
        telegramChannel.setPosition(1);
        telegramChannel.setConnectionId(100L);

        return List.of(chatChannel, telegramChannel);
    }

    @SuppressWarnings("PMD")
    private List<TaskCompletionHandlerFactory> getTaskCompletionHandlerFactories(
        ContextService contextService, CounterService counterService, TaskExecutionService taskExecutionService) {

        return List.of(
            (taskCompletionHandler, taskDispatcher) -> new BranchTaskCompletionHandler(
                contextService, EVALUATOR, taskCompletionHandler, taskDispatcher, taskExecutionService,
                taskFileStorage));
    }

    @SuppressWarnings("PMD")
    private List<TaskDispatcherResolverFactory> getTaskDispatcherResolverFactories(
        ApplicationEventPublisher eventPublisher, ContextService contextService,
        CounterService counterService, TaskExecutionService taskExecutionService) {

        return List.of(
            (taskDispatcher) -> new BranchTaskDispatcher(
                contextService, EVALUATOR, eventPublisher, taskDispatcher, taskExecutionService,
                taskFileStorage));
    }

    private Map<String, TaskHandler<?>> getTaskHandlerMap() {
        return Map.of("var/v1/set", new TestVarTaskHandler<>(Map::put));
    }
}
