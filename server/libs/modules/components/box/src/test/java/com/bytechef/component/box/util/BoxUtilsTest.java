/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.box.util;

import static com.bytechef.component.box.constant.BoxConstants.ID;
import static com.bytechef.component.box.constant.BoxConstants.NAME;
import static com.bytechef.component.box.constant.BoxConstants.TYPE;
import static com.bytechef.component.definition.ComponentDSL.option;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Domiter
 */
class BoxUtilsTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @Test
    void testGetRootFolderOptions() {
        Map<String, List<Map<String, Object>>> map = new LinkedHashMap<>();
        List<Map<String, Object>> entries = new ArrayList<>();
        Map<String, Object> folderMap = new LinkedHashMap<>();

        folderMap.put(NAME, "folderName");
        folderMap.put(ID, "folderId");
        folderMap.put(TYPE, "folder");

        entries.add(folderMap);

        map.put("entries", entries);

        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(map);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("folderName", "folderId"));
        expectedOptions.add(option("ROOT", "0"));

        assertEquals(
            expectedOptions,
            BoxUtils.getRootFolderOptions(mockedParameters, mockedParameters, Map.of(), "", mockedActionContext));
    }

    @Test
    void testGetFileIdOptions() {
        Map<String, List<Map<String, Object>>> map = new LinkedHashMap<>();
        List<Map<String, Object>> entries = new ArrayList<>();
        Map<String, Object> fileMap = new LinkedHashMap<>();

        fileMap.put(NAME, "file name");
        fileMap.put(ID, "fileId");
        fileMap.put(TYPE, "file");

        entries.add(fileMap);

        map.put("entries", entries);

        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(map);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("file name", "fileId"));

        assertEquals(
            expectedOptions,
            BoxUtils.getFileIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedActionContext));
    }

    @Test
    void testSubscribeWebhok() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(ID, "123"));

        assertEquals("123",
            BoxUtils.subscribeWebhook("webhookUrl", mockedContext, "type", "triggerEvent", "id"));

        Http.Body body = bodyArgumentCaptor.getValue();

        Object content = body.getContent();

        assertEquals(Map.of("address", "webhookUrl",
            "triggers", List.of("triggerEvent"),
            "target", Map.of(
                ID, "id",
                TYPE, "type")), content);
    }

    @Test
    void testUnsubscribeWebhook() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);

        BoxUtils.unsubscribeWebhook(mockedParameters, mockedContext);

        verify(mockedContext, times(1)).http(any());
        verify(mockedExecutor, times(1)).configuration(any());
        verify(mockedExecutor, times(1)).execute();
    }
}
