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

package com.bytechef.component.microsoft.one.drive.action;

import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.MIME_TYPE;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.NAME;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.PARENT_ID;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
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
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
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
class MicrosoftOneDriveCreateNewTextFileActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<File, ?>> fileFunctionArgumentCaptor = forClass(ContextFunction.class);
    private final File mockedContextFile = mock(File.class);
    private final Object mockedObject = mock(Object.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerformTxt(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) throws Exception {

        when(mockedContext.file(fileFunctionArgumentCaptor.capture()))
            .thenAnswer(invocation -> {
                ContextFunction<File, ?> function = invocation.getArgument(0);

                return function.apply(mockedContextFile);
            });
        when(mockedContextFile.storeContent(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedFileEntry);
        when(mockedHttp.put(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(mockedObject);

        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(NAME, "testFile", TEXT, "abc", PARENT_ID, "xy", MIME_TYPE, "plain/text"));

        Object result = MicrosoftOneDriveCreateNewTextFileAction.perform(mockedParameters, null, mockedContext);

        assertEquals(mockedObject, result);

        assertNotNull(fileFunctionArgumentCaptor.getValue());
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals(
            List.of("testFile.txt", "abc", "/me/drive/items/xy:/testFile.txt:/content"),
            stringArgumentCaptor.getAllValues());
        assertEquals(Http.Body.of(mockedFileEntry), bodyArgumentCaptor.getValue());
    }

    @Test
    void testPerformCsv(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) throws Exception {

        when(mockedContext.file(fileFunctionArgumentCaptor.capture()))
            .thenAnswer(invocation -> {
                ContextFunction<File, ?> function = invocation.getArgument(0);

                return function.apply(mockedContextFile);
            });
        when(mockedContextFile.storeContent(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedFileEntry);
        when(mockedHttp.put(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(mockedObject);

        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(NAME, "testFile", PARENT_ID, "xy", MIME_TYPE, "text/csv", TEXT, "a,b,c"));

        Object result = MicrosoftOneDriveCreateNewTextFileAction.perform(mockedParameters, null, mockedContext);

        assertEquals(mockedObject, result);

        assertNotNull(fileFunctionArgumentCaptor.getValue());
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        Configuration configuration = configurationBuilderArgumentCaptor.getValue()
            .build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals(
            List.of("testFile.csv", "a,b,c", "/me/drive/items/xy:/testFile.csv:/content"),
            stringArgumentCaptor.getAllValues());
        assertEquals(Http.Body.of(mockedFileEntry), bodyArgumentCaptor.getValue());
    }

    @Test
    void testPerformXml(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) throws Exception {

        when(mockedContext.file(fileFunctionArgumentCaptor.capture()))
            .thenAnswer(invocation -> {
                ContextFunction<File, ?> function = invocation.getArgument(0);

                return function.apply(mockedContextFile);
            });
        when(mockedContextFile.storeContent(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedFileEntry);
        when(mockedHttp.put(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(mockedObject);

        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(NAME, "testFile", MIME_TYPE, "text/xml", TEXT, "<xml/>"));

        Object result = MicrosoftOneDriveCreateNewTextFileAction.perform(mockedParameters, null, mockedContext);

        assertEquals(mockedObject, result);

        assertNotNull(fileFunctionArgumentCaptor.getValue());
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        Configuration configuration = configurationBuilderArgumentCaptor.getValue()
            .build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals(
            List.of("testFile.xml", "<xml/>", "/me/drive/items/root:/testFile.xml:/content"),
            stringArgumentCaptor.getAllValues());
        assertEquals(Http.Body.of(mockedFileEntry), bodyArgumentCaptor.getValue());
    }
}
