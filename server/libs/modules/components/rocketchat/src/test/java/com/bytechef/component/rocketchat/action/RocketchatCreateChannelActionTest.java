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

package com.bytechef.component.rocketchat.action;

import static com.bytechef.component.rocketchat.constant.RocketchatConstants.EXCLUDE_SELF;
import static com.bytechef.component.rocketchat.constant.RocketchatConstants.MEMBERS;
import static com.bytechef.component.rocketchat.constant.RocketchatConstants.NAME;
import static com.bytechef.component.rocketchat.constant.RocketchatConstants.READ_ONLY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Marija Horvat
 */
class RocketchatCreateChannelActionTest extends AbstractRocketchatActionTest {

    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(NAME, "test", MEMBERS, List.of("user1", "user2"), READ_ONLY, "false", EXCLUDE_SELF, "false"));

    @Test
    void testPerform() {
        Object result = RocketchatCreateChannelAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responseMap, result);

        Context.Http.Body body = bodyArgumentCaptor.getValue();
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put(NAME, "test");
        expected.put(MEMBERS, List.of("user1", "user2"));
        expected.put(READ_ONLY, false);
        expected.put(EXCLUDE_SELF, false);

        assertEquals(expected, body.getContent());
    }
}
