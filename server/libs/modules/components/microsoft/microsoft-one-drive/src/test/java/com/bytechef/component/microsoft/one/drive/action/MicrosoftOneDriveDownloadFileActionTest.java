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

import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
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
class MicrosoftOneDriveDownloadFileActionTest {

    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Response mockedBinaryResponse = mock(Response.class);

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getFirstHeader(stringArgumentCaptor.capture()))
            .thenReturn("link");
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse)
            .thenReturn(mockedBinaryResponse);
        when(mockedBinaryResponse.getBody())
            .thenReturn(mockedFileEntry);

        Parameters mockedParameters = MockParametersFactory.create(Map.of(ID, "xy"));

        Object result = MicrosoftOneDriveDownloadFileAction.perform(mockedParameters, null, mockedContext);

        assertEquals(mockedFileEntry, result);

        assertNotNull(httpFunctionArgumentCaptor.getValue());

        List<ConfigurationBuilder> configurationBuilders = configurationBuilderArgumentCaptor.getAllValues();

        assertEquals(2, configurationBuilders.size());

        ConfigurationBuilder firstConfigurationBuilder = configurationBuilders.getFirst();
        Configuration firstConfiguration = firstConfigurationBuilder.build();

        assertEquals(ResponseType.JSON, firstConfiguration.getResponseType());

        ConfigurationBuilder lastConfigurationBuilder = configurationBuilders.getLast();
        Configuration lastConfiguration = lastConfigurationBuilder.build();

        ResponseType lastResponseType = lastConfiguration.getResponseType();
        ResponseType binary = ResponseType.binary("text/plain");

        assertEquals(binary.getContentType(), lastResponseType.getContentType());
        assertEquals(binary.getType(), lastResponseType.getType());
        assertEquals(List.of("/me/drive/items/xy/content", "location", "link"), stringArgumentCaptor.getAllValues());
    }
}
