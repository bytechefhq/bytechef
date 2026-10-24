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

import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.SANITIZE_WITHHELD_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRecord;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRecord.Status;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRecorder;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRef;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallScope;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallTarget;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.MaskResult;
import com.bytechef.platform.component.definition.ai.agent.guardrails.PreflightMasking;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class SanitizeTextAdvisorOutcomeRecordsTest {

    private static final long JOB_ID = 1L;
    private static final long TASK_EXECUTION_ID = 2L;
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
        .build();
    private static final Parameters EMPTY = ParametersFactory.create(Map.of());
    private static final Parameters OUTPUT_ONLY = ParametersFactory.create(
        Map.of("validateInput", false, "validateOutput", true));

    private final ClusterElementCallRecorder recorder = new ClusterElementCallRecorder();

    @Test
    void testInputRedactionRecordsFrameAndSanitizer() {
        GuardrailSanitizerFunction pii = masking(
            (text, context) -> text.contains("a@b.c")
                ? MaskResult.entities(Map.of("EMAIL", List.of("a@b.c"), "PHONE", List.of("555-1234")))
                : MaskResult.unchanged());

        SanitizeTextAdvisor advisor = builderWithTarget()
            .add(element("pii_1", "pii/v1/pii"), pii, EMPTY, EMPTY, EMPTY, EMPTY, Map.of(), null)
            .build();

        ClusterElementCallRef agentRef = new ClusterElementCallRef("agent-call", "aiAgent_1");
        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        when(chain.nextCall(any())).thenReturn(chainResponse("fine"));

        ClusterElementCallScope.callWith(
            agentRef, () -> advisor.adviseCall(request("mail a@b.c or call 555-1234"), chain));

        List<ClusterElementCallRecord> records = drainRecords();

        assertThat(records).hasSize(2);

        ClusterElementCallRecord frame = records.get(0);
        ClusterElementCallRecord sanitizer = records.get(1);

        assertThat(frame.workflowNodeName()).isEqualTo("sanitizeText_1");
        assertThat(frame.componentName()).isEqualTo("sanitizeText");
        assertThat(frame.status()).isEqualTo(Status.REDACTED);
        assertThat(frame.parentId()).isEqualTo("agent-call");
        assertThat(parse(frame.output())).isEqualTo(Map.of("side", "INPUT", "maskCount", 2));

        assertThat(sanitizer.workflowNodeName()).isEqualTo("pii_1");
        assertThat(sanitizer.componentName()).isEqualTo("pii");
        assertThat(sanitizer.status()).isEqualTo(Status.REDACTED);
        assertThat(sanitizer.parentId()).isEqualTo(frame.id());
        assertThat(parse(sanitizer.output())).isEqualTo(Map.of("side", "INPUT", "maskCount", 2));
    }

    @Test
    void testOutputRedactionRecordsSideOutput() {
        GuardrailSanitizerFunction pii = masking(
            (text, context) -> text.contains("a@b.c")
                ? MaskResult.entities(Map.of("EMAIL", List.of("a@b.c")))
                : MaskResult.unchanged());

        SanitizeTextAdvisor advisor = builderWithTarget()
            .add(element("pii_1", "pii/v1/pii"), pii, OUTPUT_ONLY, EMPTY, EMPTY, EMPTY, Map.of(), null)
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        when(chain.nextCall(any())).thenReturn(chainResponse("write to a@b.c"));

        ChatClientResponse response = advisor.adviseCall(request("hello"), chain);

        assertThat(outputText(response)).doesNotContain("a@b.c");

        List<ClusterElementCallRecord> records = drainRecords();

        assertThat(records).hasSize(2);
        assertThat(parse(records.get(0)
            .output())).isEqualTo(Map.of("side", "OUTPUT", "maskCount", 1));
        assertThat(parse(records.get(1)
            .output())).isEqualTo(Map.of("side", "OUTPUT", "maskCount", 1));
    }

    @Test
    void testUnchangedSanitizerRecordsNothing() {
        GuardrailSanitizerFunction pii = masking((text, context) -> MaskResult.unchanged());
        GuardrailSanitizerFunction identity = (text, context) -> text;

        SanitizeTextAdvisor advisor = builderWithTarget()
            .add(element("pii_1", "pii/v1/pii"), pii, EMPTY, EMPTY, EMPTY, EMPTY, Map.of(), null)
            .add(element("custom_1", "custom/v1/custom"), identity, EMPTY, EMPTY, EMPTY, EMPTY, Map.of(), null)
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        when(chain.nextCall(any())).thenReturn(chainResponse("fine"));

        ChatClientResponse response = advisor.adviseCall(request("hello"), chain);

        assertThat(outputText(response)).isEqualTo("fine");
        assertThat(drainRecords()).isEmpty();
    }

    @Test
    void testMaskedResultWithoutCountRecordsNoMaskCount() {
        GuardrailSanitizerFunction masked = masking(
            (text, context) -> MaskResult.masked(text.replace("secret", "******"), text));
        GuardrailSanitizerFunction identity = (text, context) -> text;

        SanitizeTextAdvisor advisor = builderWithTarget()
            .add(element("secretKeys_1", "secretKeys/v1/secretKeys"), masked, EMPTY, EMPTY, EMPTY, EMPTY, Map.of(),
                null)
            .add(element("custom_1", "custom/v1/custom"), identity, EMPTY, EMPTY, EMPTY, EMPTY, Map.of(), null)
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        when(chain.nextCall(any())).thenReturn(chainResponse("fine"));

        advisor.adviseCall(request("my secret is here"), chain);

        List<ClusterElementCallRecord> records = drainRecords();

        assertThat(records).hasSize(2);
        assertThat(parse(records.get(0)
            .output())).isEqualTo(Map.of("side", "INPUT"));
        assertThat(records.get(1)
            .workflowNodeName()).isEqualTo("secretKeys_1");
        assertThat(parse(records.get(1)
            .output())).isEqualTo(Map.of("side", "INPUT"));
    }

    @Test
    void testSanitizerCrashRecordsWithheldFrameAndFailedSanitizer() {
        GuardrailSanitizerFunction crashing = (text, context) -> {
            throw new IllegalStateException("boom");
        };

        SanitizeTextAdvisor advisor = builderWithTarget()
            .add(element("custom_1", "custom/v1/custom"), crashing, EMPTY, EMPTY, EMPTY, EMPTY, Map.of(), null)
            .build();

        ChatClientResponse response = advisor.adviseCall(request("hello"), mock(CallAdvisorChain.class));

        assertThat(isWithheld(response)).isTrue();

        List<ClusterElementCallRecord> records = drainRecords();

        assertThat(records).hasSize(2);

        ClusterElementCallRecord frame = records.get(0);
        ClusterElementCallRecord sanitizer = records.get(1);

        assertThat(frame.workflowNodeName()).isEqualTo("sanitizeText_1");
        assertThat(frame.status()).isEqualTo(Status.WITHHELD);
        assertThat(parse(frame.output())).isEqualTo(Map.of("side", "INPUT"));

        assertThat(sanitizer.workflowNodeName()).isEqualTo("custom_1");
        assertThat(sanitizer.componentName()).isEqualTo("custom");
        assertThat(sanitizer.status()).isEqualTo(Status.FAILED);
        assertThat(sanitizer.parentId()).isEqualTo(frame.id());
        assertThat(sanitizer.output()).isNull();
        assertThat(sanitizer.error()).isEqualTo("IllegalStateException");
    }

    @Test
    void testSanitizerCrashErrorOmitsTheExceptionMessage() {
        GuardrailSanitizerFunction crashing = (text, context) -> {
            throw new IllegalStateException("leaked-user-text");
        };

        SanitizeTextAdvisor advisor = builderWithTarget()
            .add(element("custom_1", "custom/v1/custom"), crashing, EMPTY, EMPTY, EMPTY, EMPTY, Map.of(), null)
            .build();

        advisor.adviseCall(request("leaked-user-text"), mock(CallAdvisorChain.class));

        List<ClusterElementCallRecord> records = drainRecords();

        assertThat(records)
            .filteredOn(callRecord -> callRecord.status() == Status.FAILED)
            .singleElement()
            .satisfies(callRecord -> assertThat(callRecord.error())
                .isEqualTo("IllegalStateException")
                .doesNotContain("leaked-user-text"));
    }

    @Test
    void testOutputCrashRecordsWithheldFrameWithSideOutput() {
        GuardrailSanitizerFunction crashing = (text, context) -> {
            throw new IllegalStateException("boom");
        };

        SanitizeTextAdvisor advisor = builderWithTarget()
            .add(element("custom_1", "custom/v1/custom"), crashing, OUTPUT_ONLY, EMPTY, EMPTY, EMPTY, Map.of(), null)
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        when(chain.nextCall(any())).thenReturn(chainResponse("reply"));

        ChatClientResponse response = advisor.adviseCall(request("hello"), chain);

        assertThat(isWithheld(response)).isTrue();

        List<ClusterElementCallRecord> records = drainRecords();

        assertThat(records).hasSize(2);
        assertThat(records.get(0)
            .status()).isEqualTo(Status.WITHHELD);
        assertThat(parse(records.get(0)
            .output())).isEqualTo(Map.of("side", "OUTPUT"));
        assertThat(records.get(1)
            .status()).isEqualTo(Status.FAILED);
    }

    @Test
    void testStreamRedactionsRecordOneFrameForTheWholeStream() {
        GuardrailSanitizerFunction pii = masking(
            (text, context) -> text.contains("a@b.c")
                ? MaskResult.entities(Map.of("EMAIL", List.of("a@b.c")))
                : MaskResult.unchanged());

        SanitizeTextAdvisor advisor = builderWithTarget()
            .add(element("pii_1", "pii/v1/pii"), pii, OUTPUT_ONLY, EMPTY, EMPTY, EMPTY, Map.of(), null)
            .build();

        StreamAdvisorChain chain = mock(StreamAdvisorChain.class);

        when(chain.nextStream(any())).thenReturn(
            Flux.just(chainResponse("first a@b.c"), chainResponse("plain"), chainResponse("again a@b.c")));

        List<ChatClientResponse> responses = advisor.adviseStream(request("hello"), chain)
            .collectList()
            .block();

        assertThat(responses).hasSize(3);

        List<ClusterElementCallRecord> records = drainRecords();

        assertThat(records).hasSize(2);
        assertThat(records.get(0)
            .status()).isEqualTo(Status.REDACTED);
        assertThat(parse(records.get(0)
            .output())).isEqualTo(Map.of("side", "OUTPUT", "maskCount", 2));
        assertThat(parse(records.get(1)
            .output())).isEqualTo(Map.of("side", "OUTPUT", "maskCount", 2));
    }

    @Test
    void testRecordsNeverContainMaskedValues() {
        GuardrailSanitizerFunction pii = masking(
            (text, context) -> text.contains("a@b.c")
                ? MaskResult.entities(Map.of("EMAIL", List.of("a@b.c")))
                : MaskResult.unchanged());
        GuardrailSanitizerFunction rewriting = (text, context) -> text.replace("a@b.c", "[EMAIL]");

        SanitizeTextAdvisor advisor = builderWithTarget()
            .add(element("pii_1", "pii/v1/pii"), pii, EMPTY, EMPTY, EMPTY, EMPTY, Map.of(), null)
            .add(element("custom_1", "custom/v1/custom"), rewriting, EMPTY, EMPTY, EMPTY, EMPTY, Map.of(), null)
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        when(chain.nextCall(any())).thenReturn(chainResponse("fine"));

        advisor.adviseCall(request("mail me at a@b.c"), chain);

        GuardrailSanitizerFunction crashing = (text, context) -> {
            throw new IllegalStateException("cannot mask a@b.c");
        };

        SanitizeTextAdvisor crashingAdvisor = builderWithTarget()
            .add(element("custom_2", "custom/v1/custom"), crashing, EMPTY, EMPTY, EMPTY, EMPTY, Map.of(), null)
            .build();

        crashingAdvisor.adviseCall(request("mail me at a@b.c"), mock(CallAdvisorChain.class));

        List<ClusterElementCallRecord> records = drainRecords();

        assertThat(records).hasSize(5);
        assertThat(records).allSatisfy(callRecord -> {
            assertThat(Objects.toString(callRecord.output(), "")).doesNotContain("a@b.c");
            assertThat(Objects.toString(callRecord.error(), "")).doesNotContain("a@b.c");
            assertThat(callRecord.input()).isNull();
        });
    }

    @Test
    void testNoTargetRecordsNothing() {
        GuardrailSanitizerFunction pii = masking(
            (text, context) -> text.contains("a@b.c")
                ? MaskResult.entities(Map.of("EMAIL", List.of("a@b.c")))
                : MaskResult.unchanged());

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .clusterElement("sanitizeText_1", "sanitizeText")
            .add(element("pii_1", "pii/v1/pii"), pii, EMPTY, EMPTY, EMPTY, EMPTY, Map.of(), null)
            .context(mock(Context.class))
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        when(chain.nextCall(any())).thenReturn(chainResponse("fine"));

        advisor.adviseCall(request("mail me at a@b.c"), chain);

        assertThat(drainRecords()).isEmpty();
        assertThat(ClusterElementCallRecorder.getInstance()
            .drain(TASK_EXECUTION_ID)
            .records()).isEmpty();
    }

    @Test
    void testSanitizerWithoutNodeNameRecordsOnlyTheFrame() {
        GuardrailSanitizerFunction pii = masking(
            (text, context) -> text.contains("a@b.c")
                ? MaskResult.entities(Map.of("EMAIL", List.of("a@b.c")))
                : MaskResult.unchanged());

        SanitizeTextAdvisor advisor = builderWithTarget()
            .add("pii", pii, EMPTY, EMPTY)
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        when(chain.nextCall(any())).thenReturn(chainResponse("fine"));

        advisor.adviseCall(request("mail me at a@b.c"), chain);

        List<ClusterElementCallRecord> records = drainRecords();

        assertThat(records).hasSize(1);
        assertThat(records.get(0)
            .workflowNodeName()).isEqualTo("sanitizeText_1");
        assertThat(parse(records.get(0)
            .output())).isEqualTo(Map.of("side", "INPUT", "maskCount", 1));
    }

    private SanitizeTextAdvisor.Builder builderWithTarget() {
        return SanitizeTextAdvisor.builder()
            .clusterElement("sanitizeText_1", "sanitizeText")
            .callTarget(new ClusterElementCallTarget(recorder, JOB_ID, TASK_EXECUTION_ID))
            .context(mock(Context.class));
    }

    private List<ClusterElementCallRecord> drainRecords() {
        return recorder.drain(TASK_EXECUTION_ID)
            .records();
    }

    private static ClusterElement element(String workflowNodeName, String type) {
        return new ClusterElement(null, null, Map.of(), null, type, Map.of(), workflowNodeName);
    }

    private static GuardrailSanitizerFunction masking(BiFunction<String, GuardrailContext, MaskResult> maskFunction) {
        return new MaskingSanitizer() {

            @Override
            public String apply(String text, GuardrailContext context) {
                return text;
            }

            @Override
            public MaskResult mask(String text, GuardrailContext context) {
                return maskFunction.apply(text, context);
            }
        };
    }

    private static Map<String, Object> parse(String json) {
        return JSON_MAPPER.readValue(json, new TypeReference<>() {});
    }

    private static ChatClientRequest request(String userText) {
        return ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage(userText)))
            .build();
    }

    private static ChatClientResponse chainResponse(String text) {
        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage(text))))
            .build();

        return ChatClientResponse.builder()
            .chatResponse(chatResponse)
            .build();
    }

    private static boolean isWithheld(ChatClientResponse response) {
        ChatResponse chatResponse = Objects.requireNonNull(response.chatResponse());

        return chatResponse.getMetadata()
            .containsKey(SANITIZE_WITHHELD_KEY);
    }

    private static String outputText(ChatClientResponse response) {
        ChatResponse chatResponse = Objects.requireNonNull(response.chatResponse());

        Generation generation = Objects.requireNonNull(chatResponse.getResult());

        return generation.getOutput()
            .getText();
    }

    private interface MaskingSanitizer extends GuardrailSanitizerFunction, PreflightMasking {
    }
}
