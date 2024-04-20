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

package com.bytechef.component.accelo.util;

import static com.bytechef.component.accelo.constant.AcceloConstants.AGAINST_TYPE;
import static com.bytechef.component.accelo.constant.AcceloConstants.DEPLOYMENT;
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
import org.junit.jupiter.api.Test;

/**
 * @author Monika Domiter
 */
class AcceloUtilsTest {

    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @Test
    void testCreateUrl(){
        when(mockedParameters.getRequiredString(DEPLOYMENT))
            .thenReturn("deployment");

        String url = AcceloUtils.createUrl(mockedParameters, "resource");

        assertEquals("https://deployment.api.accelo.com/api/v0/resource", url);
    }

    @Test
    void testGetAgainstIdOptions() {
        Map<String, List<Map<String, Object>>> map = new LinkedHashMap<>();
        List<Map<String, Object>> countries = new ArrayList<>();
        Map<String, Object> companyMap = new LinkedHashMap<>();

        companyMap.put("title", "title");
        companyMap.put("id", "id");

        countries.add(companyMap);

        map.put("response", countries);

        when(mockedParameters.getRequiredString(AGAINST_TYPE))
            .thenReturn("issue");
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(map);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("title", "id"));

        assertEquals(
            expectedOptions,
            AcceloUtils.getAgainstIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext));
    }

    @Test
    void testGetCompanyIdOptions() {
        Map<String, List<Map<String, Object>>> map = new LinkedHashMap<>();
        List<Map<String, Object>> countries = new ArrayList<>();
        Map<String, Object> companyMap = new LinkedHashMap<>();

        companyMap.put("name", "companyName");
        companyMap.put("id", "companyId");

        countries.add(companyMap);

        map.put("response", countries);

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(map);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("companyName", "companyId"));

        assertEquals(
            expectedOptions,
            AcceloUtils.getCompanyIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext));
    }
}
