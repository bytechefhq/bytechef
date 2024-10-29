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
import static com.bytechef.component.copper.constant.CopperConstants.CUSTOM_FIELDS;
import static com.bytechef.component.copper.constant.CopperConstants.DETAILS;
import static com.bytechef.component.copper.constant.CopperConstants.DUE_DATE;
import static com.bytechef.component.copper.constant.CopperConstants.NAME;
import static com.bytechef.component.copper.constant.CopperConstants.PRIORITY;
import static com.bytechef.component.copper.constant.CopperConstants.RELATED_RESOURCE;
import static com.bytechef.component.copper.constant.CopperConstants.REMINDER_DATE;
import static com.bytechef.component.copper.constant.CopperConstants.STATUS;
import static com.bytechef.component.copper.constant.CopperConstants.TAGS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Vihar Shah
 */
class CopperCreateTaskActionTest extends AbstractCopperActionTest {

    @Test
    void testPerform() {
        when(mockedParameters.getString(NAME))
            .thenReturn("name");
        when(mockedParameters.get(RELATED_RESOURCE))
            .thenReturn(Map.of());
        when(mockedParameters.getString(ASSIGNEE_ID))
            .thenReturn("assigneeId");
        when(mockedParameters.getString(DUE_DATE))
            .thenReturn("dueDate");
        when(mockedParameters.getString(REMINDER_DATE))
            .thenReturn("reminderDate");
        when(mockedParameters.getString(PRIORITY))
            .thenReturn("priority");
        when(mockedParameters.getString(STATUS))
            .thenReturn("status");
        when(mockedParameters.getString(DETAILS))
            .thenReturn("details");
        when(mockedParameters.getList(TAGS))
            .thenReturn(List.of());
        when(mockedParameters.getList(CUSTOM_FIELDS))
            .thenReturn(List.of());

        Object result = CopperCreateTaskAction.perform(mockedParameters, mockedParameters, mockedContext);
        assertEquals(responeseMap, result);
        verify(mockedContext, times(1)).http(POST_CREATE_TASK_FUNCTION);

        Context.Http.Body body = bodyArgumentCaptor.getValue();
        assertEquals(propertyStubsMap(), body.getContent());
    }

    private Map<String, Object> propertyStubsMap() {
        return Map.of(
            NAME, "name",
            RELATED_RESOURCE, Map.of(),
            ASSIGNEE_ID, "assigneeId",
            DUE_DATE, "dueDate",
            REMINDER_DATE, "reminderDate",
            PRIORITY, "priority",
            STATUS, "status",
            DETAILS, "details",
            TAGS, List.of(),
            CUSTOM_FIELDS, List.of());
    }
}
