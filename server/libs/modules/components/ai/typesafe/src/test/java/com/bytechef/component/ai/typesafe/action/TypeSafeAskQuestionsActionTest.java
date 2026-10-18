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

import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.MODEL;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.QUESTIONS;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.STATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockContextSetupExtension.class)
class TypeSafeAskQuestionsActionTest {

    private static final String STATE_TEXT = "Help! My payouts have been failing for 3 days.";

    private static final Map<String, Object> RESPONSE_BODY = Map.of(
        "model", "jev-1.13.0",
        "answers", Map.of(
            "is_urgent", Map.of("type", "noul", "noul", 0.95),
            "department", Map.of(
                "type", "choice", "choice", "billing", "probabilities", Map.of("billing", 0.9, "technical", 0.1),
                "confidence", 0.82),
            "frustration", Map.of(
                "type", "score", "score", 1.1, "probabilities", Map.of("0", 0.1, "1", 0.7, "2", 0.2),
                "confidence", 0.6)),
        "usage", Map.of("input_tokens", 410, "output_tokens", 73));

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(
                MODEL, "jev-latest", STATE, STATE_TEXT,
                QUESTIONS, List.of(
                    Map.of(
                        "name", "is_urgent", "type", "noul", "instructions", "Does this convey urgency?",
                        "whenTrue", "The customer needs help now", "threshold", 0.9),
                    Map.of(
                        "name", "department", "type", "choice", "instructions", "Which team should handle this?",
                        "options", List.of(
                            Map.of("label", "billing", "description", "Payments and payouts"),
                            Map.of("label", "technical"))),
                    Map.of(
                        "name", "frustration", "type", "score", "instructions", "How frustrated is the customer?",
                        "levels", List.of("Calm", "Frustrated", "Very angry")))));

        mockHttp(mockedResponse, mockedExecutor, mockedHttp);

        Map<String, Object> result = TypeSafeAskQuestionsAction.perform(mockedInputParameters, null, mockedContext);

        assertEquals("/v1/systemone", stringArgumentCaptor.getValue());

        Body body = bodyArgumentCaptor.getValue();

        Map<String, Object> departmentCriteria = new HashMap<>();

        departmentCriteria.put("billing", "Payments and payouts");
        departmentCriteria.put("technical", null);

        assertEquals(
            Map.of(
                "model", "jev-latest",
                "questions", Map.of(
                    "is_urgent", Map.of(
                        "type", "noul", "instructions", "Does this convey urgency?",
                        "criteria", Map.of("true", "The customer needs help now")),
                    "department", Map.of(
                        "type", "choice", "instructions", "Which team should handle this?",
                        "criteria", departmentCriteria),
                    "frustration", Map.of(
                        "type", "score", "instructions", "How frustrated is the customer?",
                        "criteria", List.of("Calm", "Frustrated", "Very angry"))),
                "state", STATE_TEXT),
            body.getContent());

        assertEquals(
            Map.of(
                "answers", Map.of(
                    "is_urgent", Map.of("type", "noul", "noul", 0.95, "isTrue", true),
                    "department", Map.of(
                        "type", "choice", "choice", "billing",
                        "probabilities", Map.of("billing", 0.9, "technical", 0.1), "confidence", 0.82),
                    "frustration", Map.of(
                        "type", "score", "score", 1.1, "probabilities", Map.of("0", 0.1, "1", 0.7, "2", 0.2),
                        "confidence", 0.6, "nearestLevel", 1, "nearestLabel", "Frustrated")),
                "model", "jev-1.13.0",
                "usage", Map.of("input_tokens", 410, "output_tokens", 73)),
            result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPerformReadsThresholdGivenAsText(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(
                MODEL, "jev-latest", STATE, STATE_TEXT,
                QUESTIONS, List.of(
                    Map.of(
                        "name", "is_urgent", "type", "noul", "instructions", "Does this convey urgency?",
                        "threshold", "0.99"))));

        mockHttp(mockedResponse, mockedExecutor, mockedHttp);

        Map<String, Object> result = TypeSafeAskQuestionsAction.perform(mockedInputParameters, null, mockedContext);

        Map<String, Map<String, Object>> answers = (Map<String, Map<String, Object>>) result.get("answers");

        assertEquals(false, answers.get("is_urgent")
            .get("isTrue"));
    }

    @Test
    void testPerformRejectsThresholdThatIsNotANumber(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(
                MODEL, "jev-latest", STATE, STATE_TEXT,
                QUESTIONS, List.of(
                    Map.of(
                        "name", "is_urgent", "type", "noul", "instructions", "Does this convey urgency?",
                        "threshold", "high"))));

        mockHttp(mockedResponse, mockedExecutor, mockedHttp);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> TypeSafeAskQuestionsAction.perform(mockedInputParameters, null, mockedContext));

        assertEquals("Question 'is_urgent' has a threshold that is not a number: 'high'", exception.getMessage());
    }

    @Test
    void testPerformRejectsDuplicateNames(Context mockedContext) {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(
                MODEL, "jev-latest", STATE, STATE_TEXT,
                QUESTIONS, List.of(
                    Map.of("name", "is_urgent", "type", "noul", "instructions", "Is it urgent?"),
                    Map.of("name", "is_urgent", "type", "noul", "instructions", "Is it really urgent?"))));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> TypeSafeAskQuestionsAction.perform(mockedInputParameters, null, mockedContext));

        assertEquals("Question names must be unique, found 'is_urgent' twice", exception.getMessage());
    }

    @Test
    void testPerformRejectsUnknownType(Context mockedContext) {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(
                MODEL, "jev-latest", STATE, STATE_TEXT,
                QUESTIONS, List.of(Map.of("name", "tone", "type", "rank", "instructions", "Rank the tone"))));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> TypeSafeAskQuestionsAction.perform(mockedInputParameters, null, mockedContext));

        assertEquals("Question 'tone' has unknown type 'rank'", exception.getMessage());
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
