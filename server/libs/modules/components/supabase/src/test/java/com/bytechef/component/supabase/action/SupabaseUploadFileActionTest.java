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

package com.bytechef.component.supabase.action;

import static com.bytechef.component.supabase.constant.SupabaseConstants.BUCKET_NAME;
import static com.bytechef.component.supabase.constant.SupabaseConstants.FILE;
import static com.bytechef.component.supabase.constant.SupabaseConstants.FILE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Encoder;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
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

/**
 * @author Nikolina Spehar
 */
@ExtendWith(MockContextSetupExtension.class)
class SupabaseUploadFileActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Encoder, Executor>> encoderFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final Encoder mockedEncoder = mock(Encoder.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(
        FILE, mockedFileEntry, BUCKET_NAME, "bucketName", FILE_NAME, "fileName"));
    private final Map<String, Object> responseMap = Map.of();
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedContext.encoder(encoderFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<Encoder, Executor> value = encoderFunctionArgumentCaptor.getValue();

                return value.apply(mockedEncoder);
            });
        when(mockedEncoder.base64UrlEncode(stringArgumentCaptor.capture()))
            .thenReturn("encoded");

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        Map<String, Object> result = SupabaseUploadFileAction.perform(
            mockedParameters, mockedParameters, mockedContext);

        assertEquals(responseMap, result);
        assertNotNull(encoderFunctionArgumentCaptor.getValue());
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals(Body.of(mockedFileEntry), bodyArgumentCaptor.getValue());
        assertEquals(
            List.of("bucketName", "fileName", "/storage/v1/object/encoded/encoded"),
            stringArgumentCaptor.getAllValues());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
    }
}
