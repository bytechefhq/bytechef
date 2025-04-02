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

import static com.bytechef.component.bamboohr.constant.BambooHrConstants.EMPLOYEE_NUMBER;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.EMPLOYMENT_STATUS;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.FIRST_NAME;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.HIRE_DATE;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.JOB_TITLE;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.LAST_NAME;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.LOCATION;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BambooHrCreateEmployeeActionTest extends AbstractBambooHRActionTest {

    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            EMPLOYEE_NUMBER, "1", FIRST_NAME, "test", LAST_NAME, "test",
            JOB_TITLE, "Software Engineer", LOCATION, "London, UK",
            EMPLOYMENT_STATUS, "Full-Time", HIRE_DATE, "04/01/2025"));

    @Test
    void testPerform() {
        Object result = BambooHrCreateEmployeeAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responseMap, result);

        Http.Body body = bodyArgumentCaptor.getValue();
        Map<String, Object> expected = Map.of(
            EMPLOYEE_NUMBER, "1", FIRST_NAME, "test", LAST_NAME, "test",
            JOB_TITLE, "Software Engineer", LOCATION, "London, UK",
            EMPLOYMENT_STATUS, "Full-Time", HIRE_DATE, "04/01/2025");
        assertEquals(expected, body.getContent());
    }
}
