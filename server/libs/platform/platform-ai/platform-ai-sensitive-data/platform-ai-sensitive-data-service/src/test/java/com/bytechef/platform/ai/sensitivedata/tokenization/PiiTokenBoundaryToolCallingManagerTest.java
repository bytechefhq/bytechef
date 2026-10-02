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

package com.bytechef.platform.ai.sensitivedata.tokenization;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.ai.sensitivedata.SensitiveDataDetector;
import com.bytechef.platform.ai.sensitivedata.SensitiveDataMetrics;
import com.bytechef.platform.ai.sensitivedata.SensitiveDataRedactor;
import com.bytechef.platform.ai.sensitivedata.SensitiveKind;
import com.bytechef.platform.ai.sensitivedata.SensitiveSpan;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.util.MimeType;

/**
 * @author Ivica Cardic
 */
class PiiTokenBoundaryToolCallingManagerTest {

    private static final String SUSPENDED_TOOL_RESULT = "__BYTECHEF_TOOL_SUSPENDED__:" + UUID.randomUUID();

    @Test
    void testTokensInToolArgumentsAreRestoredBeforeTheToolRuns() {
        PiiTokenSession session = PiiTokenSession.create();
        String token = session.tokenFor("EMAIL_ADDRESS", "bob@acme.io");

        RecordingToolCallingManager delegate = new RecordingToolCallingManager();
        ToolCallingManager manager = PiiTokenBoundaryToolCallingManager.wrap(delegate, redactor(), () -> null);

        manager.executeToolCalls(
            promptWithSession(session), chatResponseWithToolCall("sendEmail", "{\"to\":\"" + token + "\"}"));

        assertThat(delegate.lastArguments()).isEqualTo("{\"to\":\"bob@acme.io\"}");
    }

    @Test
    void testAnUnresolvedTokenIsLeftInPlaceAndDoesNotAbortTheRun() {
        PiiTokenSession session = PiiTokenSession.create();

        RecordingToolCallingManager delegate = new RecordingToolCallingManager();
        ToolCallingManager manager = PiiTokenBoundaryToolCallingManager.wrap(delegate, redactor(), () -> null);

        manager.executeToolCalls(
            promptWithSession(session),
            chatResponseWithToolCall("sendEmail", "{\"to\":\"[PII_EMAIL_ADDRESS_9_zzzz]\"}"));

        assertThat(delegate.lastArguments()).isEqualTo("{\"to\":\"[PII_EMAIL_ADDRESS_9_zzzz]\"}");
    }

    @Test
    void testWithNoSessionInToolContextTheDelegateSeesArgumentsUnchanged() {
        RecordingToolCallingManager delegate = new RecordingToolCallingManager();
        ToolCallingManager manager = PiiTokenBoundaryToolCallingManager.wrap(delegate, redactor(), () -> null);

        manager.executeToolCalls(promptWithoutSession(), chatResponseWithToolCall("sendEmail", "{\"to\":\"x\"}"));

        assertThat(delegate.lastArguments()).isEqualTo("{\"to\":\"x\"}");
    }

    @Test
    void testWithNoSessionInToolContextTheDelegateSeesTheSameChatResponseInstance() {
        RecordingToolCallingManager delegate = new RecordingToolCallingManager();
        ToolCallingManager manager = PiiTokenBoundaryToolCallingManager.wrap(delegate, redactor(), () -> null);
        ChatResponse chatResponse = chatResponseWithToolCall("sendEmail", "{\"to\":\"x\"}");

        manager.executeToolCalls(promptWithoutSession(), chatResponse);

        assertThat(delegate.lastChatResponse()).isSameAs(chatResponse);
    }

    @Test
    void testResolveToolDefinitionsDelegatesUnchanged() {
        RecordingToolCallingManager delegate = new RecordingToolCallingManager();
        ToolCallingManager manager = PiiTokenBoundaryToolCallingManager.wrap(delegate, redactor(), () -> null);
        ToolCallingChatOptions chatOptions = ToolCallingChatOptions.builder()
            .build();

        List<ToolDefinition> toolDefinitions = manager.resolveToolDefinitions(chatOptions);

        assertThat(delegate.lastResolvedChatOptions()).isSameAs(chatOptions);
        assertThat(toolDefinitions).isSameAs(delegate.toolDefinitionsToReturn());
    }

