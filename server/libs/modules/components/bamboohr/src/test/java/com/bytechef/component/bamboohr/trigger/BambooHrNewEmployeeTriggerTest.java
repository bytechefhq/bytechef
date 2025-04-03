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

package com.bytechef.component.bamboohr.trigger;

import static com.bytechef.component.bamboohr.constant.BambooHrConstants.EMPLOYEE_NUMBER;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.FIELDS;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.FIRST_NAME;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.ID;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.LAST_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
class BambooHrNewEmployeeTriggerTest {

    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(FIELDS, List.of("firstName", "lastName", "employeeNumber")));

    @Test
    void testPoll() {

        BambooHrNewEmployeeTrigger.EMPLOYEE_LIST.add("1");

        Map<String, String> employee1 =
            Map.of(ID, "2", FIRST_NAME, "test", LAST_NAME, "test", EMPLOYEE_NUMBER, "2");

        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.header(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("employees", List.of(employee1)));

        TriggerDefinition.PollOutput pollOutput =
            BambooHrNewEmployeeTrigger.poll(mockedParameters, mockedParameters, mockedParameters, mockedTriggerContext);

        assertEquals(
            List.of("accept", "application/json", FIELDS, "firstName,lastName,employeeNumber", "accept",
                "application/json"),
            stringArgumentCaptor.getAllValues());

        assertEquals(Map.of("employees", List.of(employee1)), pollOutput.records()
            .getFirst());
        assertFalse(pollOutput.pollImmediately());

    }
}
