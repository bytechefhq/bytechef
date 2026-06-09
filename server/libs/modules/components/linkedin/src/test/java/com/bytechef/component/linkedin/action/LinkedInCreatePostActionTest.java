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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.linkedin.action.LinkedInCreatePostAction.Author;
import com.bytechef.component.linkedin.util.LinkedInUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
@ExtendWith(MockContextSetupExtension.class)
class LinkedInCreatePostActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final ArgumentCaptor<Context> contextArgumentCaptor = forClass(Context.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Encoder, Executor>> encoderFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final ArgumentCaptor<FileEntry> fileEntryArgumentCaptor = forClass(FileEntry.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Json, Executor>> jsonFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final Parameters mockedConnectionParameters = MockParametersFactory.create(
        Map.of("id_token",
            "eyJ6aXAiOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImQ5Mjk2NjhhLWJhYjEtNGM2OS05NTk4LTQzNzMxNDk3MjNmZiIsImFsZyI6IlJTMjU2In0.eyJpc3MiOiJodHRwczovL3d3dy5saW5rZWRpbi5jb20vb2F1dGgiLCJhdWQiOiI4NjNjMDA4a3NjNTlnbiIsImlhdCI6MTc2MTI5MTkyOSwiZXhwIjoxNzYxMjk1NTI5LCJzdWIiOiJGODlpY2tERUhNIiwibmFtZSI6Ik1vbmlrYSBLdcWhdGVyIiwiZ2l2ZW5fbmFtZSI6Ik1vbmlrYSIsImZhbWlseV9uYW1lIjoiS3XFoXRlciIsInBpY3R1cmUiOiJodHRwczovL21lZGlhLmxpY2RuLmNvbS9kbXMvaW1hZ2UvdjIvQzRFMDNBUUVmOUNEYktfODZ1dy9wcm9maWxlLWRpc3BsYXlwaG90by1zaHJpbmtfMTAwXzEwMC9wcm9maWxlLWRpc3BsYXlwaG90by1zaHJpbmtfMTAwXzEwMC8wLzE2NjE4NzcxNTcxMjE_ZT0xNzYyOTkyMDAwJnY9YmV0YSZ0PWNCQ3RlMjhVR0cyOUFBOWdBb3ZXYkRNUkJpTWlZWW5vdm9WcHgxOGVZb28iLCJlbWFpbCI6ImRvbWl0ZXJtb25pa2FAZ21haWwuY29tIiwiZW1haWxfdmVyaWZpZWQiOiJ0cnVlIiwibG9jYWxlIjoiZW5fVVMifQ.ivlFbzoIPW2DFwSmnNCuts-wIejHvt5DmRJW6q_vpHSlmVV4M4t8Uyuz51v01Zbbpgvr4NhI5r9l0mrrNRO28y3kkzYtVZslyV9cy8yF_I902ywR5knumq6cl1gVP8P18hLxEQDNfNdaLEC5ULGiucSL2jZmWLSnBYRdNQUgW-SU-vgxHXJPoNexciM3E4Fome044M4osGKMFPi5vnTPBw19_ErJ-_SJZNLTt3PM3uO8m51-iC"));
    private final Encoder mockedEncoder = mock(Encoder.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final Json mockedJson = mock(Json.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(AUTHOR, Author.PERSON.name(),
        COMMENTARY, "some comment", CONTENT_TYPE, IMAGES, VISIBILITY, "PUBLIC", IMAGES, List.of(mockedFileEntry)));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        Context mockedActionContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        try (MockedStatic<LinkedInUtils> githubUtilsMockedStatic = mockStatic(LinkedInUtils.class)) {
            githubUtilsMockedStatic
                .when(() -> LinkedInUtils.uploadContent(
                    contextArgumentCaptor.capture(), fileEntryArgumentCaptor.capture(), stringArgumentCaptor.capture(),
                    stringArgumentCaptor.capture()))
                .thenReturn("imageId");

            byte[] bytes = {
                1, 2, 3
            };

            when(mockedActionContext.encoder(encoderFunctionArgumentCaptor.capture()))
                .thenAnswer(inv -> encoderFunctionArgumentCaptor.getValue()
                    .apply(mockedEncoder));
            when(mockedEncoder.base64UrlDecode(stringArgumentCaptor.capture()))
                .thenReturn(bytes);

            when(mockedActionContext.json(jsonFunctionArgumentCaptor.capture()))
                .thenAnswer(inv -> jsonFunctionArgumentCaptor.getValue()
                    .apply(mockedJson));

            when(mockedHttp.post(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.body(bodyArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedResponse.getFirstHeader(stringArgumentCaptor.capture()))
                .thenReturn("abc");

            String result = LinkedInCreatePostAction.perform(
                mockedParameters, mockedConnectionParameters, mockedActionContext);

            assertEquals("abc", result);
            assertNotNull(httpFunctionArgumentCaptor.getValue());
            assertEquals(mockedActionContext, contextArgumentCaptor.getValue());
            assertEquals(
                List.of(
                    "eyJpc3MiOiJodHRwczovL3d3dy5saW5rZWRpbi5jb20vb2F1dGgiLCJhdWQiOiI4NjNjMDA4a3NjNTlnbiIsImlhdCI6MTc2MTI5MTkyOSwiZXhwIjoxNzYxMjk1NTI5LCJzdWIiOiJGODlpY2tERUhNIiwibmFtZSI6Ik1vbmlrYSBLdcWhdGVyIiwiZ2l2ZW5fbmFtZSI6Ik1vbmlrYSIsImZhbWlseV9uYW1lIjoiS3XFoXRlciIsInBpY3R1cmUiOiJodHRwczovL21lZGlhLmxpY2RuLmNvbS9kbXMvaW1hZ2UvdjIvQzRFMDNBUUVmOUNEYktfODZ1dy9wcm9maWxlLWRpc3BsYXlwaG90by1zaHJpbmtfMTAwXzEwMC9wcm9maWxlLWRpc3BsYXlwaG90by1zaHJpbmtfMTAwXzEwMC8wLzE2NjE4NzcxNTcxMjE_ZT0xNzYyOTkyMDAwJnY9YmV0YSZ0PWNCQ3RlMjhVR0cyOUFBOWdBb3ZXYkRNUkJpTWlZWW5vdm9WcHgxOGVZb28iLCJlbWFpbCI6ImRvbWl0ZXJtb25pa2FAZ21haWwuY29tIiwiZW1haWxfdmVyaWZpZWQiOiJ0cnVlIiwibG9jYWxlIjoiZW5fVVMifQ",
                    "urn:li:person:null", IMAGES, "/rest/posts", "x-restli-id"),
                stringArgumentCaptor.getAllValues());

            assertEquals(Body.of(AUTHOR, "urn:li:person:null",
                COMMENTARY, "some comment",
                "distribution", Map.of("feedDistribution", "MAIN_FEED"),
                "lifecycleState", "PUBLISHED",
                VISIBILITY, "PUBLIC",
                "content", Map.of("media", Map.of("id", "imageId"))),
                bodyArgumentCaptor.getValue());

            ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
            Configuration configuration = configurationBuilder.build();

            assertEquals(ResponseType.JSON, configuration.getResponseType());
        }
    }
}
