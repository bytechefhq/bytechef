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

import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VALIDATE_INPUT;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VALIDATE_OUTPUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class SanitizeTextAdvisorValidateInputOutputTest {

    @Test
    void validateInputTrueSanitizesUserMessageBeforeModelCall() {
        // With VALIDATE_INPUT=true the sanitizer runs on the user message text BEFORE the chain is
        // called. The chain must receive the sanitized (masked) version of the user message.
        GuardrailSanitizerFunction maskSensitive = (text, context) -> text.replace("secret-key", "<REDACTED>");

        Parameters withInputSanitize = ParametersFactory.create(Map.of(VALIDATE_INPUT, true));

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("mask", maskSensitive, withInputSanitize, ParametersFactory.create(Map.of()))
            .context(mock(Context.class))
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("my secret-key is abc123");
        ChatClientResponse modelResponse = assistantResponse("got it");

        when(chain.nextCall(any(ChatClientRequest.class))).thenReturn(modelResponse);

        advisor.adviseCall(request, chain);

        ArgumentCaptor<ChatClientRequest> captor = ArgumentCaptor.forClass(ChatClientRequest.class);

        verify(chain).nextCall(captor.capture());

        String capturedUserText = captor.getValue()
            .prompt()
            .getInstructions()
            .get(0)
            .getText();

        assertThat(capturedUserText).isEqualTo("my <REDACTED> is abc123");
    }

    @Test
    void validateInputExplicitlyFalseDoesNotModifyUserMessage() {
        // Setting VALIDATE_INPUT=false explicitly opts the sanitizer out of the input pass; the chain
        // receives the original request object unchanged. (Absence of the property would default to
        // true per DEFAULT_VALIDATE_INPUT — this test pins the explicit-false opt-out specifically.)
        GuardrailSanitizerFunction maskSensitive = (text, context) -> text.replace("secret-key", "<REDACTED>");

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add(
                "mask", maskSensitive,
                ParametersFactory.create(Map.of(VALIDATE_INPUT, false)),
                ParametersFactory.create(Map.of()))
            .context(mock(Context.class))
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("my secret-key is abc123");
        ChatClientResponse modelResponse = assistantResponse("got it");

        when(chain.nextCall(request)).thenReturn(modelResponse);

        advisor.adviseCall(request, chain);

        // The original request object is forwarded unchanged (no input sanitization).
        verify(chain).nextCall(request);
    }

    @Test
    void validateInputTrueUnchangedTextForwardsOriginalRequest() {
        // When the sanitizer returns the text unchanged (no match), the original request object is
        // forwarded as-is — the advisor must not create a new request just because it ran.
        GuardrailSanitizerFunction noMatch = (text, context) -> text; // no substitution

        Parameters withInputSanitize = ParametersFactory.create(Map.of(VALIDATE_INPUT, true));

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("noop", noMatch, withInputSanitize, ParametersFactory.create(Map.of()))
            .context(mock(Context.class))
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("innocent message");
        ChatClientResponse modelResponse = assistantResponse("ok");

        when(chain.nextCall(request)).thenReturn(modelResponse);

        advisor.adviseCall(request, chain);

        verify(chain).nextCall(request);
    }

    @Test
    void validateInputTrueThrowingInputSanitizerWithholdsRequestWithoutCallingChain() {
        // When input sanitization fails (sanitizer throws) the advisor must withhold the request —
        // it must not forward unsanitized content to the model. The chain must not be called.
        GuardrailSanitizerFunction throwing = (text, context) -> {
            throw new RuntimeException("input sanitizer down");
        };

        Parameters withInputSanitize = ParametersFactory.create(Map.of(VALIDATE_INPUT, true));

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("broken", throwing, withInputSanitize, ParametersFactory.create(Map.of()))
            .context(mock(Context.class))
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("my secret is abc");

        ChatClientResponse response = advisor.adviseCall(request, chain);

        assertThat(response.chatResponse()
            .getResult()
            .getOutput()
            .getText()).isEqualTo("[sanitizer failed — response withheld]");
        verify(chain, never()).nextCall(any());
    }

    @Test
    void validateInputTrueSelectivelyRunsOnlyFlaggedSanitizers() {
        // Only the sanitizer with VALIDATE_INPUT=true runs on input; the sibling with VALIDATE_INPUT=false
        // is skipped. The chain receives the partially-sanitized user message.
        GuardrailSanitizerFunction maskA = (text, context) -> text.replace("alpha", "<A>");
        GuardrailSanitizerFunction maskB = (text, context) -> text.replace("beta", "<B>"); // must NOT run on input

        Parameters withInput = ParametersFactory.create(Map.of(VALIDATE_INPUT, true));
        Parameters noInput = ParametersFactory.create(Map.of(VALIDATE_INPUT, false)); // explicit opt-out

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("maskA", maskA, withInput, ParametersFactory.create(Map.of()))
            .add("maskB", maskB, noInput, ParametersFactory.create(Map.of()))
            .context(mock(Context.class))
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("alpha and beta secrets");
        ChatClientResponse modelResponse = assistantResponse("received");

        when(chain.nextCall(any(ChatClientRequest.class))).thenReturn(modelResponse);

        advisor.adviseCall(request, chain);

        ArgumentCaptor<ChatClientRequest> captor = ArgumentCaptor.forClass(ChatClientRequest.class);

        verify(chain).nextCall(captor.capture());

        String capturedUserText = captor.getValue()
            .prompt()
            .getInstructions()
            .get(0)
            .getText();

        // maskA runs on input → "alpha" masked; maskB skipped on input → "beta" remains.
        assertThat(capturedUserText).isEqualTo("<A> and beta secrets");
    }

    // ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────
    // VALIDATE_OUTPUT = false — opt-out of output sanitization
    // ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void validateOutputFalseSkipsOutputSanitization() {
        // A sanitizer with VALIDATE_OUTPUT=false must not alter the model response. The original text
        // passes through unchanged even though the sanitizer would normally mask it.
        GuardrailSanitizerFunction maskSensitive = (text, context) -> text.replace("sensitive", "<REDACTED>");

        Parameters noOutputSanitize = ParametersFactory.create(Map.of(VALIDATE_OUTPUT, false));

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("mask", maskSensitive, noOutputSanitize, ParametersFactory.create(Map.of()))
            .context(mock(Context.class))
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("query");
        ChatClientResponse modelResponse = assistantResponse("here is your sensitive data");

        when(chain.nextCall(request)).thenReturn(modelResponse);

        ChatClientResponse response = advisor.adviseCall(request, chain);

        assertThat(response.chatResponse()
            .getResult()
            .getOutput()
            .getText()).isEqualTo("here is your sensitive data");
    }

    @Test
    void validateOutputFalseOnAllSanitizersPassesResponseThroughUnmodified() {
        // When every sanitizer has VALIDATE_OUTPUT=false the response is never touched.
        GuardrailSanitizerFunction maskA = (text, context) -> text.replace("x", "<X>");
        GuardrailSanitizerFunction maskB = (text, context) -> text.replace("y", "<Y>");

        Parameters noOutput = ParametersFactory.create(Map.of(VALIDATE_OUTPUT, false));

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("a", maskA, noOutput, ParametersFactory.create(Map.of()))
            .add("b", maskB, noOutput, ParametersFactory.create(Map.of()))
            .context(mock(Context.class))
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        // Use a user message that contains neither "x" nor "y" so the input-sanitize pass (validateInput defaults to
        // true) is a no-op and the chain receives the unmodified request. Otherwise the mock's exact-request matcher
        // would miss and chain.nextCall would return null.
        ChatClientRequest request = requestWithUser("question");
        ChatClientResponse modelResponse = assistantResponse("x and y data");

        when(chain.nextCall(request)).thenReturn(modelResponse);

        ChatClientResponse response = advisor.adviseCall(request, chain);

        assertThat(response.chatResponse()
            .getResult()
            .getOutput()
            .getText()).isEqualTo("x and y data");
    }

    @Test
    void validateOutputTrueDefaultRunsOutputSanitization() {
        // VALIDATE_OUTPUT has fallback=true, so a sanitizer with empty parameters (no explicit flag)
        // still sanitizes the model response — this preserves the historical always-sanitize-output
        // behaviour and guards against a regression that breaks existing output sanitization.
        GuardrailSanitizerFunction maskSensitive = (text, context) -> text.replace("sensitive", "<REDACTED>");

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("mask", maskSensitive, ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()))
            .context(mock(Context.class))
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("query");

        when(chain.nextCall(request)).thenReturn(assistantResponse("here is your sensitive data"));

        ChatClientResponse response = advisor.adviseCall(request, chain);

        assertThat(response.chatResponse()
            .getResult()
            .getOutput()
            .getText()).isEqualTo("here is your <REDACTED> data");
    }

    @Test
    void validateOutputFalseSelectivelySkipsOneOutputSanitizerWhileSiblingRuns() {
        // Per-sanitizer granularity: a sanitizer with VALIDATE_OUTPUT=false is skipped on output;
        // the sibling with empty params (fallback=true) still runs and masks its target.
        GuardrailSanitizerFunction maskA = (text, context) -> text.replace("alpha", "<A>");
        GuardrailSanitizerFunction maskB = (text, context) -> text.replace("beta", "<B>");

        Parameters noOutput = ParametersFactory.create(Map.of(VALIDATE_OUTPUT, false));
        Parameters defaultOutput = ParametersFactory.create(Map.of()); // fallback=true → runs on output

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("skipA", maskA, noOutput, ParametersFactory.create(Map.of()))
            .add("runB", maskB, defaultOutput, ParametersFactory.create(Map.of()))
            .context(mock(Context.class))
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("query");

        when(chain.nextCall(request)).thenReturn(assistantResponse("alpha and beta secrets"));

        ChatClientResponse response = advisor.adviseCall(request, chain);

        // maskA skipped (VALIDATE_OUTPUT=false) → "alpha" unchanged; maskB runs → "beta" masked.
        assertThat(response.chatResponse()
            .getResult()
            .getOutput()
            .getText()).isEqualTo("alpha and <B> secrets");
    }

    @Test
    void validateInputTrueAndOutputTrueSanitizesBothDirections() {
        // With both flags true the sanitizer runs on user input first (masking before the model sees
        // it) and then on the model response (masking before the caller sees it).
        GuardrailSanitizerFunction mask = (text, context) -> text.replace("secret", "<REDACTED>");

        Parameters both = ParametersFactory.create(Map.of(VALIDATE_INPUT, true, VALIDATE_OUTPUT, true));

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("mask", mask, both, ParametersFactory.create(Map.of()))
            .context(mock(Context.class))
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("my secret password");

        // The model echoes both the sanitized input and adds its own secret.
        when(chain.nextCall(any(ChatClientRequest.class)))
            .thenAnswer(invocation -> {
                ChatClientRequest received = invocation.getArgument(0);

                String receivedText = received.prompt()
                    .getInstructions()
                    .get(0)
                    .getText();

                return assistantResponse("you said: " + receivedText + " and another secret");
            });

        ChatClientResponse response = advisor.adviseCall(request, chain);

        // Input was sanitized before chain: model received "my <REDACTED> password"
        // Response was sanitized after chain: "secret" in model response also masked
        assertThat(response.chatResponse()
            .getResult()
            .getOutput()
            .getText()).isEqualTo("you said: my <REDACTED> password and another <REDACTED>");
    }

    @Test
    void validateInputTrueStreamSanitizesRequestBeforeStreaming() {
        // On the streaming path, input sanitization runs before nextStream is called. The stream
        // chain receives the sanitized request, not the original.
        GuardrailSanitizerFunction mask = (text, context) -> text.replace("secret", "<REDACTED>");

        Parameters withInput = ParametersFactory.create(Map.of(VALIDATE_INPUT, true));

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("mask", mask, withInput, ParametersFactory.create(Map.of()))
            .context(mock(Context.class))
            .build();

        StreamAdvisorChain streamChain = mock(StreamAdvisorChain.class);
        ChatClientRequest request = requestWithUser("my secret here");

        when(streamChain.nextStream(any(ChatClientRequest.class)))
            .thenReturn(Flux.just(assistantResponse("ok")));

        advisor.adviseStream(request, streamChain)
            .collectList()
            .block();

        ArgumentCaptor<ChatClientRequest> captor = ArgumentCaptor.forClass(ChatClientRequest.class);

        verify(streamChain).nextStream(captor.capture());

        String capturedUserText = captor.getValue()
            .prompt()
            .getInstructions()
            .get(0)
            .getText();

        assertThat(capturedUserText).isEqualTo("my <REDACTED> here");
    }

    @Test
    void validateInputTrueStreamThrowingInputSanitizerWithholdsWithoutStreaming() {
        // When input sanitization fails on the streaming path, a withheld response is emitted and
        // the stream chain is never started.
        GuardrailSanitizerFunction throwing = (text, context) -> {
            throw new RuntimeException("input classifier down");
        };

        Parameters withInput = ParametersFactory.create(Map.of(VALIDATE_INPUT, true));

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("broken", throwing, withInput, ParametersFactory.create(Map.of()))
            .context(mock(Context.class))
            .build();

        StreamAdvisorChain streamChain = mock(StreamAdvisorChain.class);

        List<ChatClientResponse> responses = advisor
            .adviseStream(requestWithUser("my secret"), streamChain)
            .collectList()
            .block();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0)
            .chatResponse()
            .getResult()
            .getOutput()
            .getText()).isEqualTo("[sanitizer failed — response withheld]");
        verify(streamChain, never()).nextStream(any());
    }

    private static ChatClientRequest requestWithUser(String text) {
        return ChatClientRequest.builder()
            .prompt(new Prompt(List.<Message>of(new UserMessage(text))))
            .build();
    }

    private static ChatClientResponse assistantResponse(String text) {
        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage(text))))
            .build();

        return ChatClientResponse.builder()
            .chatResponse(chatResponse)
            .build();
    }
}
