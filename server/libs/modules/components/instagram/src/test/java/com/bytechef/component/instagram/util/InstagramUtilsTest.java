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

package com.bytechef.component.instagram.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
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
class InstagramUtilsTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testCreateMediaContainer(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("id", "container-1"));

        String containerId = InstagramUtils.createMediaContainer(
            mockedContext, "user-1", Map.of("image_url", "https://example.com/a.jpg"));

        assertEquals("container-1", containerId);
        assertEquals("/user-1/media", stringArgumentCaptor.getValue());
        assertEquals(
            Body.of(Map.of("image_url", "https://example.com/a.jpg"), BodyContentType.JSON),
            bodyArgumentCaptor.getValue());
    }

    @Test
    void testCreateMediaContainerWithoutIdFails(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        when(mockedHttp.post(anyString()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(any(Body.class)))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of());

        assertThrows(
            ProviderException.class,
            () -> InstagramUtils.createMediaContainer(mockedContext, "user-1", Map.of()));
    }

    @Test
    void testGetComments(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        List<Map<String, Object>> comments = List.of(Map.of("id", "comment-1", "text", "Nice"));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter("fields", "id,text,username,timestamp,like_count,replies"))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("data", comments));

        assertEquals(comments, InstagramUtils.getComments(mockedContext, "media-1"));
        assertEquals("/media-1/comments", stringArgumentCaptor.getValue());
    }

    @Test
    void testGetDataIgnoresMissingData() {
        assertTrue(
            InstagramUtils.getData(Map.of("paging", Map.of()))
                .isEmpty());
        assertTrue(
            InstagramUtils.getData(null)
                .isEmpty());
    }

    @Test
    void testGetUserId(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter("fields", "user_id"))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("user_id", 17841400000000000L));

        assertEquals("17841400000000000", InstagramUtils.getUserId(mockedContext));
        assertEquals("/me", stringArgumentCaptor.getValue());
    }

    @Test
    void testPublishMediaContainer(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("id", "media-1"));

        assertEquals(
            Map.of("id", "media-1"), InstagramUtils.publishMediaContainer(mockedContext, "user-1", "container-1"));
        assertEquals("/user-1/media_publish", stringArgumentCaptor.getValue());
        assertEquals(
            Body.of(Map.of("creation_id", "container-1"), BodyContentType.JSON), bodyArgumentCaptor.getValue());
    }

    @Test
    void testWaitUntilContainerFinished(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter("fields", "status_code,status"))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("status_code", "FINISHED"));

        InstagramUtils.waitUntilContainerFinished(mockedContext, "container-1");

        assertEquals("/container-1", stringArgumentCaptor.getValue());
    }

    @Test
    void testWaitUntilContainerFinishedFailsOnError(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        when(mockedHttp.get(anyString()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter("fields", "status_code,status"))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("status_code", "ERROR", "status", "Unsupported video format"));

        ProviderException providerException = assertThrows(
            ProviderException.class,
            () -> InstagramUtils.waitUntilContainerFinished(mockedContext, "container-1"));

        assertTrue(
            providerException.getMessage()
                .contains("Unsupported video format"));
    }

    @Test
    void testWaitUntilContainerFinishedFailsOnExpired(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        when(mockedHttp.get(anyString()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter("fields", "status_code,status"))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("status_code", "EXPIRED"));

        assertThrows(
            ProviderException.class,
            () -> InstagramUtils.waitUntilContainerFinished(mockedContext, "container-1"));
    }
}
