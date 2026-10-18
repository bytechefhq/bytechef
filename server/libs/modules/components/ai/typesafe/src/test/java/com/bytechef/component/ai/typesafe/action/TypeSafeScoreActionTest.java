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
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.LEVELS;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.MODEL;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.STATE;
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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockContextSetupExtension.class)
class TypeSafeScoreActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);

    @Test
    @SuppressWarnings("unchecked")
    void testPerform(Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {
        List<String> levels = List.of("Calm", "Frustrated", "Very angry");

        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(
                INSTRUCTIONS, "How frustrated is the customer?", LEVELS, levels, MODEL, "jev-latest",
                STATE, "This is the third time I am asking!"));

        Map<String, Object> probabilities = Map.of("0", 0.1, "1", 0.3, "2", 0.6);

        when(mockedHttp.post("/v1/systemone"))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of(
                    "model", "jev-1.13.0",
                    "answers", Map.of(
                        "question", Map.of(
                            "type", "score", "score", 1.5, "confidence", 0.7, "probabilities", probabilities)),
                    "usage", Map.of("input_tokens", 10, "output_tokens", 2)));

        Map<String, Object> result = TypeSafeScoreAction.perform(mockedInputParameters, null, mockedContext);

        Body body = bodyArgumentCaptor.getValue();

        assertEquals(
            Map.of(
                "model", "jev-latest",
                "questions", Map.of(
                    "question", Map.of(
                        "type", "score", "instructions", "How frustrated is the customer?", "criteria", levels)),
                "state", "This is the third time I am asking!"),
            body.getContent());
        assertEquals(
            Map.of(
                "type", "score", "score", 1.5, "confidence", 0.7, "probabilities", probabilities,
                "nearestLevel", 2, "nearestLabel", "Very angry", "model", "jev-1.13.0",
                "usage", Map.of("input_tokens", 10, "output_tokens", 2)),
            result);
    }
}
