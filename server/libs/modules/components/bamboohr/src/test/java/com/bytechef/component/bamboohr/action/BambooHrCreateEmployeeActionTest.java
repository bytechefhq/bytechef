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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockContextSetupExtension.class)
class BambooHrCreateEmployeeActionTest {

    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            EMPLOYEE_NUMBER, "1", FIRST_NAME, "test", LAST_NAME, "test",
            JOB_TITLE, "Software Engineer", LOCATION, "London, UK",
            EMPLOYMENT_STATUS, "Full-Time", HIRE_DATE, "04/01/2025"));

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor) {

        String url = "https://api.bamboohr.com/api/gateway.php/bytechef/v1/employees/1";

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getHeader(stringArgumentCaptor.capture()))
            .thenReturn(List.of(url));

        Object result = BambooHrCreateEmployeeAction.perform(mockedParameters, null, mockedContext);

        assertEquals(Map.of("url", url, ID, "1"), result);

        ContextFunction<Http, Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);
        assertEquals(List.of("/employees/", "location"), stringArgumentCaptor.getAllValues());

        Map<String, Object> expectedBody = Map.of(
            EMPLOYEE_NUMBER, "1", FIRST_NAME, "test", LAST_NAME, "test",
            JOB_TITLE, "Software Engineer", LOCATION, "London, UK",
            EMPLOYMENT_STATUS, "Full-Time", HIRE_DATE, "04/01/2025");

        Body body = bodyArgumentCaptor.getValue();

        assertEquals(expectedBody, body.getContent());
    }
}
