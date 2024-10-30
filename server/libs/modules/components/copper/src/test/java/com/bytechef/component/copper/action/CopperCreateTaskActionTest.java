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

package com.bytechef.component.copper.action;

import static com.bytechef.component.copper.action.CopperCreateTaskAction.POST_CREATE_TASK_FUNCTION;
import static com.bytechef.component.copper.constant.CopperConstants.ASSIGNEE_ID;
import static com.bytechef.component.copper.constant.CopperConstants.DETAILS;
import static com.bytechef.component.copper.constant.CopperConstants.DUE_DATE;
import static com.bytechef.component.copper.constant.CopperConstants.NAME;
import static com.bytechef.component.copper.constant.CopperConstants.PRIORITY;
import static com.bytechef.component.copper.constant.CopperConstants.REMINDER_DATE;
import static com.bytechef.component.copper.constant.CopperConstants.STATUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Vihar Shah
 * @author Monika Kušter
 */
class CopperCreateTaskActionTest extends AbstractCopperActionTest {

    @Test
    void testPerform() {
        Date date = new Date();

        long epochSecond = date.toInstant()
            .getEpochSecond();

        mockedParameters = MockParametersFactory.create(
            Map.of(
                NAME, "name", ASSIGNEE_ID, "assigneeId", DUE_DATE, date, REMINDER_DATE, date,
                PRIORITY, "priority", STATUS, "status", DETAILS, "details"));

        Object result = CopperCreateTaskAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedObject, result);

        verify(mockedContext, times(1)).http(POST_CREATE_TASK_FUNCTION);

        Http.Body body = bodyArgumentCaptor.getValue();

        Map<String, Object> expectedBody = Map.of(
            NAME, "name",
            ASSIGNEE_ID, "assigneeId",
            DUE_DATE, epochSecond,
            REMINDER_DATE, epochSecond,
            PRIORITY, "priority",
            STATUS, "status",
            DETAILS, "details");

        assertEquals(expectedBody, body.getContent());
    }
}
