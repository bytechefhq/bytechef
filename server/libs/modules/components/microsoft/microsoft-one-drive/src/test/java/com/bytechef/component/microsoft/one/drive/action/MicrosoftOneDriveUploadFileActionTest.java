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

import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.FILE;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.PARENT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
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
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
@ExtendWith(MockContextSetupExtension.class)
class MicrosoftOneDriveUploadFileActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Parameters mockedParameters = MockParametersFactory.create(Map.of(PARENT_ID, "xy", FILE, mockedFileEntry));

        Map<String, String> map = Map.of("key", "value");

        when(mockedFileEntry.getName())
            .thenReturn("file.pdf");
        when(mockedHttp.put(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(map);

        Object result = MicrosoftOneDriveUploadFileAction.perform(mockedParameters, null, mockedContext);

        assertEquals(map, result);

        ContextFunction<Http, Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();
        ResponseType responseType = configuration.getResponseType();

        assertEquals(ResponseType.JSON, responseType);
        assertEquals("/me/drive/items/xy:/file.pdf:/content", stringArgumentCaptor.getValue());
        assertEquals(Body.of(mockedFileEntry), bodyArgumentCaptor.getValue());
    }

    @Test
    void testPerformWithoutParentId(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Parameters parametersWithEmptyParentId = MockParametersFactory.create(Map.of(FILE, mockedFileEntry));
        Map<String, String> map = Map.of("key", "value");

        when(mockedFileEntry.getName())
            .thenReturn("file.pdf");
        when(mockedHttp.put(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(map);

        Object result = MicrosoftOneDriveUploadFileAction.perform(parametersWithEmptyParentId, null, mockedContext);

        assertEquals(map, result);
        ContextFunction<Http, Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();
        ResponseType responseType = configuration.getResponseType();

        assertEquals(ResponseType.JSON, responseType);
        assertEquals("/me/drive/items/root:/file.pdf:/content", stringArgumentCaptor.getValue());
        assertEquals(Body.of(mockedFileEntry), bodyArgumentCaptor.getValue());
    }
}
