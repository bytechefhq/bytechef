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

package com.bytechef.component.copper.util;

import static com.bytechef.component.copper.constant.CopperConstants.ID;
import static com.bytechef.component.copper.constant.CopperConstants.LEAD;
import static com.bytechef.component.copper.constant.CopperConstants.NAME;
import static com.bytechef.component.copper.constant.CopperConstants.TYPE;
import static com.bytechef.component.definition.ComponentDsl.option;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Domiter
 */
class CopperOptionUtilsTest {

    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @BeforeEach
    void beforeEach() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.headers(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
    }

    @Test
    void testGetActivityTypeOptions() {
        Map<String, List<Map<String, Object>>> linkedHashMap = new LinkedHashMap<>();
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> map = new LinkedHashMap<>();

        map.put(NAME, "ActivityName");
        map.put(ID, "ActivityId");
        list.add(map);
        linkedHashMap.put("user", list);

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(linkedHashMap);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("ActivityName", "ActivityId"));

        assertEquals(expectedOptions,
            CopperOptionUtils.getActivityTypeOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext));
    }

    @Test
    void testGetCompanyIdOptions() {
        List<Map<String, String>> linkedHashMaps = new ArrayList<>();

        Map<String, String> tagMap = new LinkedHashMap<>();

        tagMap.put(NAME, "companyName");
        tagMap.put(ID, "123");

        linkedHashMaps.add(tagMap);

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(linkedHashMaps);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("companyName", "123"));

        assertEquals(expectedOptions,
            CopperOptionUtils.getCompanyIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext));
    }

    @Test
    void testGetContactTypesOptions() {
        List<Map<String, Object>> linkedHashMaps = new ArrayList<>();

        Map<String, Object> tagMap = new LinkedHashMap<>();

        tagMap.put(NAME, "ContactType");
        tagMap.put(ID, "123");

        linkedHashMaps.add(tagMap);

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(linkedHashMaps);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("ContactType", "123"));

        assertEquals(expectedOptions,
            CopperOptionUtils.getContactTypesOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext));

    }

    @Test
    void testGetParentOptions() {
        Map<String, Object> parentMap = new LinkedHashMap<>();

        parentMap.put(NAME, "parentName");
        parentMap.put(ID, "123");

        List<Map<String, Object>> list = List.of(parentMap);

        when(mockedParameters.getRequiredString(TYPE))
            .thenReturn(LEAD);

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(list);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("parentName", "123"));

        assertEquals(expectedOptions,
            CopperOptionUtils.getParentOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext));
    }

    @Test
    void testGetTagsOptions() {
        List<Map<String, Object>> linkedHashMaps = new ArrayList<>();

        Map<String, Object> tagMap = new LinkedHashMap<>();

        tagMap.put(NAME, "Tag1");

        linkedHashMaps.add(tagMap);

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(linkedHashMaps);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("Tag1", "Tag1"));

        assertEquals(expectedOptions,
            CopperOptionUtils.getTagsOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext));
    }

    @Test
    void testGetUserOptions() {
        List<Map<String, String>> tags = new ArrayList<>();
        Map<String, String> tagMap = new LinkedHashMap<>();

        tagMap.put(NAME, "ContactType");
        tagMap.put(ID, "123");

        tags.add(tagMap);

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(tags);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("ContactType", "123"));

        assertEquals(expectedOptions,
            CopperOptionUtils.getUserOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext));
    }
}
