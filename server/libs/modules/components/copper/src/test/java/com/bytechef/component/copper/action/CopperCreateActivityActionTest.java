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

import static com.bytechef.component.copper.action.CopperCreateActivityAction.POST_ACTIVITIES_CONTEXT_FUNCTION;
import static com.bytechef.component.copper.constant.CopperConstants.ACTIVITY_TYPE;
import static com.bytechef.component.copper.constant.CopperConstants.DETAILS;
import static com.bytechef.component.copper.constant.CopperConstants.ID;
import static com.bytechef.component.copper.constant.CopperConstants.PARENT;
import static com.bytechef.component.copper.constant.CopperConstants.TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class CopperCreateActivityActionTest extends AbstractCopperActionTest {

    @Test
    void testPerform() {
        mockedParameters = MockParametersFactory.create(
            Map.of(ACTIVITY_TYPE, "activityType", DETAILS, "details", TYPE, "lead", ID, "id"));

        Object result = CopperCreateActivityAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedObject, result);

        verify(mockedContext, times(1)).http(POST_ACTIVITIES_CONTEXT_FUNCTION);

        Http.Body body = bodyArgumentCaptor.getValue();

        Map<String, Object> expectedBody = Map.of(
            TYPE, Map.of("category", "user", ID, "activityType"),
            DETAILS, "details",
            PARENT, Map.of(ID, "id", TYPE, "lead"));

        assertEquals(expectedBody, body.getContent());
    }
}