    @Test
    void testPiiInAToolResultIsTokenizedBeforeItReachesTheModel() {
        PiiTokenSession session = PiiTokenSession.create();

        ToolCallingManager manager = PiiTokenBoundaryToolCallingManager.wrap(
            returningResult("{\"email\":\"bob@acme.io\"}"), redactor(), () -> null);

        ToolExecutionResult result = manager.executeToolCalls(promptWithSession(session), anyToolCall());

        assertThat(lastResponseData(result)).doesNotContain("bob@acme.io")
            .contains("[PII_EMAIL_ADDRESS_");
    }

    @Test
    void testPiiInAToolResultIsLeftUntouchedWhenTheWorkspaceHasPiiRedactionOff() {
        PiiTokenSession session = PiiTokenSession.create();
        SensitiveDataPolicy piiOffPolicy = new SensitiveDataPolicy(
            Set.of(SensitiveKind.SECRET), SensitiveDataRedactor.DEFAULT_MIN_CONFIDENCE, true);

        ToolCallingManager manager = PiiTokenBoundaryToolCallingManager.wrap(
            returningResult("{\"email\":\"bob@acme.io\"}"), redactor(), () -> null);

        ToolExecutionResult result =
            manager.executeToolCalls(promptWithSessionAndPolicy(session, piiOffPolicy), anyToolCall());

        assertThat(lastResponseData(result)).contains("bob@acme.io")
            .doesNotContain("[PII_")
            .doesNotContain("[REDACTED_");
    }

    @Test
    void testASecretInAToolResultIsStillRedactedWhenPiiRedactionIsOff() {
        PiiTokenSession session = PiiTokenSession.create();
        SensitiveDataPolicy piiOffPolicy = new SensitiveDataPolicy(
            Set.of(SensitiveKind.SECRET), SensitiveDataRedactor.DEFAULT_MIN_CONFIDENCE, true);

        ToolCallingManager manager = PiiTokenBoundaryToolCallingManager.wrap(
            returningResult("key=AKIAIOSFODNN7EXAMPLE"), redactor(), () -> null);

        ToolExecutionResult result =
            manager.executeToolCalls(promptWithSessionAndPolicy(session, piiOffPolicy), anyToolCall());

        assertThat(lastResponseData(result)).doesNotContain("AKIAIOSFODNN7EXAMPLE")
            .contains("[REDACTED_");
    }

    @Test
    void testAnExplicitMinConfidenceIsHonouredInsteadOfTheDefault() {
        PiiTokenSession session = PiiTokenSession.create();
        SensitiveDataPolicy highBarPolicy =
            new SensitiveDataPolicy(Set.of(SensitiveKind.PII, SensitiveKind.SECRET), 0.95, true);

        ToolCallingManager manager = PiiTokenBoundaryToolCallingManager.wrap(
            returningResult("{\"email\":\"bob@acme.io\"}"), redactor(), () -> null);

        ToolExecutionResult result =
            manager.executeToolCalls(promptWithSessionAndPolicy(session, highBarPolicy), anyToolCall());

        assertThat(lastResponseData(result)).contains("bob@acme.io");
    }

    @Test
    void testTheSameValueKeepsOneTokenAcrossTheWholeCall() {
        PiiTokenSession session = PiiTokenSession.create();
        String promptToken = session.tokenFor("EMAIL_ADDRESS", "bob@acme.io");

        ToolCallingManager manager = PiiTokenBoundaryToolCallingManager.wrap(
            returningResult("{\"email\":\"bob@acme.io\"}"), redactor(), () -> null);

        ToolExecutionResult result = manager.executeToolCalls(promptWithSession(session), anyToolCall());

        assertThat(lastResponseData(result)).contains(promptToken);
    }

    @Test
    void testASecretInAToolResultIsRedactedAndNotTokenized() {
        PiiTokenSession session = PiiTokenSession.create();

        ToolCallingManager manager = PiiTokenBoundaryToolCallingManager.wrap(
            returningResult("key=AKIAIOSFODNN7EXAMPLE"), redactor(), () -> null);

        ToolExecutionResult result = manager.executeToolCalls(promptWithSession(session), anyToolCall());

        assertThat(lastResponseData(result)).doesNotContain("AKIAIOSFODNN7EXAMPLE")
            .contains("[REDACTED_")
            .doesNotContain("[PII_");
    }

