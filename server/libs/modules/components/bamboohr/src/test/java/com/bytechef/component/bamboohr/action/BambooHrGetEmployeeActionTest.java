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

package com.bytechef.component.bamboohr.action;

import static com.bytechef.component.bamboohr.constant.BambooHrConstants.ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
class BambooHrGetEmployeeActionTest extends AbstractBambooHRActionTest {

    private final Parameters mockedParameters = mock(Parameters.class);
    private final ArgumentCaptor<Map<String, List<String>>> queryArgumentCaptor = ArgumentCaptor.forClass(Map.class);

    @Test
    void testPerform() {
        when(mockedExecutor.queryParameters(queryArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.headers(any()))
            .thenReturn(mockedExecutor);

        when(mockedParameters.getRequiredString(ID)).thenReturn("1");
        when(mockedParameters.getArray("fields")).thenReturn(new String[] {
            "firstName", "lastName", "employeeNumber"
        });

        Object result = BambooHrGetEmployeeAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responseMap, result);

        Map<String, List<String>> query = queryArgumentCaptor.getValue();
        assertEquals(Map.of("fields", List.of("firstName,lastName,employeeNumber")), query);
    }
}
