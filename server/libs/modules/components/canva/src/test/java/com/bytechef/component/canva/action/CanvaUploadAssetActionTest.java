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
import static com.bytechef.component.canva.constant.CanvaConstants.DELAY_MS;
import static com.bytechef.component.canva.constant.CanvaConstants.MAX_ATTEMPTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.canva.util.CanvaUtils;
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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Ivona Pavela
 */
@ExtendWith(MockContextSetupExtension.class)
class CanvaUploadAssetActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Http.Body.class);
    private final ArgumentCaptor<Context> contextArgumentCaptor = forClass(Context.class);
    private final FileEntry fileEntry = mock(FileEntry.class);
    private final ArgumentCaptor<Integer> integerArgumentCaptor = forClass(Integer.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(ASSET_NAME, "test", ASSET, fileEntry));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        try (MockedStatic<CanvaUtils> canvaUtilsMockedStatic = mockStatic(CanvaUtils.class)) {
            canvaUtilsMockedStatic.when(
                () -> CanvaUtils.pollJob(
                    contextArgumentCaptor.capture(), stringArgumentCaptor.capture(),
                    integerArgumentCaptor.capture(), integerArgumentCaptor.capture()))
                .thenReturn(Map.of());

            when(mockedHttp.post(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.header(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.body(bodyArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(Map.of("job", Map.of("id", "123")));

            when(mockedContext.encoder(any()))
                .thenReturn("base64name");
            when(mockedContext.json(any()))
                .thenReturn("jsonName");

            Object result = CanvaUploadAssetAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertEquals(Map.of(), result);

            List<String> expectedStrings = List.of("/asset-uploads", "Asset-Upload-Metadata", "jsonName");

            assertEquals(expectedStrings, stringArgumentCaptor.getAllValues());

            ContextFunction<Http, Executor> function = httpFunctionArgumentCaptor.getValue();
            assertNotNull(function);

            ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
            Http.Configuration configuration = configurationBuilder.build();
            Http.ResponseType responseType = configuration.getResponseType();

            assertEquals(Http.ResponseType.Type.JSON, responseType.getType());

            Body body = bodyArgumentCaptor.getValue();

            assertEquals(fileEntry, body.getContent());
            assertEquals(mockedContext, contextArgumentCaptor.getValue());
            assertEquals(List.of(MAX_ATTEMPTS, DELAY_MS), integerArgumentCaptor.getAllValues());
        }
    }
}
