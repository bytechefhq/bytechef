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

package com.bytechef.component.google.forms.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.FORM_ID;
import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.NEXT_PAGE_TOKEN;
import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.RESPONDENT_EMAIL;
import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.RESPONSE_ID;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.google.forms.util.GoogleFormsUtils.FormFileUploadAnswer;
import com.bytechef.component.google.forms.util.GoogleFormsUtils.FormTextAnswer;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
@ExtendWith(MockContextSetupExtension.class)
class GoogleFormUtilsTest {

    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(FORM_ID, "123"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<Object[]> objectsArgumentCaptor = forClass(Object[].class);

    @Test
    @SuppressWarnings("unchecked")
    void testCreateCustomResponse(
        Context mockedContext, Http.Executor mockedExecutor, Http.Response mockedResponse, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Map<String, Object> form = Map.of(
            "items", List.of(
                Map.of("title", "Your name", "questionItem", Map.of("question", Map.of("questionId", "q1"))),
                Map.of("title", "Upload file", "questionItem", Map.of("question", Map.of("questionId", "q2")))));

        Map<String, Object> response = Map.of(
            "responseId", "resp-1",
            "respondentEmail", "alice@example.com",
            "answers", Map.of(
                "q1", Map.of(
                    "textAnswers", Map.of(
                        "answers", List.of(Map.of("value", "Alice"), Map.of("value", "A.")))),
                "q2", Map.of(
                    "fileUploadAnswers", Map.of(
                        "answers", List.of(Map.of("fileId", "file-123", "fileName", "cv.pdf"))))));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(form);

        Map<String, Object> result = GoogleFormsUtils.createCustomResponse(mockedContext, "form-123", response);

        Map<String, Object> expectedResponse = Map.of(
            FORM_ID, "form-123", RESPONSE_ID, "resp-1", RESPONDENT_EMAIL, "alice@example.com",
            "question_1", new FormTextAnswer("q1", "Your name", List.of("Alice", "A.")),
            "question_2", new FormFileUploadAnswer("q2", "Upload file", "file-123", "cv.pdf"));

        assertEquals(expectedResponse, result);

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();
        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals("/forms/form-123", stringArgumentCaptor.getValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetResponseIdOptions(
        Context mockedContext, Http.Executor mockedExecutor, Http.Response mockedResponse, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Map<String, Object> page1 = Map.of("responses", List.of(Map.of("responseId", "r1")), "nextPageToken", "t1");

        Map<String, Object> page2 = Map.of(
            "responses", List.of(Map.of("responseId", "r2", "respondentEmail", "user@example.com")));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(page1, page2);

        List<Option<String>> result = GoogleFormsUtils.getResponseIdOptions(
            mockedParameters, null, null, null, mockedContext);

        assertEquals(List.of(option("r1", "r1"), option("user@example.com (r2)", "r2")), result);

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        Http.Configuration configuration = configurationBuilder.build();

        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals(List.of("/forms/123/responses", "/forms/123/responses"), stringArgumentCaptor.getAllValues());

        List<Object[]> objectsArgumentCaptorAllValues = objectsArgumentCaptor.getAllValues();

        assertEquals(2, objectsArgumentCaptorAllValues.size());

        Object[] queryParameters1 = {
            NEXT_PAGE_TOKEN, null
        };
        Object[] queryParameters2 = {
            NEXT_PAGE_TOKEN, "t1"
        };

        assertArrayEquals(queryParameters1, objectsArgumentCaptorAllValues.get(0));
        assertArrayEquals(queryParameters2, objectsArgumentCaptorAllValues.get(1));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetCustomResponses(
        Context mockedContext, Http.Executor mockedExecutor, Http.Response mockedResponse, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {
        Map<String, Object> page1 = Map.of(
            "responses", List.of(
                Map.of("responseId", "newer", "respondentEmail", "n@example.com", "createTime",
                    "2025-01-02T10:00:00Z")),
            "nextPageToken", "t1");
        Map<String, Object> page2 = Map.of(
            "responses", List.of(
                Map.of("responseId", "older", "respondentEmail", "o@example.com", "createTime",
                    "2025-01-01T10:00:00Z")));

        Map<String, Object> form = Map.of(
            "items", List.of(
                Map.of(
                    "title", "Q1",
                    "questionItem", Map.of(
                        "question", Map.of("questionId", "q1")))));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(page1, page2, form, form);

        List<Map<String, Object>> customResponses = GoogleFormsUtils.getCustomResponses(
            mockedContext, "form-xyz", null);

        assertEquals(2, customResponses.size());

        assertEquals(List.of(
            Map.of(FORM_ID, "form-xyz", RESPONSE_ID, "newer", RESPONDENT_EMAIL, "n@example.com"),
            Map.of(FORM_ID, "form-xyz", RESPONSE_ID, "older", RESPONDENT_EMAIL, "o@example.com")), customResponses);

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        Http.Configuration configuration = configurationBuilder.build();

        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals(
            List.of("/forms/form-xyz/responses", "/forms/form-xyz/responses", "/forms/form-xyz", "/forms/form-xyz"),
            stringArgumentCaptor.getAllValues());

        List<Object[]> objectsArgumentCaptorAllValues = objectsArgumentCaptor.getAllValues();

        assertEquals(2, objectsArgumentCaptorAllValues.size());

        Object[] queryParameters1 = {
            "pageSize", 5000
        };
        Object[] queryParameters2 = {
            "pageSize", 5000, NEXT_PAGE_TOKEN, "t1"
        };

        assertArrayEquals(queryParameters1, objectsArgumentCaptorAllValues.get(0));
        assertArrayEquals(queryParameters2, objectsArgumentCaptorAllValues.get(1));
    }
}
