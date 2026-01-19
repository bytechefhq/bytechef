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

import static com.bytechef.component.text.helper.constant.TextHelperConstants.FIRST_NAME;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.FULL_NAME;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.IS_FIRST_NAME_FIRST;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.LAST_NAME;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.MIDDLE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class TextHelperGetFirstMiddleLastNameActionTest {

    private Parameters mockedParameters;

    @Test
    void testPerformWithTwoPartNameFirstNameFirst() {
        mockedParameters = MockParametersFactory.create(Map.of(FULL_NAME, "John Doe", IS_FIRST_NAME_FIRST, true));

        Map<String, String> result = TextHelperGetFirstMiddleLastNameAction.perform(mockedParameters, null, null);

        assertEquals(Map.of(FIRST_NAME, "John", MIDDLE_NAME, "", LAST_NAME, "Doe"), result);
    }

    @Test
    void testPerformWithTwoPartNameLastNameFirst() {
        mockedParameters = MockParametersFactory.create(Map.of(FULL_NAME, "Doe John", IS_FIRST_NAME_FIRST, false));

        Map<String, String> result = TextHelperGetFirstMiddleLastNameAction.perform(mockedParameters, null, null);

        assertEquals(Map.of(FIRST_NAME, "John", MIDDLE_NAME, "", LAST_NAME, "Doe"), result);
    }

    @Test
    void testPerformWithThreePartNameFirstNameFirst() {
        mockedParameters = MockParametersFactory.create(
            Map.of(FULL_NAME, "John Michael Doe", IS_FIRST_NAME_FIRST, true));

        Map<String, String> result = TextHelperGetFirstMiddleLastNameAction.perform(mockedParameters, null, null);

        assertEquals(Map.of(FIRST_NAME, "John", MIDDLE_NAME, "Michael", LAST_NAME, "Doe"), result);
    }

    @Test
    void testPerformWithThreePartNameLastNameFirst() {
        mockedParameters = MockParametersFactory.create(
            Map.of(FULL_NAME, "Doe Michael John", IS_FIRST_NAME_FIRST, false));

        Map<String, String> result = TextHelperGetFirstMiddleLastNameAction.perform(mockedParameters, null, null);

        assertEquals(Map.of(FIRST_NAME, "John", MIDDLE_NAME, "Michael", LAST_NAME, "Doe"), result);
    }

    @Test
    void testPerformNamesWithMultipleSpaces() {
        mockedParameters = MockParametersFactory.create(Map.of(FULL_NAME, "John  Doe", IS_FIRST_NAME_FIRST, true));

        Map<String, String> result = TextHelperGetFirstMiddleLastNameAction.perform(mockedParameters, null, null);

        assertEquals(Map.of(FIRST_NAME, "John", MIDDLE_NAME, "", LAST_NAME, "Doe"), result);
    }

    @Test
    void testPerformThrowExceptionForSinglePartName() {
        mockedParameters = MockParametersFactory.create(Map.of(FULL_NAME, "John", IS_FIRST_NAME_FIRST, true));

        assertThrows(ArrayIndexOutOfBoundsException.class,
            () -> TextHelperGetFirstMiddleLastNameAction.perform(mockedParameters, null, null));
    }

    @Test
    void testPerformHandleEmptyString() {
        mockedParameters = MockParametersFactory.create(Map.of(FULL_NAME, "", IS_FIRST_NAME_FIRST, true));

        assertThrows(ArrayIndexOutOfBoundsException.class,
            () -> TextHelperGetFirstMiddleLastNameAction.perform(mockedParameters, null, null));
    }

    @Test
    void testPerformNamesWithSpecialCharacters() {
        mockedParameters = MockParametersFactory.create(
            Map.of(FULL_NAME, "Jean-Paul Sartre", IS_FIRST_NAME_FIRST, true));

        Map<String, String> result = TextHelperGetFirstMiddleLastNameAction.perform(mockedParameters, null, null);

        assertEquals(Map.of(FIRST_NAME, "Jean-Paul", MIDDLE_NAME, "", LAST_NAME, "Sartre"), result);
    }
}
