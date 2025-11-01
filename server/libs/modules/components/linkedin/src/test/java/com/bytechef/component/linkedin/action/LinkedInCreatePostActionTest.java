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

package com.bytechef.component.linkedin.action;

import static com.bytechef.component.definition.Context.ContextFunction;
import static com.bytechef.component.definition.Context.Encoder;
import static com.bytechef.component.definition.Context.Http;
import static com.bytechef.component.definition.Context.Json;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.AUTHOR;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.COMMENTARY;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.CONTENT_TYPE;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.IMAGES;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.VISIBILITY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.ActionDefinition.SingleConnectionPerformFunction;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.linkedin.action.LinkedInCreatePostAction.Author;
import com.bytechef.component.linkedin.util.LinkedInUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class LinkedInCreatePostActionTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final ArgumentCaptor<Context> contextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Encoder, Http.Executor>> encoderFunctionArgumentCaptor =
        ArgumentCaptor.forClass(ContextFunction.class);
    private final ArgumentCaptor<FileEntry> fileEntryArgumentCaptor = ArgumentCaptor.forClass(FileEntry.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor =
        ArgumentCaptor.forClass(ContextFunction.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Json, Http.Executor>> jsonFunctionArgumentCaptor =
        ArgumentCaptor.forClass(ContextFunction.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Parameters mockedConnectionParameters = MockParametersFactory.create(
        Map.of("id_token",
            "eyJ6aXAiOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImQ5Mjk2NjhhLWJhYjEtNGM2OS05NTk4LTQzNzMxNDk3MjNmZiIsImFsZyI6IlJTMjU2In0.eyJpc3MiOiJodHRwczovL3d3dy5saW5rZWRpbi5jb20vb2F1dGgiLCJhdWQiOiI4NjNjMDA4a3NjNTlnbiIsImlhdCI6MTc2MTI5MTkyOSwiZXhwIjoxNzYxMjk1NTI5LCJzdWIiOiJGODlpY2tERUhNIiwibmFtZSI6Ik1vbmlrYSBLdcWhdGVyIiwiZ2l2ZW5fbmFtZSI6Ik1vbmlrYSIsImZhbWlseV9uYW1lIjoiS3XFoXRlciIsInBpY3R1cmUiOiJodHRwczovL21lZGlhLmxpY2RuLmNvbS9kbXMvaW1hZ2UvdjIvQzRFMDNBUUVmOUNEYktfODZ1dy9wcm9maWxlLWRpc3BsYXlwaG90by1zaHJpbmtfMTAwXzEwMC9wcm9maWxlLWRpc3BsYXlwaG90by1zaHJpbmtfMTAwXzEwMC8wLzE2NjE4NzcxNTcxMjE_ZT0xNzYyOTkyMDAwJnY9YmV0YSZ0PWNCQ3RlMjhVR0cyOUFBOWdBb3ZXYkRNUkJpTWlZWW5vdm9WcHgxOGVZb28iLCJlbWFpbCI6ImRvbWl0ZXJtb25pa2FAZ21haWwuY29tIiwiZW1haWxfdmVyaWZpZWQiOiJ0cnVlIiwibG9jYWxlIjoiZW5fVVMifQ.ivlFbzoIPW2DFwSmnNCuts-wIejHvt5DmRJW6q_vpHSlmVV4M4t8Uyuz51v01Zbbpgvr4NhI5r9l0mrrNRO28y3kkzYtVZslyV9cy8yF_I902ywR5knumq6cl1gVP8P18hLxEQDNfNdaLEC5ULGiucSL2jZmWLSnBYRdNQUgW-SU-vgxHXJPoNexciM3E4Fome044M4osGKMFPi5vnTPBw19_ErJ-_SJZNLTt3PM3uO8m51-iC"));
    private final Encoder mockedEncoder = mock(Encoder.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final Http mockedHttp = mock(Http.class);
    private final Json mockedJson = mock(Json.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPerform() throws Exception {
        when(mockedParameters.getRequiredString(AUTHOR))
            .thenReturn(Author.PERSON.name());
        when(mockedParameters.getRequiredString(COMMENTARY))
            .thenReturn("some comment");
        when(mockedParameters.getString(CONTENT_TYPE))
            .thenReturn(IMAGES);
        when(mockedParameters.getString(VISIBILITY, "PUBLIC"))
            .thenReturn("PUBLIC");
        when(mockedParameters.getRequiredList(IMAGES, FileEntry.class))
            .thenReturn(List.of(mockedFileEntry));

        try (MockedStatic<LinkedInUtils> githubUtilsMockedStatic = mockStatic(LinkedInUtils.class)) {
            githubUtilsMockedStatic
                .when(() -> LinkedInUtils.uploadContent(
                    contextArgumentCaptor.capture(), fileEntryArgumentCaptor.capture(), stringArgumentCaptor.capture(),
                    stringArgumentCaptor.capture()))
                .thenReturn("imageId");

            Optional<PerformFunction> performFunction = LinkedInCreatePostAction.ACTION_DEFINITION.getPerform();

            assertTrue(performFunction.isPresent());

            SingleConnectionPerformFunction singleConnectionPerformFunction =
                (SingleConnectionPerformFunction) performFunction.get();

            byte[] bytes = {
                1, 2, 3
            };

            when(mockedActionContext.encoder(encoderFunctionArgumentCaptor.capture()))
                .thenAnswer(inv -> encoderFunctionArgumentCaptor.getValue()
                    .apply(mockedEncoder));
            when(mockedEncoder.urlDecodeBase64FromString(stringArgumentCaptor.capture()))
                .thenReturn(bytes);

            when(mockedActionContext.json(jsonFunctionArgumentCaptor.capture()))
                .thenAnswer(inv -> jsonFunctionArgumentCaptor.getValue()
                    .apply(mockedJson));

            when(mockedActionContext.http(httpFunctionArgumentCaptor.capture()))
                .thenAnswer(inv -> httpFunctionArgumentCaptor.getValue()
                    .apply(mockedHttp));
            when(mockedHttp.post(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.body(bodyArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.configuration(any()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.execute())
                .thenReturn(mockedResponse);
            when(mockedResponse.getFirstHeader(stringArgumentCaptor.capture()))
                .thenReturn("abc");

            Object result = singleConnectionPerformFunction.apply(
                mockedParameters, mockedConnectionParameters, mockedActionContext);

            assertEquals("abc", result);
            assertEquals(mockedActionContext, contextArgumentCaptor.getValue());
            assertEquals(
                List.of(
                    "eyJpc3MiOiJodHRwczovL3d3dy5saW5rZWRpbi5jb20vb2F1dGgiLCJhdWQiOiI4NjNjMDA4a3NjNTlnbiIsImlhdCI6MTc2MTI5MTkyOSwiZXhwIjoxNzYxMjk1NTI5LCJzdWIiOiJGODlpY2tERUhNIiwibmFtZSI6Ik1vbmlrYSBLdcWhdGVyIiwiZ2l2ZW5fbmFtZSI6Ik1vbmlrYSIsImZhbWlseV9uYW1lIjoiS3XFoXRlciIsInBpY3R1cmUiOiJodHRwczovL21lZGlhLmxpY2RuLmNvbS9kbXMvaW1hZ2UvdjIvQzRFMDNBUUVmOUNEYktfODZ1dy9wcm9maWxlLWRpc3BsYXlwaG90by1zaHJpbmtfMTAwXzEwMC9wcm9maWxlLWRpc3BsYXlwaG90by1zaHJpbmtfMTAwXzEwMC8wLzE2NjE4NzcxNTcxMjE_ZT0xNzYyOTkyMDAwJnY9YmV0YSZ0PWNCQ3RlMjhVR0cyOUFBOWdBb3ZXYkRNUkJpTWlZWW5vdm9WcHgxOGVZb28iLCJlbWFpbCI6ImRvbWl0ZXJtb25pa2FAZ21haWwuY29tIiwiZW1haWxfdmVyaWZpZWQiOiJ0cnVlIiwibG9jYWxlIjoiZW5fVVMifQ",
                    "urn:li:person:null", IMAGES, "/rest/posts", "x-restli-id"),
                stringArgumentCaptor.getAllValues());

            Http.Body body = bodyArgumentCaptor.getValue();

            Map<String, Object> expectedBody = Map.of(
                AUTHOR, "urn:li:person:null",
                COMMENTARY, "some comment",
                "distribution", Map.of("feedDistribution", "MAIN_FEED"),
                "lifecycleState", "PUBLISHED",
                VISIBILITY, "PUBLIC",
                "content", Map.of("media", Map.of("id", "imageId")));

            assertEquals(expectedBody, body.getContent());
        }
    }
}
