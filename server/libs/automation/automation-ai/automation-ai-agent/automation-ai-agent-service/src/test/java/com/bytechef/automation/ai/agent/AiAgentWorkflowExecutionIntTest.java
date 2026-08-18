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
import com.bytechef.platform.workflow.JobInputConstants;
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
 * End-to-end verification of the generated agent workflow's inbound branch routing: builds a real two-channel (chat,
 * telegram) {@code AiAgent} through {@link AiAgentWorkflowGenerator}, then drives its {@code branch_in} dispatcher
 * through the same {@code JobSyncExecutor}-based harness the branch task-dispatcher module uses for its own IntTests
 * (see {@code server/libs/modules/task-dispatchers/branch/src/test}'s {@code BranchTaskDispatcherIntTest}), asserting
 * that real inputs are routed to the right case and the envelope's SpEL expressions evaluate to the expected fields.
 *
 * <p>
 * Route taken: {@code branch_in}'s own case tasks are {@code var/v1/set} envelopes (no aiAgent/LLM call and no
 * branch_out reply are needed to observe routing), so this executes the workflow definition's inbound half only.
 * {@code aiAgent_1} and {@code branch_out} are covered structurally (definition shape, not live execution) by
 * {@code AiAgentWorkflowGeneratorTest} and the facade IntTests. Since {@code ClassPathResourceWorkflowRepository} only
 * loads workflows from a fixed classpath resource pattern (no in-memory registration hook), the trimmed workflow is a
 * checked-in fixture ({@code workflows/agent_workflow_branch_in.yaml}) rather than the generator's raw output written
 * at test time; {@link #testGeneratedBranchInMatchesExecutionFixture()} pins that fixture's {@code branch_in} task to
 * equal what the real generator emits for the identical fixture agent, so the two can never silently drift apart.
 *
 * @author Ivica Cardic
 */
@TaskDispatcherIntTest
// This module's *other* IntTests are JDBC/Liquibase-backed (real AiAgent persistence), so spring-data-jdbc and its
// autoconfiguration are on the whole module's test classpath; the "testint" profile the testIntegration Gradle task
// activates for every module (com.bytechef.java-common-conventions.gradle.kts) then makes Spring Boot try to build a
// DataSource bean with no configured URL. TaskDispatcherJobTestExecutor never needs a DataSource -- see
// TaskDispatcherIntTestConfiguration, which wires an in-memory job pipeline directly -- so it's excluded here rather
// than given real connection properties.
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

    /**
     * Drift guard for {@code workflows/agent_workflow_branch_in.yaml}: the fixture's {@code branch_in} task must stay
     * byte-identical to what {@link AiAgentWorkflowGenerator#generate} emits for the same two-channel (chat, telegram)
     * agent — otherwise this test's "real" branch_in would silently diverge from the generator it is supposed to be
     * exercising.
     */
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
