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

package com.bytechef.component.sendgrid.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.ID;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.File;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
@ExtendWith(MockContextSetupExtension.class)
class SendgridUtilsTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final Object mockedObject = mock(Object.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<File, Executor>> fileFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final File mockedFile = mock(File.class);
    private final ArgumentCaptor<FileEntry> fileEntryArgumentCaptor = forClass(FileEntry.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);

    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    @Test
    void testGetAllAttachments(Context mockedContext) throws IOException {

        List<FileEntry> fileEntries = List.of(mockedFileEntry);

        byte[] bytes = {
            1, 2, 3
        };

        when(mockedContext.file(fileFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<File, Executor> value =
                    fileFunctionArgumentCaptor.getValue();

                return value.apply(mockedFile);
            });

        when(mockedFile.readAllBytes(fileEntryArgumentCaptor.capture()))
            .thenReturn(bytes);

        List<Map<String, Object>> result =
            SendgridUtils.getAllAttachments(fileEntries, mockedContext);

        assertEquals(1, result.size());

        Map<String, Object> attachment = result.getFirst();

        assertEquals(mockedFileEntry.getName(), attachment.get("filename"));
        assertEquals(mockedFileEntry.getMimeType(), attachment.get("type"));
        assertNotNull(attachment.get("content"));

        ContextFunction<File, Executor> fileContextFunction = fileFunctionArgumentCaptor.getValue();
        assertNotNull(fileContextFunction);

        assertEquals(mockedFileEntry, fileEntryArgumentCaptor.getValue());
    }

    @Test
    void testConvertToEmailList() {
        List<String> input = List.of("a@test.com", "b@test.com");

        List<Map<String, String>> result = SendgridUtils.convertToEmailList(input);
        assertEquals(List.of(Map.of("email", "a@test.com"), Map.of("email", "b@test.com")), result);
    }

    @Test
    void testSendEmail(
        Context mockedContext, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);

        Map<String, Object> body = Map.of("key", "value");

        SendgridUtils.sendEmail(mockedContext, body);

        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(Http.ResponseType.JSON, configuration.getResponseType());

        assertEquals("/mail/send", stringArgumentCaptor.getValue());
        assertEquals(body, bodyArgumentCaptor.getValue()
            .getContent());
    }

    @Test
    void testGetTemplateIdOptions(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(
                "templates", List.of(
                    Map.of(NAME, "name1", ID, "1"),
                    Map.of(NAME, "name2", ID, "2"))));

        List<Option<String>> result = SendgridUtils.getTemplateIdOptions(
            null, null, null, null, mockedContext);

        assertEquals(result, List.of(option("name1", "1"), option("name2", "2")));
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(Http.ResponseType.JSON, configuration.getResponseType());
        assertEquals(List.of("/templates", "generations", "dynamic"), stringArgumentCaptor.getAllValues());
    }
}
