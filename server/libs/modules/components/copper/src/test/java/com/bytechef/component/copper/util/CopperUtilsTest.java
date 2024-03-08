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

package com.bytechef.component.copper.util;

import static com.bytechef.component.definition.Authorization.KEY;
import static com.bytechef.component.definition.Authorization.USERNAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Domiter
 */
class CopperUtilsTest {

    private final Parameters mockedParameters = mock(Parameters.class);

    @Test
    void testGetHeaders() {
        when(mockedParameters.getRequiredString(KEY))
            .thenReturn("myAccessToken");
        when(mockedParameters.getRequiredString(USERNAME))
            .thenReturn("user@example.com");

        Map<String, List<String>> expectedHeaders = Map.of(
            "X-PW-AccessToken", List.of("myAccessToken"),
            "X-PW-Application", List.of("developer_api"),
            "X-PW-UserEmail", List.of("user@example.com"),
            "Content-Type", List.of("application/json"));

        assertEquals(expectedHeaders, CopperUtils.getHeaders(mockedParameters));
    }

}
