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

package com.bytechef.component.one.simple.api.util;

import static com.bytechef.component.one.simple.api.util.OneSimpleAPIUtils.getCurrencyOptions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Luka Ljubić
 */
class OneSimpleAPIUtilsTest {

    private static final int EXPECTED_OPTIONS_SIZE = 14;
    private static final String EXPECTED_FIRST_OPTION_VALUE = "EUR";
    private static final String EXPECTED_FIRST_OPTION_LABEL = "Euro";
    private static final String EXPECTED_LAST_OPTION_VALUE = "USD";
    private static final String EXPECTED_LAST_OPTION_LABEL = "United States Dollar";

    @Test
    void testGetCurrencyOptions() {
        Parameters parameters = mock(Parameters.class);
        ActionContext context = mock(ActionContext.class);

        List<Option<String>> options = getCurrencyOptions(parameters, parameters, null, "", context);

        assertNotNull(options, "Returned options list should not be null.");

        assertEquals(EXPECTED_OPTIONS_SIZE, options.size(),
            "The returned options size does not match the expected size.");

        Option<String> firstOption = options.get(0);
        assertEquals(EXPECTED_FIRST_OPTION_VALUE, firstOption.getValue(),
            "First option value does not match.");
        assertEquals(EXPECTED_FIRST_OPTION_LABEL, firstOption.getLabel(),
            "First option label does not match.");

        Option<String> lastOption = options.get(options.size() - 1);
        assertEquals(EXPECTED_LAST_OPTION_VALUE, lastOption.getValue(),
            "Last option value does not match.");
        assertEquals(EXPECTED_LAST_OPTION_LABEL, lastOption.getLabel(),
            "Last option label does not match.");
    }
}
