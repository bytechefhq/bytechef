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

package com.bytechef.component.ai.agent.guardrails.advisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.agent.guardrails.MissingModelChildException;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailStage;
import com.bytechef.platform.component.definition.ai.agent.guardrails.MaskResult;
import com.bytechef.platform.component.definition.ai.agent.guardrails.PreflightMasking;
import com.bytechef.platform.component.definition.ai.agent.guardrails.Violation;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

class CheckForViolationsAdvisorTest {

    @Test
    void testGetOrderIsHighestPrecedence() {
        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .build();

        assertThat(advisor.getOrder()).isEqualTo(Advisor.HIGHEST_PRECEDENCE);
    }

    @Test
    void testViolationBlocksAndSkipsModelCall() {
        GuardrailCheckFunction keywords = (text, context) -> text.contains("bad")
            ? Optional.of(Violation.ofMatch("keywords", "bad"))
            : Optional.empty();

        GuardrailCheckFunction passThrough = (text, context) -> Optional.empty();

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("keywords", keywords, null, null, null)
            .add("second", passThrough, null, null, null)
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("this is bad");

        ChatClientResponse response = advisor.adviseCall(request, chain);

        assertThat(response.chatResponse()
            .getResult()
            .getOutput()
            .getText()).isEqualTo("BLOCKED");
        verify(chain, never()).nextCall(any());
    }

    @Test
    void testNoViolationPassesThrough() {
        GuardrailCheckFunction always = (text, context) -> Optional.empty();

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("always", always, null, null, null)
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("clean");
        ChatClientResponse next = mock(ChatClientResponse.class);

        when(chain.nextCall(request)).thenReturn(next);

        ChatClientResponse response = advisor.adviseCall(request, chain);

        assertThat(response).isSameAs(next);
    }

    @Test
    void testExceptionInCheckFailsClosed() {
        GuardrailCheckFunction throwing = (text, context) -> {
            throw new RuntimeException("simulated failure");
        };

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("throwing", throwing, null, null, null)
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("some text");

        ChatClientResponse response = advisor.adviseCall(request, chain);

        assertThat(response.chatResponse()
            .getResult()
            .getOutput()
            .getText()).isEqualTo("BLOCKED");
        verify(chain, never()).nextCall(any());
    }

    @Test
    void testMissingModelChildExceptionConvertsToBlock() {
        GuardrailCheckFunction missingModel = (text, context) -> {
            throw new MissingModelChildException("jailbreak");
        };

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("jailbreak", missingModel, null, null, null)
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("some text");

        ChatClientResponse response = advisor.adviseCall(request, chain);

        assertThat(response.chatResponse()
            .getResult()
            .getOutput()
            .getText()).isEqualTo("BLOCKED");
        verify(chain, never()).nextCall(any());
    }

