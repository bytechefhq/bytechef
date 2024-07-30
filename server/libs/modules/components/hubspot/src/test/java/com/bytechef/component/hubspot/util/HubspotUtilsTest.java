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

package com.bytechef.component.hubspot.util;

import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.hubspot.constant.HubspotConstants.ID;
import static com.bytechef.component.hubspot.constant.HubspotConstants.LABEL;
import static com.bytechef.component.hubspot.constant.HubspotConstants.RESULTS;
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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class HubspotUtilsTest {

    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @BeforeEach
    void beforeEach() {
        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
    }

    @Test
    void testGetContactsOptions() {
        Map<String, Object> propertiesMap = Map.of("firstname", "first", "lastname", "last");

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(RESULTS, List.of(Map.of(ID, "123", "properties", propertiesMap))));

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("first last", "123"));

        assertEquals(
            expectedOptions,
            HubspotUtils.getContactsOptions(mockedParameters, mockedParameters, Map.of(), "", mockedActionContext));
    }

    @Test
    void testGetDealStageOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(RESULTS, List.of(Map.of(ID, "123", LABEL, "label"))));

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("label", "123"));

        assertEquals(
            expectedOptions,
            HubspotUtils.getDealStageOptions(mockedParameters, mockedParameters, Map.of(), "", mockedActionContext));
    }

    @Test
    void testGetOwnerOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(RESULTS, List.of(Map.of(ID, "123", "email", "label"))));

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("email", "123"));

        assertEquals(
            expectedOptions,
            HubspotUtils.getOwnerOptions(mockedParameters, mockedParameters, Map.of(), "", mockedActionContext));
    }

    @Test
    void testGetPipelineDealOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(RESULTS, List.of(Map.of(ID, "123", LABEL, "label"))));

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("label", "123"));

        assertEquals(
            expectedOptions,
            HubspotUtils.getPipelineDealOptions(mockedParameters, mockedParameters, Map.of(), "", mockedActionContext));
    }

}
