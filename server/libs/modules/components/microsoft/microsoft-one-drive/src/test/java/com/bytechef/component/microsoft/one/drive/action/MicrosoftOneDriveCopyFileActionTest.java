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
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.NAME;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.PARENT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
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
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
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
class MicrosoftOneDriveCopyFileActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Response mockedStatusResponse = mock(Response.class);

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse)
            .thenReturn(mockedStatusResponse);
        when(mockedResponse.getStatusCode())
            .thenReturn(202);
        when(mockedResponse.getFirstHeader(stringArgumentCaptor.capture()))
            .thenReturn("https://graph.microsoft.com/v1.0/monitor/operations/123");
        when(mockedStatusResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("status", "completed"));

        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(ID, "xy", NAME, "testFile", PARENT_ID, "testFolder"));

        Object result = MicrosoftOneDriveCopyFileAction.perform(mockedParameters, null, mockedContext);

        assertEquals(Map.of("status", "completed"), result);

        assertNotNull(httpFunctionArgumentCaptor.getValue());

        List<ConfigurationBuilder> configurationBuilders = configurationBuilderArgumentCaptor.getAllValues();

        assertEquals(2, configurationBuilders.size());

        for (ConfigurationBuilder configurationBuilder : configurationBuilders) {
            Configuration configuration = configurationBuilder.build();

            assertEquals(ResponseType.JSON, configuration.getResponseType());
        }

        assertEquals(
            List.of("/me/drive/items/xy/copy", "location", "https://graph.microsoft.com/v1.0/monitor/operations/123"),
            stringArgumentCaptor.getAllValues());
        assertEquals(
            Body.of(
                Map.of(NAME, "testFile", "parentReference", Map.of(ID, "testFolder")), Http.BodyContentType.JSON),
            bodyArgumentCaptor.getValue());
    }

    @Test
    void testPerformWithoutParentId(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Response mockedStatusResponse = mock(Response.class);

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse)
            .thenReturn(mockedStatusResponse);
        when(mockedResponse.getStatusCode())
            .thenReturn(202);
        when(mockedResponse.getFirstHeader(stringArgumentCaptor.capture()))
            .thenReturn("https://graph.microsoft.com/v1.0/monitor/operations/123");
        when(mockedStatusResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("status", "completed"));

        Parameters mockedParameters = MockParametersFactory.create(Map.of(ID, "xy", NAME, "testFile"));

        Object result = MicrosoftOneDriveCopyFileAction.perform(mockedParameters, null, mockedContext);

        assertNotNull(result);
        assertEquals(Map.of("status", "completed"), result);

        assertNotNull(httpFunctionArgumentCaptor.getValue());

        List<ConfigurationBuilder> configurationBuilders = configurationBuilderArgumentCaptor.getAllValues();

        assertEquals(2, configurationBuilders.size());

        for (ConfigurationBuilder configurationBuilder : configurationBuilders) {
            Configuration configuration = configurationBuilder.build();

            assertEquals(ResponseType.JSON, configuration.getResponseType());
        }

        assertEquals(
            List.of("/me/drive/items/xy/copy", "location", "https://graph.microsoft.com/v1.0/monitor/operations/123"),
            stringArgumentCaptor.getAllValues());
        assertEquals(
            Body.of(
                Map.of(NAME, "testFile", "parentReference", Map.of(ID, "root")), Http.BodyContentType.JSON),
            bodyArgumentCaptor.getValue());
    }
}
