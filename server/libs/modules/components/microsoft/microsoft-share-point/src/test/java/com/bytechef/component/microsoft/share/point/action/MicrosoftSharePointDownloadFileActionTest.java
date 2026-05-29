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

package com.bytechef.component.microsoft.share.point.action;

import static com.bytechef.component.definition.Context.ContextFunction;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.FILE_ID;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.REDIRECT_STATUS_CODE;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.SITE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Nikolina Spehar
 */
@ExtendWith(MockContextSetupExtension.class)
class MicrosoftSharePointDownloadFileActionTest {

    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(SITE_ID, "testSiteId", FILE_ID, "testFileId"));
    private final FileEntry mockedFileEntry = mock(FileEntry.class);

    @Test
    void testPerformReturnsFileEntryOnRedirect(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getStatusCode())
            .thenReturn(REDIRECT_STATUS_CODE);
        when(mockedResponse.getFirstHeader(stringArgumentCaptor.capture()))
            .thenReturn("https://redirect.example.com/file");
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedFileEntry);

        FileEntry result = MicrosoftSharePointDownloadFileAction.perform(mockedParameters, null, mockedContext);

        assertEquals(mockedFileEntry, result);

        List<ContextFunction<Http, Executor>> values = httpFunctionArgumentCaptor.getAllValues();

        for (ContextFunction<Http, Executor> function : values) {
            assertNotNull(function);
        }

        List<ConfigurationBuilder> configurationBuilder = configurationBuilderArgumentCaptor.getAllValues();

        assertEquals(2, configurationBuilder.size());

        Configuration configuration = configurationBuilder.getFirst()
            .build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());

        Configuration configuration2 = configurationBuilder.getLast()
            .build();

        assertEquals(ResponseType.BINARY, configuration2.getResponseType());
        assertEquals(
            List.of("/sites/testSiteId/drive/items/testFileId/content", "location",
                "https://redirect.example.com/file"),
            stringArgumentCaptor.getAllValues());
    }

    @Test
    void testPerformThrowsProviderExceptionWhenNotRedirect(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getStatusCode())
            .thenReturn(200);

        assertThrows(
            ProviderException.class,
            () -> MicrosoftSharePointDownloadFileAction.perform(mockedParameters, null, mockedContext));

        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/sites/testSiteId/drive/items/testFileId/content", stringArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
    }
}
