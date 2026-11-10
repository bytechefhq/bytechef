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

package com.bytechef.component.instagram.action;

import static com.bytechef.component.instagram.constant.InstagramConstants.CAPTION;
import static com.bytechef.component.instagram.constant.InstagramConstants.IMAGE_URL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
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
 * @author Ivica Cardic
 */
@ExtendWith(MockContextSetupExtension.class)
class InstagramCreateImagePostActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {
        Parameters parameters = MockParametersFactory.create(
            Map.of(IMAGE_URL, "https://example.com/image.jpg", CAPTION, "Hello"));

        when(mockedHttp.get(anyString()))
            .thenReturn(mockedExecutor);
        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(anyString(), anyString()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("user_id", "user-1"), Map.of("id", "container-1"), Map.of("id", "media-1"));

        Map<String, Object> result = InstagramCreateImagePostAction.perform(parameters, null, mockedContext);

        assertEquals(Map.of("id", "media-1"), result);
        assertEquals(List.of("/user-1/media", "/user-1/media_publish"), stringArgumentCaptor.getAllValues());
        assertEquals(
            List.of(
                Body.of(Map.of("image_url", "https://example.com/image.jpg", "caption", "Hello"), BodyContentType.JSON),
                Body.of(Map.of("creation_id", "container-1"), BodyContentType.JSON)),
            bodyArgumentCaptor.getAllValues());
    }

    @Test
    void testPerformWithoutCaption(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        Parameters parameters = MockParametersFactory.create(Map.of(IMAGE_URL, "https://example.com/image.jpg"));

        when(mockedHttp.get(anyString()))
            .thenReturn(mockedExecutor);
        when(mockedHttp.post(anyString()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(anyString(), anyString()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("user_id", "user-1"), Map.of("id", "container-1"), Map.of("id", "media-1"));

        InstagramCreateImagePostAction.perform(parameters, null, mockedContext);

        List<Body> bodies = bodyArgumentCaptor.getAllValues();

        assertEquals(
            Body.of(Map.of("image_url", "https://example.com/image.jpg"), BodyContentType.JSON), bodies.getFirst());
    }
}