    @Test
    void testInterruptedExceptionRestoresInterruptFlagAndBlocks() {
        GuardrailCheckFunction interrupting = (text, context) -> {
            throw new InterruptedException("cancelled");
        };

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("interrupting", interrupting, null, null, null)
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("some text");

        try {
            ChatClientResponse response = advisor.adviseCall(request, chain);

            assertThat(response.chatResponse()
                .getResult()
                .getOutput()
                .getText()).isEqualTo("BLOCKED");
            assertThat(Thread.currentThread()
                .isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void testLlmStageInterruptedExceptionRestoresInterruptFlagAndBlocks() {
        // The PREFLIGHT-stage interrupt path is covered above; this test pins the LLM-stage path
        // (CheckForViolationsAdvisor#runChecks LLM loop). The interrupt flag must be restored regardless of which
        // stage produced the exception, so cancellation propagates correctly through downstream advisors and
        // executors.
        GuardrailCheckFunction llmInterrupting = new GuardrailCheckFunction() {

            @Override
            public Optional<Violation> apply(String text, GuardrailContext context) throws Exception {
                throw new InterruptedException("cancelled mid-LLM-call");
            }

            @Override
            public GuardrailStage stage() {
                return GuardrailStage.LLM;
            }
        };

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("llm-interrupting", llmInterrupting, null, null, null)
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("some text");

        try {
            ChatClientResponse response = advisor.adviseCall(request, chain);

            assertThat(response.chatResponse()
                .getResult()
                .getOutput()
                .getText()).isEqualTo("BLOCKED");
            assertThat(Thread.currentThread()
                .isInterrupted()).isTrue();
            verify(chain, never()).nextCall(any());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void testEmptyUserTextBlocksClosedWithoutRunningChecks() {
        // C2 contract: an empty USER message means there is no text to validate, so the advisor blocks-closed
        // rather than passing the request to the model with zero guardrail coverage. Individual check functions
        // are not invoked — the block is produced by the advisor itself.
        GuardrailCheckFunction neverCalled = (text, context) -> {
            throw new AssertionError("check must not run for empty text");
        };

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("neverCalled", neverCalled, null, null, null)
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("");

        ChatClientResponse response = advisor.adviseCall(request, chain);

        assertThat(response.chatResponse()
            .getResult()
            .getOutput()
            .getText()).isEqualTo("BLOCKED");
        verify(chain, never()).nextCall(any());
    }

    @Test
    void testStreamViolationShortCircuits() {
        GuardrailCheckFunction keywords = (text, context) -> text.contains("bad")
            ? Optional.of(Violation.ofMatch("keywords", "bad"))
            : Optional.empty();

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("keywords", keywords, null, null, null)
            .build();

        StreamAdvisorChain streamChain = mock(StreamAdvisorChain.class);
        ChatClientRequest request = requestWithUser("bad input");

        Flux<ChatClientResponse> flux = advisor.adviseStream(request, streamChain);

        List<ChatClientResponse> responses = flux.collectList()
            .block();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0)
            .chatResponse()
            .getResult()
            .getOutput()
            .getText()).isEqualTo("BLOCKED");
        verify(streamChain, never()).nextStream(any());
    }

    @Test
    void testStreamNoViolationDelegates() {
        GuardrailCheckFunction always = (text, context) -> Optional.empty();

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("always", always, null, null, null)
            .build();

        StreamAdvisorChain streamChain = mock(StreamAdvisorChain.class);
        ChatClientRequest request = requestWithUser("clean");

        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage("ok"))))
            .build();
        ChatClientResponse expected = ChatClientResponse.builder()
            .chatResponse(chatResponse)
            .build();

        when(streamChain.nextStream(request)).thenReturn(Flux.just(expected));

        List<ChatClientResponse> responses = advisor.adviseStream(request, streamChain)
            .collectList()
            .block();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0)).isSameAs(expected);
    }

    @Test
    void testStreamFailClosedOnCheckException() {
        // Regression pin: if adviseStream regresses to onErrorResume(_ -> Flux.empty()) or similar, a broken check
        // would silently let traffic through on the streaming path even though adviseCall still blocks. The stream
        // path must mirror the call path's fail-closed semantics.
        GuardrailCheckFunction brokenLlm = new GuardrailCheckFunction() {

            @Override
            public Optional<Violation> apply(String text, GuardrailContext context) {
                throw new RuntimeException("LLM down");
            }

            @Override
            public GuardrailStage stage() {
                return GuardrailStage.LLM;
            }
        };

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("jailbreak", brokenLlm, null, null, null)
            .build();

        StreamAdvisorChain streamChain = mock(StreamAdvisorChain.class);
        ChatClientRequest request = requestWithUser("hello");

        List<ChatClientResponse> responses = advisor.adviseStream(request, streamChain)
            .collectList()
            .block();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0)
            .chatResponse()
            .getResult()
            .getOutput()
            .getText()).isEqualTo("BLOCKED");

        verify(streamChain, never()).nextStream(any());
    }

    @Test
    void testStreamMissingModelChildBlocksClosed() {
        // Even when no MODEL is wired, the advisor must block the stream request rather than silently skip the LLM
        // check. Parity with the call path's MissingModelChildException handling.
        GuardrailCheckFunction brokenLlm = new GuardrailCheckFunction() {

            @Override
            public Optional<Violation> apply(String text, GuardrailContext context) {
                throw new MissingModelChildException("jailbreak");
            }

            @Override
            public GuardrailStage stage() {
                return GuardrailStage.LLM;
            }
        };

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("jailbreak", brokenLlm, null, null, null)
            .build();

        StreamAdvisorChain streamChain = mock(StreamAdvisorChain.class);
        ChatClientRequest request = requestWithUser("hello");

        List<ChatClientResponse> responses = advisor.adviseStream(request, streamChain)
            .collectList()
            .block();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0)
            .chatResponse()
            .getResult()
            .getOutput()
            .getText()).isEqualTo("BLOCKED");

        verify(streamChain, never()).nextStream(any());
    }

    @Test
    void testSystemOnlyRequestWithNoUserMessageBlocksClosed() {
        // A prompt with only a SystemMessage (no USER turn at all) has no user text to validate. The advisor must
        // block-closed rather than pass through and let the model process instructions that bypassed every guardrail.
        // This is distinct from "USER text = empty string" covered by
        // testEmptyUserTextBlocksClosedWithoutRunningChecks.
        GuardrailCheckFunction neverCalled = (text, context) -> {
            throw new AssertionError("check must not run when there is no USER message");
        };

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("neverCalled", neverCalled, null, null, null)
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        Prompt systemOnly = new Prompt(
            List.<Message>of(new org.springframework.ai.chat.messages.SystemMessage("only system")));
        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(systemOnly)
            .build();

        ChatClientResponse response = advisor.adviseCall(request, chain);

        assertThat(response.chatResponse()
            .getResult()
            .getOutput()
            .getText()).isEqualTo("BLOCKED");
        verify(chain, never()).nextCall(any());
    }

    private static ChatClientRequest requestWithUser(String text) {
        Prompt prompt = new Prompt(List.<Message>of(new UserMessage(text)));

        return ChatClientRequest.builder()
            .prompt(prompt)
            .build();
    }

    @Test
    void testAdvisorIsThreadSafeAcrossConcurrentInvocations() {
        // Advisors are instantiated once per-agent and reused across every chat request — Spring AI will invoke them
        // on its own thread pool. The per-request state (aggregated Violations, MaskEntityMap) lives inside runChecks
        // and must not leak between calls; checks themselves are stateless functions. Pin this by firing the same
        // advisor concurrently and asserting every invocation sees exactly its own input.
        AtomicInteger callCount = new AtomicInteger();

        GuardrailCheckFunction pattern = (text, context) -> {
            callCount.incrementAndGet();

            // The violation string must depend only on the per-call text — if the advisor were to leak state, the
            // aggregated list would contain substrings from other threads' requests.
            if (text.contains("bad-")) {
                return Optional.of(Violation.ofMatch("pattern", text.substring(text.indexOf("bad-"))));
            }

            return Optional.empty();
        };

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("pattern", pattern, null, null, null)
            .build();

        int concurrency = 100;

        IntStream.range(0, concurrency)
            .parallel()
            .forEach(index -> {
                String expected = "bad-" + index;
                List<Violation> violations = advisor.runChecksForTesting(requestWithUser("prefix " + expected));

                assertThat(violations)
                    .as("request %d must see its own substring; any mismatch means advisor state leaked across threads",
                        index)
                    .singleElement()
                    .satisfies(violation -> assertThat(((Violation.PatternViolation) violation).matchedSubstrings())
                        .containsExactly(expected));
            });

        assertThat(callCount.get()).isEqualTo(concurrency);
    }

    @Test
    void testPreflightMaskingAdvisorIsThreadSafeAcrossConcurrentInvocations() {
        // Targeted regression: the advisor instantiates a fresh MaskEntityMap per runChecks() call, so a
        // PreflightMasking guardrail invoked across many threads must never see entities from another thread's
        // request. If a future refactor hoisted maskEntities to a class-level field, the per-thread inputs recorded
        // here would interleave and the assertion that each thread saw its own value would fail.
        //
        // Companion to testAdvisorIsThreadSafeAcrossConcurrentInvocations — that test pins the violations-aggregation
        // path; this one pins the masking-aggregation path that runs for PreflightMasking checks.
        Set<String> observedTexts = ConcurrentHashMap.newKeySet();

        GuardrailCheckFunction maskingCheck = new MaskingCheck(observedTexts);

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("masking", maskingCheck, null, null, null)
            .build();

        int concurrency = 100;

        IntStream.range(0, concurrency)
            .parallel()
            .forEach(index -> {
                String unique = "secret-" + index;
                List<Violation> violations = advisor.runChecksForTesting(requestWithUser("hello " + unique));

                // Every invocation must observe its own input (no interleave with another thread's text). The
                // guardrail also reports the unique string as a match, exercising the violations-aggregation path.
                assertThat(violations)
                    .as("request %d must see its own input value", index)
                    .singleElement()
                    .satisfies(violation -> assertThat(((Violation.PatternViolation) violation).matchedSubstrings())
                        .containsExactly(unique));
            });

        assertThat(observedTexts).hasSize(concurrency);
    }

    @Test
    void testMultiTurnPromptScansEveryUserMessageForViolations() {
        // Pin the multi-turn bypass defense: CheckForViolationsAdvisor.extractUserText concatenates every USER
        // message in turn order so an attacker cannot bury payload in an earlier turn while leaving a benign
        // latest turn. A refactor to getLastUserText() or similar would silently reintroduce the bypass with
        // every existing single-turn test still green — this test is the only guard.
        GuardrailCheckFunction keywords = (text, context) -> text.contains("payload")
            ? Optional.of(Violation.ofMatch("keywords", "payload"))
            : Optional.empty();

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("keywords", keywords, null, null, null)
            .build();

        Prompt prompt = new Prompt(List.<Message>of(
            new UserMessage("earlier turn with payload buried"),
            new UserMessage("innocuous latest turn")));

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(prompt)
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        ChatClientResponse response = advisor.adviseCall(request, chain);

        assertThat(response.chatResponse()
            .getResult()
            .getOutput()
            .getText())
                .as("multi-turn bypass: payload in an earlier USER message must still trip the keyword check")
                .isEqualTo("BLOCKED");
        verify(chain, never()).nextCall(any());
    }

    @Test
    void testOutOfMemoryErrorEscapesRatherThanBecomesAViolation() {
        // Pin the "catch(Throwable) minus catch(OutOfMemoryError) rethrow" discipline: an OOM escaping a check
        // must propagate unchanged so the JVM-level handler runs, not be aggregated as a fail-closed violation
        // that hides the root cause. A future refactor that collapses the two catches into catch(Throwable) would
        // silently swallow OOM and pretend the guardrail failed cleanly. Also covers both PREFLIGHT and LLM stage
        // paths (the advisor has the OOM rethrow in both).
        GuardrailCheckFunction oomingPreflight = (text, context) -> {
            throw new OutOfMemoryError("simulated heap exhaustion");
        };

        CheckForViolationsAdvisor preflightAdvisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("ooming", oomingPreflight, null, null, null)
            .build();

        ChatClientRequest request = requestWithUser("anything");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> preflightAdvisor.runChecksForTesting(request))
            .as("OutOfMemoryError in a PREFLIGHT check must escape the advisor, not be aggregated")
            .isInstanceOf(OutOfMemoryError.class)
            .hasMessageContaining("simulated heap exhaustion");

        GuardrailCheckFunction oomingLlm = new GuardrailCheckFunction() {

            @Override
            public Optional<Violation> apply(String text, GuardrailContext context) {
                throw new OutOfMemoryError("simulated heap exhaustion");
            }

            @Override
            public GuardrailStage stage() {
                return GuardrailStage.LLM;
            }
        };

        CheckForViolationsAdvisor llmAdvisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("oomingLlm", oomingLlm, null, null, null)
            .build();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> llmAdvisor.runChecksForTesting(request))
            .as("OutOfMemoryError in an LLM check must escape the advisor, not be aggregated")
            .isInstanceOf(OutOfMemoryError.class)
            .hasMessageContaining("simulated heap exhaustion");
    }

    /**
     * Test fixture combining {@link GuardrailCheckFunction} and {@link PreflightMasking} so the advisor's
     * {@code instanceof PreflightMasking} branch fires and the per-call {@code MaskEntityMap} is exercised.
     */
    private static final class MaskingCheck implements GuardrailCheckFunction, PreflightMasking {

        private final Set<String> observedTexts;

        MaskingCheck(Set<String> observedTexts) {
            this.observedTexts = observedTexts;
        }

        @Override
        public Optional<Violation> apply(String text, GuardrailContext context) {
            observedTexts.add(text);

            int index = text.indexOf("secret-");

            if (index < 0) {
                return Optional.empty();
            }

            return Optional.of(Violation.ofMatch("masking", text.substring(index)));
        }

        @Override
        public MaskResult mask(String text, GuardrailContext context) {
            int index = text.indexOf("secret-");

            if (index < 0) {
                return MaskResult.unchanged();
            }

            return MaskResult.entities(Map.of("SECRET", List.of(text.substring(index))));
        }
    }
}
