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

package com.bytechef.component.util;

import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.COMPANY_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.quickbooks.util.QuickbooksUtils;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class QuickbooksUtilsTest {

    private final Parameters mockedParameters = mock(Parameters.class);

    @Test
    void testGetCompanyId() {
        when(mockedParameters.getRequiredString(COMPANY_ID))
            .thenReturn("123 456 abc");

        assertEquals("123456abc", QuickbooksUtils.getCompanyId(mockedParameters));
    }
}