    @Test
    void testAResultThatCannotBeTokenizedIsRedactedRatherThanReturnedRaw() {
        PiiTokenSession session = PiiTokenSession.create();

        ToolCallingManager manager = PiiTokenBoundaryToolCallingManager.wrap(
            returningResult("{\"email\":\"bob@acme.io\"}"), throwingOnTokenizeRedactor(), () -> null);

        ToolExecutionResult result = manager.executeToolCalls(promptWithSession(session), anyToolCall());

        assertThat(lastResponseData(result)).doesNotContain("bob@acme.io")
            .contains("[REDACTED_");
    }

    @Test
    void testTheSuspendSentinelSurvivesTheNormalTokenizationPath() {
        PiiTokenSession session = PiiTokenSession.create();

        ToolCallingManager manager = PiiTokenBoundaryToolCallingManager.wrap(
            returningResult(SUSPENDED_TOOL_RESULT), redactor(), () -> null);

        ToolExecutionResult result = manager.executeToolCalls(promptWithSession(session), anyToolCall());

        assertThat(lastResponseData(result)).isEqualTo(SUSPENDED_TOOL_RESULT);
    }

    @Test
    void testTheSuspendSentinelSurvivesTheFailClosedPathEvenWhenRedactionAlsoThrows() {
        PiiTokenSession session = PiiTokenSession.create();

        ToolCallingManager manager = PiiTokenBoundaryToolCallingManager.wrap(
            returningResult(SUSPENDED_TOOL_RESULT), throwingOnEverythingRedactor(), () -> null);

        ToolExecutionResult result = manager.executeToolCalls(promptWithSession(session), anyToolCall());

        assertThat(lastResponseData(result)).isEqualTo(SUSPENDED_TOOL_RESULT);
    }

    @Test
    void testOtherMessagesInTheConversationHistoryAreLeftUnchangedAndInPlace() {
        PiiTokenSession session = PiiTokenSession.create();
        UserMessage userMessage = new UserMessage("what is the customer's email?");

        ToolCallingManager manager = PiiTokenBoundaryToolCallingManager.wrap(
            returningResultAfter(userMessage, "{\"email\":\"bob@acme.io\"}"), redactor(), () -> null);

        ToolExecutionResult result = manager.executeToolCalls(promptWithSession(session), anyToolCall());

        assertThat(result.conversationHistory()).hasSize(3);
        assertThat(result.conversationHistory()
            .getFirst()).isSameAs(userMessage);
        assertThat(lastResponseData(result)).doesNotContain("bob@acme.io");
    }

    @Test
    void testTheToolReceivesRealValuesWhileTheReturnedAssistantMessageCarriesTheToken() {
        PiiTokenSession session = PiiTokenSession.create();
        String token = session.tokenFor("EMAIL_ADDRESS", "bob@acme.io");

        RecordingToolCallingManager delegate = new RecordingToolCallingManager();
        ToolCallingManager manager = PiiTokenBoundaryToolCallingManager.wrap(delegate, redactor(), () -> null);

        ToolExecutionResult result = manager.executeToolCalls(
            promptWithSession(session), chatResponseWithToolCall("sendEmail", "{\"to\":\"" + token + "\"}"));

        assertThat(delegate.lastArguments()).isEqualTo("{\"to\":\"bob@acme.io\"}");
        assertThat(assistantToolCallArguments(result)).doesNotContain("bob@acme.io");
        assertThat(assistantToolCallArguments(result)).isEqualTo("{\"to\":\"" + token + "\"}");
    }

    @Test
    void testTheSameValueKeepsOneTokenBetweenTheAssistantMessageAndTheToolResult() {
        PiiTokenSession session = PiiTokenSession.create();
        String token = session.tokenFor("EMAIL_ADDRESS", "bob@acme.io");

        ToolCallingManager manager = PiiTokenBoundaryToolCallingManager.wrap(
            returningResult("{\"email\":\"bob@acme.io\"}"), redactor(), () -> null);

        ToolExecutionResult result = manager.executeToolCalls(
            promptWithSession(session), chatResponseWithToolCall("lookupCustomer", "{\"email\":\"" + token + "\"}"));

        assertThat(assistantToolCallArguments(result)).isEqualTo("{\"email\":\"" + token + "\"}");
        assertThat(lastResponseData(result)).isEqualTo("{\"email\":\"" + token + "\"}");
    }

