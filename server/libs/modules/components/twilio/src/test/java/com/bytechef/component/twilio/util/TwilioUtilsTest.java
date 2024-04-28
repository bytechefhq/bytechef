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

package com.bytechef.component.twilio.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Domiter
 */
class TwilioUtilsTest {

    private final Parameters mockedParameters = mock(Parameters.class);
    private final ActionContext mockedContext = mock(ActionContext.class);

    @Test
    void testGetZoneIdOptions() {
        List<Option<String>> zoneIdOptions = TwilioUtils.getZoneIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(27, zoneIdOptions.size());
    }
}
