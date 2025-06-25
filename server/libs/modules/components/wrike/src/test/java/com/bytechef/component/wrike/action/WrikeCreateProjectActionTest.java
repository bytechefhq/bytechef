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

package com.bytechef.component.wrike.action;

import static com.bytechef.component.wrike.constant.WrikeConstants.BUDGET;
import static com.bytechef.component.wrike.constant.WrikeConstants.CONTRACT_TYPE;
import static com.bytechef.component.wrike.constant.WrikeConstants.DESCRIPTION;
import static com.bytechef.component.wrike.constant.WrikeConstants.END_DATE;
import static com.bytechef.component.wrike.constant.WrikeConstants.OWNER_IDS;
import static com.bytechef.component.wrike.constant.WrikeConstants.PARENT_ID;
import static com.bytechef.component.wrike.constant.WrikeConstants.PROJECT;
import static com.bytechef.component.wrike.constant.WrikeConstants.START_DATE;
import static com.bytechef.component.wrike.constant.WrikeConstants.TITLE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Nikolina Spehar
 */
class WrikeCreateProjectActionTest {

    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final ArgumentCaptor<Object[]> queryArgumentCaptor = ArgumentCaptor.forClass(Object[].class);
    private final Map<String, Object> responseMap = Map.of();
    private final LocalDate mockStartDate = LocalDate.of(2025, 6, 18);
    private final LocalDate mockEndDate = LocalDate.of(2025, 6, 19);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            PARENT_ID, "parentId",
            TITLE, "title",
            DESCRIPTION, "description",
            START_DATE, mockStartDate,
            END_DATE, mockEndDate,
            CONTRACT_TYPE, "Billable",
            OWNER_IDS, List.of("id"),
            BUDGET, 100));

    @Test
    void testPerform() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody())
            .thenReturn(responseMap);

        Object result = WrikeCreateProjectAction.perform(
            mockedParameters, mockedParameters, mockedContext);

        assertEquals(Map.of(), result);

        Object[] queryParameters = queryArgumentCaptor.getValue();

        Map<String, Object> expectedProject = Map.of(
            START_DATE, LocalDate.of(2025, 6, 18),
            END_DATE, LocalDate.of(2025, 6, 19),
            CONTRACT_TYPE, "Billable",
            OWNER_IDS, List.of("id"),
            BUDGET, 100);

        Object[] expectedQueryParameters = {
            TITLE, "title", DESCRIPTION, "description", PROJECT, expectedProject
        };

        assertArrayEquals(expectedQueryParameters, queryParameters);
    }
}
