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

package com.bytechef.component.active.campaign.util;

import static com.bytechef.component.definition.ComponentDSL.option;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Domiter
 */
class ActiveCampaignUtilsTest {

    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @BeforeEach
    void beforeEach() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
    }

    @Test
    void testGetContactIdOptions() {
        Map<String, Object> map = new LinkedHashMap<>();
        List<Map<String, String>> contacts = new ArrayList<>();
        Map<String, String> contactMap = new LinkedHashMap<>();

        contactMap.put("email", "email");
        contactMap.put("id", "id");

        contacts.add(contactMap);

        map.put("contacts", contacts);

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(map);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("email", "id"));

        assertEquals(
            expectedOptions,
            ActiveCampaignUtils.getContactIdOptions(mockedParameters, mockedParameters, "", mockedContext));
    }

    @Test
    void testGetTaskTypeIdOptions() {
        Map<String, Object> map = new LinkedHashMap<>();
        List<Map<String, String>> dealTaskTypes = new ArrayList<>();
        Map<String, String> taskTypeMap = new LinkedHashMap<>();

        taskTypeMap.put("title", "title");
        taskTypeMap.put("id", "id");

        dealTaskTypes.add(taskTypeMap);

        map.put("dealTasktypes", dealTaskTypes);

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(map);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("title", "id"));

        assertEquals(
            expectedOptions,
            ActiveCampaignUtils.getTaskTypeIdOptions(mockedParameters, mockedParameters, "", mockedContext));
    }
}
