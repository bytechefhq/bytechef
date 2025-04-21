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

package com.bytechef.component.bamboohr.action;

import static com.bytechef.component.bamboohr.constant.BambooHrConstants.EMPLOYEE_NUMBER;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.EMPLOYMENT_STATUS;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.FIRST_NAME;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.HIRE_DATE;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.ID;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.JOB_TITLE;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.LAST_NAME;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.LOCATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BambooHrCreateEmployeeActionTest {

    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            EMPLOYEE_NUMBER, "1", FIRST_NAME, "test", LAST_NAME, "test",
            JOB_TITLE, "Software Engineer", LOCATION, "London, UK",
            EMPLOYMENT_STATUS, "Full-Time", HIRE_DATE, "04/01/2025"));

    @Test
    void testPerform() {
        String url = "https://api.bamboohr.com/api/gateway.php/bytechef/v1/employees/1";

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getHeader(stringArgumentCaptor.capture()))
            .thenReturn(List.of(url));

        Object result = BambooHrCreateEmployeeAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(Map.of("url", url, ID, "1"), result);
        assertEquals("location", stringArgumentCaptor.getValue());

        Map<String, Object> expectedBody = Map.of(
            EMPLOYEE_NUMBER, "1", FIRST_NAME, "test", LAST_NAME, "test",
            JOB_TITLE, "Software Engineer", LOCATION, "London, UK",
            EMPLOYMENT_STATUS, "Full-Time", HIRE_DATE, "04/01/2025");

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(expectedBody, body.getContent());
    }
}
