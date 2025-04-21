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

package com.bytechef.component.mautic.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.mautic.constant.MauticConstants.ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class MauticUtilsTest {

    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of());
    private final Response mockedResponse = mock(Response.class);
    private final List<Map<String, Object>> responseList = List.of(Map.of(ID, "id"));

    @Test
    void getCompanyOptions() {
        testGetOptions("company");
    }

    @Test
    void getContactOptions() {
        testGetOptions("contact");
    }

    private void testGetOptions(String option) {
        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseList);

        List<Option<String>> options = httpCall(option);

        List<Option<String>> expectedOptions = List.of(
            option("id", "id"));

        assertEquals(expectedOptions, options);
    }

    private List<Option<String>> httpCall(String option) {
        List<Option<String>> options = new ArrayList<>();

        if (option.equals("company")) {
            options = MauticUtils.getCompanyOptions(
                mockedParameters, mockedParameters, Map.of(), anyString(), mockedActionContext);
        }

        if (option.equals("contact")) {
            options = MauticUtils.getContactOptions(
                mockedParameters, mockedParameters, Map.of(), anyString(), mockedActionContext);
        }

        return options;
    }
}
