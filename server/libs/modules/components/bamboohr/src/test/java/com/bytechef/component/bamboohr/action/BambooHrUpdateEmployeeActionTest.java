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

import static com.bytechef.component.bamboohr.constant.BambooHrConstants.EMPLOYMENT_STATUS;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.FIRST_NAME;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.HIRE_DATE;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.ID;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.JOB_TITLE;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.LAST_NAME;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.LOCATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
class BambooHrUpdateEmployeeActionTest {

    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            ID, "1", FIRST_NAME, "test", LAST_NAME, "test",
            JOB_TITLE, "Software Engineer", LOCATION, "London, UK",
            EMPLOYMENT_STATUS, "Full-Time", HIRE_DATE, "04/01/2025"));

    @Test
    void testPerform() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);

        Object result = BambooHrUpdateEmployeeAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertNull(result);

        Http.Body body = bodyArgumentCaptor.getValue();
        Map<String, Object> expectedBody = Map.of(
            FIRST_NAME, "test", LAST_NAME, "test",
            JOB_TITLE, "Software Engineer", LOCATION, "London, UK",
            EMPLOYMENT_STATUS, "Full-Time", HIRE_DATE, "04/01/2025");

        assertEquals(expectedBody, body.getContent());
    }
}
