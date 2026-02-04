/*
 * Copyright 2025 ByteChef
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

package com.bytechef.component.text.helper.action;

import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @author Nikolina Spehar
 */
class TextHelperUnderscoreActionTest {

    @ParameterizedTest
    @CsvSource(value = {
        "'Hello, World!', hello_world",
        "'Java@#$ Is *** Awesome!!!', java_is_awesome",
        "'  Too   many   spaces  ', too_many_spaces",
        "'Version 2 Release 10', version_2_release_10"
    })
    void testPerform(String input, String expectedResult) {
        Parameters mockedParameters = MockParametersFactory.create(Map.of(TEXT, input == null ? "" : input));

        assertEquals(expectedResult, TextHelperUnderscoreAction.perform(mockedParameters, null, null));
    }
}
