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

package com.bytechef.component.copper.util;

import static com.bytechef.component.definition.ComponentDSL.option;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * @author Monika Domiter
 */
class CopperOptionUtilsTest {

    private MockedStatic<CopperUtils> copperUtilsMockedStatic;
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Context.Http.Executor mockedExecutor = mock(Context.Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Context.Http.Response mockedResponse = mock(Context.Http.Response.class);

    @BeforeEach
    public void beforeEach() {
        copperUtilsMockedStatic = mockStatic(CopperUtils.class);

        copperUtilsMockedStatic.when(() -> CopperUtils.getHeaders(mockedParameters))
            .thenReturn(Map.of());

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.headers(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
    }

    @AfterEach
    public void afterEach() {
        copperUtilsMockedStatic.close();
    }

    @Test
    void testGetActivityTypeOptions() {
        LinkedHashMap<String, ArrayList<LinkedHashMap<String, Object>>> linkedHashMap = new LinkedHashMap<>();
        ArrayList<LinkedHashMap<String, Object>> list = new ArrayList<>();
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();

        map.put("name", "ActivityName");
        map.put("id", "ActivityId");
        list.add(map);
        linkedHashMap.put("user", list);

        when(mockedResponse.getBody(any(Context.TypeReference.class)))
            .thenReturn(linkedHashMap);

        List<Option<String>> expectedOptions = new ArrayList<>();
        expectedOptions.add(option("ActivityName", "ActivityId"));

        assertEquals(expectedOptions,
            CopperOptionUtils.getActivityTypeOptions(mockedParameters, mockedParameters, "", mockedContext));
    }

    @Test
    void testGetCompanyIdOptions() {
        ArrayList<LinkedHashMap<String, String>> linkedHashMaps = new ArrayList<>();

        LinkedHashMap<String, String> tagMap = new LinkedHashMap<>();
        tagMap.put("name", "companyName");
        tagMap.put("id", "123");

        linkedHashMaps.add(tagMap);

        when(mockedResponse.getBody(any(Context.TypeReference.class)))
            .thenReturn(linkedHashMaps);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("companyName", "123"));
        assertEquals(expectedOptions,
            CopperOptionUtils.getCompanyIdOptions(mockedParameters, mockedParameters, "", mockedContext));
    }

    @Test
    void testGetContactTypesOptions() {
        ArrayList<LinkedHashMap<String, Object>> linkedHashMaps = new ArrayList<>();

        LinkedHashMap<String, Object> tagMap = new LinkedHashMap<>();
        tagMap.put("name", "ContactType");
        tagMap.put("id", "123");

        linkedHashMaps.add(tagMap);

        when(mockedResponse.getBody(any(Context.TypeReference.class)))
            .thenReturn(linkedHashMaps);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("ContactType", "123"));
        assertEquals(expectedOptions,
            CopperOptionUtils.getContactTypesOptions(mockedParameters, mockedParameters, "", mockedContext));

    }

    @Test
    void testGetTagsOptions() {
        ArrayList<LinkedHashMap<String, Object>> linkedHashMaps = new ArrayList<>();

        LinkedHashMap<String, Object> tagMap = new LinkedHashMap<>();
        tagMap.put("name", "Tag1");

        linkedHashMaps.add(tagMap);

        when(mockedResponse.getBody(any(Context.TypeReference.class)))
            .thenReturn(linkedHashMaps);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("Tag1", "Tag1"));

        assertEquals(expectedOptions,
            CopperOptionUtils.getTagsOptions(mockedParameters, mockedParameters, "", mockedContext));
    }

    @Test
    void testGetUserOptions() {
        ArrayList<LinkedHashMap<String, String>> linkedHashMaps = new ArrayList<>();

        LinkedHashMap<String, String> tagMap = new LinkedHashMap<>();
        tagMap.put("name", "ContactType");
        tagMap.put("id", "123");

        linkedHashMaps.add(tagMap);

        when(mockedResponse.getBody(any(Context.TypeReference.class)))
            .thenReturn(linkedHashMaps);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("ContactType", "123"));
        assertEquals(expectedOptions,
            CopperOptionUtils.getUserOptions(mockedParameters, mockedParameters, "", mockedContext));
    }
}
