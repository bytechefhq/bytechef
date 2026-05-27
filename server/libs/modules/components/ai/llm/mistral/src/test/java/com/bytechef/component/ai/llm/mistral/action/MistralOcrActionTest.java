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

package com.bytechef.component.ai.llm.mistral.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.mistral.constant.MistralConstants.FILE_ID;
import static com.bytechef.component.ai.llm.mistral.constant.MistralConstants.TYPE;
import static com.bytechef.component.ai.llm.mistral.constant.MistralConstants.URL;
import static com.bytechef.component.definition.Context.ContextFunction;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.llm.mistral.action.MistralOcrAction.DocumentType;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Nikolina Spehar
 */
@ExtendWith(MockContextSetupExtension.class)
class MistralOcrActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerformWithImageUrl(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(
                MODEL, "mistral-ocr-latest", TYPE, DocumentType.IMAGE_URL.getValue(),
                URL, "https://example.com/image.png"));

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(Map.of());

        Object result = MistralOcrAction.perform(mockedInputParameters, null, mockedContext);

        assertEquals(Map.of(), result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("https://api.mistral.ai/v1/ocr", stringArgumentCaptor.getValue());
        assertEquals(
            Body.of(
                MODEL, "mistral-ocr-latest",
                "document", Map.of(
                    TYPE, DocumentType.IMAGE_URL.getValue(),
                    DocumentType.IMAGE_URL.getValue(), "https://example.com/image.png")),
            bodyArgumentCaptor.getValue());
    }

    @Test
    void testPerformWithDocumentUrl(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(
                MODEL, "mistral-ocr-latest", TYPE, DocumentType.DOCUMENT_URL.getValue(),
                URL, "https://example.com/document.pdf"));

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(Map.of());

        Object result = MistralOcrAction.perform(mockedInputParameters, null, mockedContext);

        assertEquals(Map.of(), result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertNotNull(result);
        assertEquals("https://api.mistral.ai/v1/ocr", stringArgumentCaptor.getValue());
        assertEquals(
            Body.of(
                MODEL, "mistral-ocr-latest",
                "document", Map.of(
                    TYPE, DocumentType.DOCUMENT_URL.getValue(),
                    DocumentType.DOCUMENT_URL.getValue(), "https://example.com/document.pdf")),
            bodyArgumentCaptor.getValue());
    }

    @Test
    void testPerformWithFile(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(MODEL, "mistral-ocr-latest", TYPE, DocumentType.FILE.getValue(), FILE_ID, "file-abc123"));

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(Map.of());

        Object result = MistralOcrAction.perform(mockedInputParameters, null, mockedContext);

        assertEquals(Map.of(), result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertNotNull(result);
        assertEquals("https://api.mistral.ai/v1/ocr", stringArgumentCaptor.getValue());
        assertEquals(
            Body.of(
                MODEL, "mistral-ocr-latest",
                "document", Map.of(TYPE, DocumentType.FILE.getValue(), FILE_ID, "file-abc123")),
            bodyArgumentCaptor.getValue());
    }
}
