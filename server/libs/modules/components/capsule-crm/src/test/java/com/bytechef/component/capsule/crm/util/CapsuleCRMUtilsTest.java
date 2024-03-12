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

package com.bytechef.component.capsule.crm.util;

import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.FIRST_NAME_PROPERTY;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.LAST_NAME_PROPERTY;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.NAME_PROPERTY;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.TYPE;
import static com.bytechef.component.definition.ComponentDSL.option;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Domiter
 */
class CapsuleCRMUtilsTest {

    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Context.Http.Executor mockedExecutor = mock(Context.Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Context.Http.Response mockedResponse = mock(Context.Http.Response.class);

    @Test
    void testCreateNamePropertiesForPerson() {
        when(mockedParameters.getRequiredString(TYPE))
                .thenReturn("person");

        List<Property.ValueProperty<?>> nameProperties = CapsuleCRMUtils.createNameProperties(
            mockedParameters, mockedParameters, mockedContext);

        assertEquals(2, nameProperties.size());

        assertEquals(FIRST_NAME_PROPERTY, nameProperties.getFirst());
        assertEquals(LAST_NAME_PROPERTY, nameProperties.get(1));
    }

    @Test
    void testCreateNamePropertiesForOrganization() {
        when(mockedParameters.getRequiredString(TYPE))
            .thenReturn("organization");

        List<Property.ValueProperty<?>> nameProperties = CapsuleCRMUtils.createNameProperties(
            mockedParameters, mockedParameters, mockedContext);

        assertEquals(1, nameProperties.size());

        assertEquals(NAME_PROPERTY, nameProperties.getFirst());
    }

    @Test
    void testGetCountryOptions() {
        Map<String, List<Map<String, Object>>> map = new LinkedHashMap<>();
        List<Map<String, Object>> countries = new ArrayList<>();
        Map<String, Object> countryMap = new LinkedHashMap<>();

        countryMap.put("name", "countryName");

        countries.add(countryMap);

        map.put("countries", countries);

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.headers(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(Context.TypeReference.class)))
            .thenReturn(map);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("countryName", "countryName"));

        assertEquals(
            expectedOptions,
            CapsuleCRMUtils.getCountryOptions(mockedParameters, mockedParameters, "", mockedContext));
    }
}