    @Test
    void testMultipleToolRoundsKeepTheSameTokenInEveryReturnedAssistantMessage() {
        PiiTokenSession session = PiiTokenSession.create();
        String token = session.tokenFor("EMAIL_ADDRESS", "bob@acme.io");

        RecordingToolCallingManager delegate = new RecordingToolCallingManager();
        ToolCallingManager manager = PiiTokenBoundaryToolCallingManager.wrap(delegate, redactor(), () -> null);

        ToolExecutionResult firstRoundResult = manager.executeToolCalls(
            promptWithSession(session), chatResponseWithToolCall("sendEmail", "{\"to\":\"" + token + "\"}"));

        Prompt secondPrompt = new Prompt(
            firstRoundResult.conversationHistory(),
            ToolCallingChatOptions.builder()
                .toolContext(PiiTokenSessionToolContext.into(Map.of(), session))
                .build());

        ToolExecutionResult secondRoundResult = manager.executeToolCalls(
            secondPrompt, chatResponseWithToolCall("sendEmail", "{\"to\":\"" + token + "\"}"));

        List<AssistantMessage> assistantMessages = assistantMessagesIn(secondRoundResult);

        assertThat(assistantMessages).hasSize(2);

        for (AssistantMessage assistantMessage : assistantMessages) {
            String arguments = assistantMessage.getToolCalls()
                .getFirst()
                .arguments();

            assertThat(arguments).doesNotContain("bob@acme.io");
            assertThat(arguments).isEqualTo("{\"to\":\"" + token + "\"}");
        }
    }

    @Test
    void testToolCallArgumentsAreRestoredOnANonFirstGenerationAndMetadataAndMediaSurviveTheRebuild() {
        PiiTokenSession session = PiiTokenSession.create();
        String token = session.tokenFor("EMAIL_ADDRESS", "bob@acme.io");

        ChatGenerationMetadata firstGenerationMetadata = ChatGenerationMetadata.builder()
            .finishReason("STOP")
            .build();
        ChatGenerationMetadata secondGenerationMetadata = ChatGenerationMetadata.builder()
            .finishReason("TOOL_CALLS")
            .build();
        Media media = new Media(MimeType.valueOf("image/png"), URI.create("https://acme.io/screenshot.png"));

        AssistantMessage firstGenerationOutput = AssistantMessage.builder()
            .content("I looked into it but did not need any tools.")
            .build();
        AssistantMessage secondGenerationOutput = AssistantMessage.builder()
            .content("")
            .toolCalls(List.of(toolCall("call-1", "sendEmail", "{\"to\":\"" + token + "\"}")))
            .media(List.of(media))
            .build();

        ChatResponse chatResponse = new ChatResponse(
            List.of(
                new Generation(firstGenerationOutput, firstGenerationMetadata),
                new Generation(secondGenerationOutput, secondGenerationMetadata)));

        RecordingToolCallingManager delegate = new RecordingToolCallingManager();
        ToolCallingManager manager = PiiTokenBoundaryToolCallingManager.wrap(delegate, redactor(), () -> null);

        manager.executeToolCalls(promptWithSession(session), chatResponse);

        ChatResponse forwarded = delegate.lastChatResponse();

        assertThat(forwarded).isNotNull();

        List<Generation> forwardedGenerations = Objects.requireNonNull(forwarded)
            .getResults();

        assertThat(forwardedGenerations).hasSize(2);
        assertThat(forwardedGenerations.get(0)
            .getMetadata()).isEqualTo(firstGenerationMetadata);

        AssistantMessage restoredOutput = forwardedGenerations.get(1)
            .getOutput();

        assertThat(restoredOutput.getToolCalls()).singleElement()
            .extracting(AssistantMessage.ToolCall::arguments)
            .isEqualTo("{\"to\":\"bob@acme.io\"}");
        assertThat(forwardedGenerations.get(1)
            .getMetadata()).isEqualTo(secondGenerationMetadata);
        assertThat(restoredOutput.getMedia()).containsExactly(media);
    }

    @Test
    void testRecordsToolArgsRestoredAtMostOncePerInvocationWithSeveralToolCalls() {
        PiiTokenSession session = PiiTokenSession.create();
        String firstToken = session.tokenFor("EMAIL_ADDRESS", "bob@acme.io");
        String secondToken = session.tokenFor("EMAIL_ADDRESS", "alice@acme.io");
        RecordingMetrics metrics = new RecordingMetrics();

        ToolCallingManager manager =
            PiiTokenBoundaryToolCallingManager.wrap(new RecordingToolCallingManager(), redactor(), () -> metrics);

        manager.executeToolCalls(
            promptWithSession(session),
            chatResponseWithToolCalls(
                toolCall("call-1", "sendEmail", "{\"to\":\"" + firstToken + "\"}"),
                toolCall("call-2", "sendEmail", "{\"to\":\"" + secondToken + "\"}")));

        assertThat(metrics.toolArgsRestoredCount).isEqualTo(1);
    }

