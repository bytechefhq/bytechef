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

package com.bytechef.ai.copilot.tool.ask;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class SubAgentAskRelayToolCallbackTest {

    private static final String QUESTION_ENVELOPE =
        """
            {"kind":"ask-user-question","awaitingAnswer":true,"questions":[{"question":"Which project?",\
            "options":[{"label":"Alpha","description":"the first"},{"label":"Beta","description":"the second"}]}]}""";

    @Test
    void testToolDefinitionIsTheDelegatesVerbatim() {
        FakeDelegate delegate = new FakeDelegate("buildWorkflow", "summary");

        SubAgentAskRelayToolCallback toolCallback = new SubAgentAskRelayToolCallback(
            delegate, new FakeRelay(null), SubAgentQuestionRenderer.JSON);

        ToolDefinition toolDefinition = toolCallback.getToolDefinition();

        assertThat(toolDefinition.name()).isEqualTo("buildWorkflow");
        assertThat(toolDefinition.description()).isEqualTo("delegate description");
        assertThat(toolDefinition.inputSchema()).isEqualTo(FakeDelegate.INPUT_SCHEMA);
    }

    @Test
    void testResultPassesThroughUnchangedWhenNoQuestionWasRaised() {
        FakeDelegate delegate = new FakeDelegate("buildWorkflow", "the specialist's answer");

        SubAgentAskRelayToolCallback toolCallback = new SubAgentAskRelayToolCallback(
            delegate, new FakeRelay(null), SubAgentQuestionRenderer.JSON);

        assertThat(toolCallback.call("{\"request\":\"go\"}")).isEqualTo("the specialist's answer");
        assertThat(delegate.lastToolInput).isEqualTo("{\"request\":\"go\"}");
    }

    @Test
    void testJsonRendererReturnsTheRawEnvelopeAndDiscardsTheSpecialistSummary() {
        FakeDelegate delegate = new FakeDelegate("buildWorkflow", "I asked the user something");

        SubAgentAskRelayToolCallback toolCallback = new SubAgentAskRelayToolCallback(
            delegate, new FakeRelay(QUESTION_ENVELOPE), SubAgentQuestionRenderer.JSON);

        assertThat(toolCallback.call("{\"request\":\"go\"}")).isEqualTo(QUESTION_ENVELOPE);
    }

    @Test
    void testPlainTextRendererFormatsTheEnvelopeAsNumberedOptions() {
        FakeDelegate delegate = new FakeDelegate("configureMcpServer", "I asked the user something");

        SubAgentAskRelayToolCallback toolCallback = new SubAgentAskRelayToolCallback(
            delegate, new FakeRelay(QUESTION_ENVELOPE), SubAgentQuestionRenderer.PLAIN_TEXT);

        String result = toolCallback.call("{\"request\":\"go\"}");

        assertThat(result)
            .contains("The configureMcpServer agent needs a decision before continuing:")
            .contains("Which project?")
            .contains("1. Alpha — the first")
            .contains("2. Beta — the second")
            .contains("call configureMcpServer again, restating the original request together with the chosen"
                + " answer")
            .contains("this agent does not carry anything over from the call that asked");
    }

    @Test
    void testPlainTextRendererAppendsAnErrorShapedResultToTheQuestion() {
        // A specialist that asks and then hits an AccessDeniedException from an admin-guarded facade must not have
        // that denial dropped along with its discarded summary — the user would answer the question and the
        // re-delegation would be denied again with nothing explaining why.
        FakeDelegate delegate = new FakeDelegate(
            "configureMcpServer", "{\"error\":\"configureMcpServer failed (AccessDeniedException)\"}");

        SubAgentAskRelayToolCallback toolCallback = new SubAgentAskRelayToolCallback(
            delegate, new FakeRelay(QUESTION_ENVELOPE), SubAgentQuestionRenderer.PLAIN_TEXT);

        String result = toolCallback.call("{\"request\":\"go\"}");

        assertThat(result)
            .contains("The configureMcpServer agent needs a decision before continuing:")
            .contains("also reported an error on the call that asked")
            .contains("AccessDeniedException");
    }

    @Test
    void testJsonRendererKeepsTheEnvelopeByteIdenticalDespiteAnErrorShapedResult() {
        // The JSON envelope is parsed by the client as the ask-user-question payload, so the error is logged rather
        // than appended: trailing prose would fail that parse and cost the user the choice card entirely.
        FakeDelegate delegate = new FakeDelegate(
            "configureMcpServer", "{\"error\":\"configureMcpServer failed (AccessDeniedException)\"}");

        SubAgentAskRelayToolCallback toolCallback = new SubAgentAskRelayToolCallback(
            delegate, new FakeRelay(QUESTION_ENVELOPE), SubAgentQuestionRenderer.JSON);

        assertThat(toolCallback.call("{\"request\":\"go\"}")).isEqualTo(QUESTION_ENVELOPE);
    }

    @Test
    void testPlainTextRendererDoesNotTreatAnOrdinaryResultMentioningErrorAsAFailure() {
        FakeDelegate delegate = new FakeDelegate(
            "configureMcpServer", "I asked the user something after fixing an error in the mapping");

        SubAgentAskRelayToolCallback toolCallback = new SubAgentAskRelayToolCallback(
            delegate, new FakeRelay(QUESTION_ENVELOPE), SubAgentQuestionRenderer.PLAIN_TEXT);

        assertThat(toolCallback.call("{\"request\":\"go\"}"))
            .doesNotContain("also reported an error on the call that asked");
    }

    @Test
    void testNullDelegateResultBecomesAToolError() {
        FakeDelegate delegate = new FakeDelegate("writeScript", null);

        SubAgentAskRelayToolCallback toolCallback = new SubAgentAskRelayToolCallback(
            delegate, new FakeRelay(null), SubAgentQuestionRenderer.JSON);

        assertThat(toolCallback.call("{\"request\":\"go\"}")).contains("writeScript subagent returned null");
    }

    private static final class FakeRelay implements SubAgentAskRelay {

        private final @Nullable String pendingQuestion;

        private FakeRelay(@Nullable String pendingQuestion) {
            this.pendingQuestion = pendingQuestion;
        }

        @Override
        public ToolCallback askUserQuestionToolCallback() {
            return new FakeDelegate("askUserQuestion", "unused");
        }

        @Override
        public <T> AskOutcome<T> runWithChannel(Supplier<T> supplier) {
            return new AskOutcome<>(supplier.get(), pendingQuestion);
        }
    }

    private static final class FakeDelegate implements ToolCallback {

        private static final String INPUT_SCHEMA = "{\"type\":\"object\"}";

        private final String name;
        private final @Nullable String result;

        private @Nullable String lastToolInput;

        private FakeDelegate(String name, @Nullable String result) {
            this.name = name;
            this.result = result;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder()
                .name(name)
                .description("delegate description")
                .inputSchema(INPUT_SCHEMA)
                .build();
        }

        @Override
        @SuppressWarnings("NullAway")
        public String call(String toolInput) {
            return call(toolInput, null);
        }

        @Override
        @SuppressWarnings("NullAway")
        public String call(String toolInput, @Nullable ToolContext toolContext) {
            lastToolInput = toolInput;

            return result;
        }
    }
}
