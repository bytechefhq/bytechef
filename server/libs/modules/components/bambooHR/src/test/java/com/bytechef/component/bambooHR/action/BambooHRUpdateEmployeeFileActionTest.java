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

package com.bytechef.component.bambooHR.action;

import static com.bytechef.component.bambooHR.constant.BambooHRConstants.FILE_ID;
import static com.bytechef.component.bambooHR.constant.BambooHRConstants.ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Marija Horvat
 */
class BambooHRUpdateEmployeeFileActionTest extends AbstractBambooHRActionTest {

    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            ID, "1", FILE_ID, "1", "name", "test",
            "categoryId", "1", "shareWithEmployee", "true"));

    @Test
    void testPerform() {
        Object result = BambooHRUpdateEmployeeFileAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responseMap, result);

        Context.Http.Body body = bodyArgumentCaptor.getValue();
        Map<String, Object> expected = Map.of(
            "name", "test", "categoryId", "1", "shareWithEmployee", "true");
        assertEquals(expected, body.getContent());
    }
}