    @Test
    void testTheMetricsSupplierIsConsultedPerInvocationNotOnceAtConstruction() {
        AtomicReference<SensitiveDataMetrics> currentMetrics = new AtomicReference<>();

        ToolCallingManager manager = PiiTokenBoundaryToolCallingManager.wrap(
            new RecordingToolCallingManager(), redactor(), currentMetrics::get);

        PiiTokenSession firstSession = PiiTokenSession.create();
        String firstToken = firstSession.tokenFor("EMAIL_ADDRESS", "bob@acme.io");

        manager.executeToolCalls(
            promptWithSession(firstSession),
            chatResponseWithToolCall("sendEmail", "{\"to\":\"" + firstToken + "\"}"));

        RecordingMetrics metrics = new RecordingMetrics();

        currentMetrics.set(metrics);

        PiiTokenSession secondSession = PiiTokenSession.create();
        String secondToken = secondSession.tokenFor("EMAIL_ADDRESS", "alice@acme.io");

        manager.executeToolCalls(
            promptWithSession(secondSession),
            chatResponseWithToolCall("sendEmail", "{\"to\":\"" + secondToken + "\"}"));

        assertThat(metrics.toolArgsRestoredCount)
            .as("the invocation after the supplier started resolving should record through it")
            .isEqualTo(1);
    }

    @Test
    void testTheSessionIsStrippedFromTheToolContextTheToolItselfSees() {
        PiiTokenSession session = PiiTokenSession.create();
        String token = session.tokenFor("EMAIL_ADDRESS", "bob@acme.io");

        RecordingToolCallingManager delegate = new RecordingToolCallingManager();
        ToolCallingManager manager =
            PiiTokenBoundaryToolCallingManager.wrap(delegate, redactor(), () -> null);

        ToolCallingChatOptions chatOptions = ToolCallingChatOptions.builder()
            .toolContext(
                PiiTokenSessionToolContext.into(
                    Map.of("bytechef.agent-tool-invocation-context", "must-survive"), session))
            .build();

        manager.executeToolCalls(
            new Prompt(List.of(new UserMessage("hi")), chatOptions),
            chatResponseWithToolCall("sendEmail", "{\"to\":\"" + token + "\"}"));

        Map<String, Object> observedToolContext = delegate.lastToolContext();

        assertThat(observedToolContext)
            .as("the tool must not be able to reach the session")
            .doesNotContainKey(PiiTokenSessionToolContext.KEY);
        assertThat(observedToolContext)
            .as("every other tool-context entry must survive the strip")
            .containsEntry("bytechef.agent-tool-invocation-context", "must-survive");

        assertThat(delegate.lastArguments()).isEqualTo("{\"to\":\"bob@acme.io\"}");
    }

    @Test
    void testDoesNotRecordToolArgsRestoredWhenNothingWasRestored() {
        RecordingMetrics metrics = new RecordingMetrics();
        ToolCallingManager manager =
            PiiTokenBoundaryToolCallingManager.wrap(new RecordingToolCallingManager(), redactor(), () -> metrics);

        manager.executeToolCalls(
            promptWithSession(PiiTokenSession.create()), chatResponseWithToolCall("sendEmail", "{\"to\":\"x\"}"));

        assertThat(metrics.toolArgsRestoredCount).isZero();
    }

    @Test
    void testRecordsTokenUnresolvedWhenAnArgumentTokenCannotBeResolved() {
        RecordingMetrics metrics = new RecordingMetrics();
        ToolCallingManager manager =
            PiiTokenBoundaryToolCallingManager.wrap(new RecordingToolCallingManager(), redactor(), () -> metrics);

        manager.executeToolCalls(
            promptWithSession(PiiTokenSession.create()),
            chatResponseWithToolCall("sendEmail", "{\"to\":\"[PII_EMAIL_ADDRESS_9_zzzz]\"}"));

        assertThat(metrics.tokenUnresolvedCount).isEqualTo(1);
        assertThat(metrics.toolArgsRestoredCount).isZero();
    }

