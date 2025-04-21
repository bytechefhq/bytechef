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

import static com.bytechef.component.bamboohr.constant.BambooHrConstants.CATEGORY_ID;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.FILE_ID;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.ID;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.NAME;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.SHARE_WITH_EMPLOYEE;
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
class BambooHrUpdateEmployeeFileActionTest {

    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(ID, "1", FILE_ID, "1", NAME, "test", CATEGORY_ID, "1", SHARE_WITH_EMPLOYEE, "true"));

    @Test
    void testPerform() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);

        Object result = BambooHrUpdateEmployeeFileAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertNull(result);

        Http.Body body = bodyArgumentCaptor.getValue();
        Map<String, Object> expectedBody = Map.of(NAME, "test", CATEGORY_ID, "1", SHARE_WITH_EMPLOYEE, "true");

        assertEquals(expectedBody, body.getContent());
    }
}
