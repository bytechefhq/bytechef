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

package com.bytechef.component.gamma.action;

import static com.bytechef.component.gamma.constant.GammaConstants.ADDITIONAL_INSTRUCTIONS;
import static com.bytechef.component.gamma.constant.GammaConstants.EXPORT_AS;
import static com.bytechef.component.gamma.constant.GammaConstants.FORMAT;
import static com.bytechef.component.gamma.constant.GammaConstants.GENERATION_ID;
import static com.bytechef.component.gamma.constant.GammaConstants.INPUT_TEXT;
import static com.bytechef.component.gamma.constant.GammaConstants.NUM_CARDS;
import static com.bytechef.component.gamma.constant.GammaConstants.TEXT_MODE;
import static com.bytechef.component.gamma.constant.GammaConstants.THEME_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
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
class GammaGeneratePresentationActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final Map<String, Object> responseMap = Map.of(GENERATION_ID, "generation1");
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Map<String, Object> parameterMap = new HashMap<>();

        parameterMap.put(INPUT_TEXT, "Renewable energy");
        parameterMap.put(TEXT_MODE, "generate");
        parameterMap.put(FORMAT, "presentation");
        parameterMap.put(THEME_ID, "theme1");
        parameterMap.put(NUM_CARDS, 8);
        parameterMap.put(ADDITIONAL_INSTRUCTIONS, "Keep it short");
        parameterMap.put(EXPORT_AS, "pdf");

        Parameters mockedParameters = MockParametersFactory.create(parameterMap);

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        Map<String, Object> result = GammaGeneratePresentationAction.perform(mockedParameters, null, mockedContext);

        assertEquals(responseMap, result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/generations", stringArgumentCaptor.getValue());

        Body body = bodyArgumentCaptor.getValue();

        assertEquals(
            Map.of(
                INPUT_TEXT, "Renewable energy",
                TEXT_MODE, "generate",
                FORMAT, "presentation",
                THEME_ID, "theme1",
                NUM_CARDS, 8,
                ADDITIONAL_INSTRUCTIONS, "Keep it short",
                EXPORT_AS, "pdf"),
            body.getContent());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        assertEquals(ResponseType.JSON, configurationBuilder.build()
            .getResponseType());
    }

    @Test
    void testPerformOmitsEmptyOptionalFields(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(INPUT_TEXT, "Renewable energy", TEXT_MODE, "condense"));

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        GammaGeneratePresentationAction.perform(mockedParameters, null, mockedContext);

        Body body = bodyArgumentCaptor.getValue();

        assertEquals(Map.of(INPUT_TEXT, "Renewable energy", TEXT_MODE, "condense"), body.getContent());
    }
}