    @Test
    void testRecordsToolResultTokenizedAtMostOncePerInvocationWithSeveralResponses() {
        PiiTokenSession session = PiiTokenSession.create();
        RecordingMetrics metrics = new RecordingMetrics();

        ToolCallingManager manager = PiiTokenBoundaryToolCallingManager.wrap(
            returningResults("{\"email\":\"bob@acme.io\"}", "key=AKIAIOSFODNN7EXAMPLE"), redactor(), () -> metrics);

        manager.executeToolCalls(promptWithSession(session), anyToolCall());

        assertThat(metrics.toolResultTokenizedCount).isEqualTo(1);
    }

    @Test
    void testDoesNotRecordToolResultTokenizedWhenNothingWasTokenized() {
        PiiTokenSession session = PiiTokenSession.create();
        RecordingMetrics metrics = new RecordingMetrics();

        ToolCallingManager manager = PiiTokenBoundaryToolCallingManager.wrap(
            returningResults("nothing sensitive here"), redactor(), () -> metrics);

        manager.executeToolCalls(promptWithSession(session), anyToolCall());

        assertThat(metrics.toolResultTokenizedCount).isZero();
    }

    @Test
    void testRecordsAssistantHistoryRetokenizedWhenAnAssistantToolCallArgumentIsRetokenized() {
        PiiTokenSession session = PiiTokenSession.create();
        String token = session.tokenFor("EMAIL_ADDRESS", "bob@acme.io");
        RecordingMetrics metrics = new RecordingMetrics();

        ToolCallingManager manager =
            PiiTokenBoundaryToolCallingManager.wrap(new RecordingToolCallingManager(), redactor(), () -> metrics);

        manager.executeToolCalls(
            promptWithSession(session), chatResponseWithToolCall("sendEmail", "{\"to\":\"" + token + "\"}"));

        assertThat(metrics.assistantHistoryRetokenizedCount).isEqualTo(1);
    }

    @Test
    void testDoesNotRecordAssistantHistoryRetokenizedWhenNothingWasRetokenized() {
        RecordingMetrics metrics = new RecordingMetrics();
        ToolCallingManager manager =
            PiiTokenBoundaryToolCallingManager.wrap(new RecordingToolCallingManager(), redactor(), () -> metrics);

        manager.executeToolCalls(
            promptWithSession(PiiTokenSession.create()), chatResponseWithToolCall("sendEmail", "{\"to\":\"x\"}"));

        assertThat(metrics.assistantHistoryRetokenizedCount).isZero();
    }

    private static ToolCallingManager returningResult(String responseData) {
        return new ReturningResultToolCallingManager(List.of(), responseData);
    }

    private static ToolCallingManager returningResultAfter(Message precedingMessage, String responseData) {
        return new ReturningResultToolCallingManager(List.of(precedingMessage), responseData);
    }

    private static ToolCallingManager returningResults(String... responseDataItems) {
        return new ReturningMultipleResultsToolCallingManager(List.of(responseDataItems));
    }

    private static ChatResponse anyToolCall() {
        return chatResponseWithToolCall("lookupCustomer", "{}");
    }

    private static String lastResponseData(ToolExecutionResult result) {
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) result.conversationHistory()
            .getLast();

