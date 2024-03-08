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

package com.bytechef.component.freshsales.util;

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
class FreshsalesUtilsTest {

    private final Parameters mockedParameters = mock(Parameters.class);

    @Test
    void testGetHeaders() {
        when(mockedParameters.getRequiredString(KEY))
            .thenReturn("key");

        Map<String, List<String>> expectedHeaders = Map.of(
            "Content-Type", List.of("application/json"),
            "Authorization", List.of("Token token=key"));

        assertEquals(expectedHeaders, FreshsalesUtils.getHeaders(mockedParameters));
    }

    @Test
    void testGetUrl() {
        when(mockedParameters.getRequiredString(USERNAME))
            .thenReturn("username");

        String actualUrl = FreshsalesUtils.getUrl(mockedParameters, "resource");

        assertEquals("https://username.myfreshworks.com/crm/sales/api/resource", actualUrl);
    }
}
