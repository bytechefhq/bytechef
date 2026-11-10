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
import static com.bytechef.component.instagram.constant.InstagramConstants.VIDEO_URL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
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
 * @author Ivica Cardic
 */
@ExtendWith(MockContextSetupExtension.class)
class InstagramCreateReelActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final ArgumentCaptor<String> getPathArgumentCaptor = forClass(String.class);
    private final Parameters parameters = MockParametersFactory.create(
        Map.of(VIDEO_URL, "https://example.com/video.mp4", CAPTION, "Reel"));
    private final ArgumentCaptor<String> postPathArgumentCaptor = forClass(String.class);

    @Test
    void testPerformPublishesWhenFinished(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        when(mockedHttp.get(getPathArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedHttp.post(postPathArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(anyString(), anyString()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of("user_id", "user-1"), Map.of("id", "container-1"), Map.of("status_code", "FINISHED"),
                Map.of("id", "media-1"));

        Map<String, Object> result = InstagramCreateReelAction.perform(parameters, null, mockedContext);

        assertEquals(Map.of("id", "media-1"), result);
        assertEquals(List.of("/me", "/container-1"), getPathArgumentCaptor.getAllValues());
        assertEquals(List.of("/user-1/media", "/user-1/media_publish"), postPathArgumentCaptor.getAllValues());
        assertEquals(
            List.of(
                Body.of(
                    Map.of("media_type", "REELS", "video_url", "https://example.com/video.mp4", "caption", "Reel"),
                    BodyContentType.JSON),
                Body.of(Map.of("creation_id", "container-1"), BodyContentType.JSON)),
            bodyArgumentCaptor.getAllValues());
    }

    @Test
    void testPerformFailsWithoutPublishingOnError(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        when(mockedHttp.get(anyString()))
            .thenReturn(mockedExecutor);
        when(mockedHttp.post(postPathArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(anyString(), anyString()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(any(Body.class)))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of("user_id", "user-1"), Map.of("id", "container-1"),
                Map.of("status_code", "ERROR", "status", "Video too long"));

        assertThrows(
            ProviderException.class, () -> InstagramCreateReelAction.perform(parameters, null, mockedContext));

        assertEquals(List.of("/user-1/media"), postPathArgumentCaptor.getAllValues());
        verify(mockedHttp, never()).post("/user-1/media_publish");
    }
}
