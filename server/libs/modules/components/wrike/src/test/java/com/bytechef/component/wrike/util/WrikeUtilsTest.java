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

package com.bytechef.component.wrike.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.wrike.constant.WrikeConstants.DATA;
import static com.bytechef.component.wrike.constant.WrikeConstants.FIRST_NAME;
import static com.bytechef.component.wrike.constant.WrikeConstants.ID;
import static com.bytechef.component.wrike.constant.WrikeConstants.PARENT;
import static com.bytechef.component.wrike.constant.WrikeConstants.TITLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class WrikeUtilsTest {

    private final Context mockedContext = mock(Context.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(PARENT, "parent"));
    private final Response mockedResponse = mock(Response.class);

    @Test
    void testGetContactIdOptions() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(DATA, List.of(Map.of(FIRST_NAME, "firstName", ID, "id"))));

        List<Option<String>> result = WrikeUtils.getContactIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        List<Option<String>> expected = List.of(option("firstName", "id"));

        assertEquals(expected, result);
    }

    @Test
    void testGetParentIdOptions() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(DATA, List.of(
                Map.of(TITLE, "title1", ID, "id1", "scope", "scope"),
                Map.of(TITLE, "title2", ID, "id2", "scope", "RbFolder"))));

        List<Option<String>> result = WrikeUtils.getParentIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        List<Option<String>> expected = List.of(option("title1", "id1"));

        assertEquals(expected, result);
    }
}
