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

package com.bytechef.component.ai.typesafe.action;

import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.INSTRUCTIONS;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.MODEL;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.STATE;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.THRESHOLD;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.WHEN_FALSE;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.WHEN_TRUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockContextSetupExtension.class)
class TypeSafeNoulActionTest {

    private static final Map<String, Object> RESPONSE_BODY = Map.of(
        "model", "jev-1.13.0",
        "answers", Map.of("question", Map.of("type", "noul", "noul", 0.7)),
        "usage", Map.of("input_tokens", 312, "output_tokens", 48));

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(
                INSTRUCTIONS, "Does this convey urgency?", MODEL, "jev-latest",
                STATE, "Help! My payouts have been failing for 3 days.", WHEN_FALSE, "It can wait",
                WHEN_TRUE, "The customer needs help now"));

        mockHttp(mockedResponse, mockedExecutor, mockedHttp);

        Map<String, Object> result = TypeSafeNoulAction.perform(mockedInputParameters, null, mockedContext);

        assertEquals("/v1/systemone", stringArgumentCaptor.getValue());

        Body body = bodyArgumentCaptor.getValue();

        assertEquals(
            Map.of(
                "model", "jev-latest",
                "questions", Map.of(
                    "question", Map.of(
                        "type", "noul",
                        "instructions", "Does this convey urgency?",
                        "criteria", Map.of("true", "The customer needs help now", "false", "It can wait"))),
                "state", "Help! My payouts have been failing for 3 days."),
            body.getContent());
        assertEquals(
            Map.of(
                "type", "noul", "noul", 0.7, "isTrue", true, "model", "jev-1.13.0",
                "usage", Map.of("input_tokens", 312, "output_tokens", 48)),
            result);
    }

    @Test
    void testPerformWithoutCriteriaAndHigherThreshold(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        Map<String, Object> inputParameters = new HashMap<>();

        inputParameters.put(INSTRUCTIONS, "Is this a question?");
        inputParameters.put(MODEL, "jev-latest");
        inputParameters.put(STATE, Map.of("message", "What time is it?"));
        inputParameters.put(THRESHOLD, 0.8);

        Parameters mockedInputParameters = MockParametersFactory.create(inputParameters);

        mockHttp(mockedResponse, mockedExecutor, mockedHttp);

        Map<String, Object> result = TypeSafeNoulAction.perform(mockedInputParameters, null, mockedContext);

        Body body = bodyArgumentCaptor.getValue();

        assertEquals(
            Map.of(
                "model", "jev-latest",
                "questions", Map.of("question", Map.of("type", "noul", "instructions", "Is this a question?")),
                "state", Map.of("message", "What time is it?")),
            body.getContent());
        assertEquals(false, result.get("isTrue"));
    }

    @SuppressWarnings("unchecked")
    private void mockHttp(Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {
        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(RESPONSE_BODY);
    }
}
