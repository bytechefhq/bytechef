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

package com.bytechef.component.canva.action;

import static com.bytechef.component.canva.constant.CanvaConstants.ASSET;
import static com.bytechef.component.canva.constant.CanvaConstants.ASSET_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivona Pavela
 */
@ExtendWith(MockContextSetupExtension.class)
class CanvaUploadAssetActionTest {

    FileEntry fileEntry = org.mockito.Mockito.mock(FileEntry.class);
    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Http.Body.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            ASSET_NAME, "test",
            ASSET, fileEntry));

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.post(stringArgumentCaptor.capture())).thenReturn(mockedExecutor);
        when(mockedExecutor.headers(any())).thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture())).thenReturn(mockedExecutor);

        when(mockedHttp.get(stringArgumentCaptor.capture())).thenReturn(mockedExecutor);

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("job", Map.of("id", "123")))
            .thenReturn(Map.of(
                "id", "123",
                "status", "success"));

        Object result = CanvaUploadAssetAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(
            Map.of(
                "id", "123",
                "status", "success"),
            result);

        assertEquals("/asset-uploads", stringArgumentCaptor.getAllValues()
            .get(0));
        assertEquals("/asset-uploads/123", stringArgumentCaptor.getAllValues()
            .get(1));

        ContextFunction<Http, Executor> function = httpFunctionArgumentCaptor.getValue();
        assertNotNull(function);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();

        assertEquals(Http.ResponseType.Type.JSON, configuration.getResponseType()
            .getType());

        assertEquals(2, bodyArgumentCaptor.getAllValues()
            .size());

        Body postBody = bodyArgumentCaptor.getAllValues()
            .get(0);
        assertEquals(Http.BodyContentType.BINARY, postBody.getContentType());

        Body getBody = bodyArgumentCaptor.getAllValues()
            .get(1);
        assertEquals(Http.BodyContentType.JSON, getBody.getContentType());
    }
}