        return toolResponseMessage.getResponses()
            .getFirst()
            .responseData();
    }

    private static String assistantToolCallArguments(ToolExecutionResult result) {
        return assistantMessageIn(result).getToolCalls()
            .getFirst()
            .arguments();
    }

    private static AssistantMessage assistantMessageIn(ToolExecutionResult result) {
        return assistantMessagesIn(result).getFirst();
    }

    private static List<AssistantMessage> assistantMessagesIn(ToolExecutionResult result) {
        List<AssistantMessage> assistantMessages = new ArrayList<>();

        for (Message message : result.conversationHistory()) {
            if (message instanceof AssistantMessage assistantMessage) {
                assistantMessages.add(assistantMessage);
            }
        }

        return assistantMessages;
    }

    private static AssistantMessage assistantMessageOf(ChatResponse chatResponse) {
        Generation generation = chatResponse.getResult();

        if (generation == null) {
            throw new IllegalStateException("Expected a generation in the ChatResponse passed to executeToolCalls");
        }

        return generation.getOutput();
    }

    private static ToolResponseMessage inertToolResponseFor(AssistantMessage assistantMessage) {
        List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>(assistantMessage.getToolCalls()
            .size());

        for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
            responses.add(new ToolResponseMessage.ToolResponse(toolCall.id(), toolCall.name(), "ok"));
        }

        return ToolResponseMessage.builder()
            .responses(responses)
            .build();
    }

    private static SensitiveDataRedactor redactor() {
        return new SensitiveDataRedactor(
            List.of(
                literalDetector("email", SensitiveKind.PII, "EMAIL_ADDRESS", "bob@acme.io"),
                literalDetector("aws-key", SensitiveKind.SECRET, "AWS_ACCESS_KEY", "AKIAIOSFODNN7EXAMPLE")));
    }

    private static SensitiveDataRedactor throwingOnTokenizeRedactor() {
        return new ThrowingOnTokenizeRedactor(
            List.of(literalDetector("email", SensitiveKind.PII, "EMAIL_ADDRESS", "bob@acme.io")));
    }

    private static SensitiveDataRedactor throwingOnEverythingRedactor() {
        return new ThrowingOnEverythingRedactor(List.of());
    }

    private static SensitiveDataDetector literalDetector(
        String name, SensitiveKind kind, String category, String literal) {

        return new SensitiveDataDetector() {

            @Override
            public String name() {
                return name;
            }

            @Override
            public List<SensitiveSpan> detect(String text) {
                int start = text.indexOf(literal);

                if (start < 0) {
                    return List.of();
                }

                return List.of(new SensitiveSpan(kind, category, start, start + literal.length(), 0.9));
            }
        };
    }

    private static final class ThrowingOnTokenizeRedactor extends SensitiveDataRedactor {

        private ThrowingOnTokenizeRedactor(List<SensitiveDataDetector> detectors) {
            super(detectors);
        }

        @Override
        public RedactionResult tokenizeWithSpans(
            String text, Set<SensitiveKind> kinds, PiiTokenSession session, double minConfidence,
            @Nullable SensitiveDataMetrics metrics) {

            throw new IllegalStateException("tokenization is broken");
        }
    }

    private static final class ThrowingOnEverythingRedactor extends SensitiveDataRedactor {

        private ThrowingOnEverythingRedactor(List<SensitiveDataDetector> detectors) {
            super(detectors);
        }

        @Override
        public RedactionResult tokenizeWithSpans(
            String text, Set<SensitiveKind> kinds, PiiTokenSession session, double minConfidence,
            @Nullable SensitiveDataMetrics metrics) {

            throw new IllegalStateException("tokenization is broken");
        }

        @Override
        public String redact(
            String text, Set<SensitiveKind> kinds, double minConfidence, @Nullable SensitiveDataMetrics metrics) {

            throw new IllegalStateException("redaction is broken too");
        }
    }

    private static final class ReturningMultipleResultsToolCallingManager implements ToolCallingManager {

        private final List<String> responseDataItems;

        private ReturningMultipleResultsToolCallingManager(List<String> responseDataItems) {
            this.responseDataItems = responseDataItems;
        }

        @Override
        public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
            return List.of();
        }

        @Override
        public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
            List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>(responseDataItems.size());

            for (int index = 0; index < responseDataItems.size(); index++) {
                responses.add(
                    new ToolResponseMessage.ToolResponse(
                        "call-" + index, "lookupCustomer", responseDataItems.get(index)));
            }

            ToolResponseMessage toolResponseMessage = ToolResponseMessage.builder()
                .responses(responses)
                .build();

            return ToolExecutionResult.builder()
                .conversationHistory(List.of(assistantMessageOf(chatResponse), toolResponseMessage))
                .build();
        }
    }

    private static final class ReturningResultToolCallingManager implements ToolCallingManager {

        private final List<Message> precedingMessages;
        private final String responseData;

        private ReturningResultToolCallingManager(List<Message> precedingMessages, String responseData) {
            this.precedingMessages = precedingMessages;
            this.responseData = responseData;
        }

        @Override
        public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
            return List.of();
        }

        @Override
        public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
            ToolResponseMessage.ToolResponse response = new ToolResponseMessage.ToolResponse(
                "call-1", "lookupCustomer", responseData);
            ToolResponseMessage toolResponseMessage = ToolResponseMessage.builder()
                .responses(List.of(response))
                .build();

            List<Message> conversationHistory = new ArrayList<>(precedingMessages);

            conversationHistory.add(assistantMessageOf(chatResponse));
            conversationHistory.add(toolResponseMessage);

            return ToolExecutionResult.builder()
                .conversationHistory(conversationHistory)
                .build();
        }
    }

    private static Prompt promptWithSession(PiiTokenSession session) {
        return new Prompt(
            List.of(),
            ToolCallingChatOptions.builder()
                .toolContext(PiiTokenSessionToolContext.into(Map.of(), session))
                .build());
    }

    private static Prompt promptWithSessionAndPolicy(PiiTokenSession session, SensitiveDataPolicy policy) {
        Map<String, Object> toolContext = SensitiveDataPolicyToolContext.into(
            PiiTokenSessionToolContext.into(Map.of(), session), policy);

        return new Prompt(
            List.of(),
            ToolCallingChatOptions.builder()
                .toolContext(toolContext)
                .build());
    }

    private static Prompt promptWithoutSession() {
        return new Prompt(List.of(), ToolCallingChatOptions.builder()
            .build());
    }

    private static ChatResponse chatResponseWithToolCall(String toolName, String arguments) {
        return chatResponseWithToolCalls(toolCall("call-1", toolName, arguments));
    }

    private static ChatResponse chatResponseWithToolCalls(AssistantMessage.ToolCall... toolCalls) {
        AssistantMessage assistantMessage = AssistantMessage.builder()
            .content("")
            .toolCalls(List.of(toolCalls))
            .build();

        return new ChatResponse(List.of(new Generation(assistantMessage)));
    }

    private static AssistantMessage.ToolCall toolCall(String id, String toolName, String arguments) {
        return new AssistantMessage.ToolCall(id, "function", toolName, arguments);
    }

    private static final class RecordingToolCallingManager implements ToolCallingManager {

        private final List<ToolDefinition> toolDefinitionsToReturn = List.of();

        private @Nullable ToolCallingChatOptions lastResolvedChatOptions;
        private @Nullable ChatResponse lastChatResponse;
        private @Nullable Map<String, Object> lastToolContext;

        @Override
        public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
            lastResolvedChatOptions = chatOptions;

            return toolDefinitionsToReturn;
        }

        @Override
        public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
            lastChatResponse = chatResponse;

            if (prompt.getOptions() instanceof ToolCallingChatOptions toolCallingChatOptions) {
                lastToolContext = toolCallingChatOptions.getToolContext();
            }

            AssistantMessage assistantMessage = assistantMessageOf(chatResponse);
            List<Message> conversationHistory = new ArrayList<>(prompt.getInstructions());

            conversationHistory.add(assistantMessage);
            conversationHistory.add(inertToolResponseFor(assistantMessage));

            return ToolExecutionResult.builder()
                .conversationHistory(conversationHistory)
                .build();
        }

        private Map<String, Object> lastToolContext() {
            return lastToolContext == null ? Map.of() : lastToolContext;
        }

        private String lastArguments() {
            ChatResponse chatResponse = lastChatResponse;

            if (chatResponse == null) {
                throw new IllegalStateException("executeToolCalls was never called");
            }

            Generation generation = chatResponse.getResult();

            if (generation == null) {
                throw new IllegalStateException("Expected a generation in the recorded ChatResponse");
            }

            return generation.getOutput()
                .getToolCalls()
                .getFirst()
                .arguments();
        }

        private @Nullable ChatResponse lastChatResponse() {
            return lastChatResponse;
        }

        private @Nullable ToolCallingChatOptions lastResolvedChatOptions() {
            return lastResolvedChatOptions;
        }

        private List<ToolDefinition> toolDefinitionsToReturn() {
            return toolDefinitionsToReturn;
        }
    }

    private static final class RecordingMetrics implements SensitiveDataMetrics {

        private int toolArgsRestoredCount;
        private int toolResultTokenizedCount;
        private int tokenUnresolvedCount;
        private int assistantHistoryRetokenizedCount;

        @Override
        public void recordDetectorFailure(String detectorName) {
        }

        @Override
        public void recordToolArgsRestored() {
            toolArgsRestoredCount++;
        }

        @Override
        public void recordToolResultTokenized() {
            toolResultTokenizedCount++;
        }

        @Override
        public void recordTokenUnresolved() {
            tokenUnresolvedCount++;
        }

        @Override
        public void recordAssistantHistoryRetokenized() {
            assistantHistoryRetokenizedCount++;
        }
    }
}
