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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class TextHelperGetFirstMiddleLastNameActionTest {

    private final Context mockedContext = mock(Context.class);
    private final Parameters mockedParameters = mock(Parameters.class);

    @Test
    void testPerformWithTwoPartNameFirstNameFirst() {
        when(mockedParameters.getRequiredString(FULL_NAME)).thenReturn("John Doe");
        when(mockedParameters.getRequiredBoolean(IS_FIRST_NAME_FIRST)).thenReturn(true);

        Map<String, String> result = TextHelperGetFirstMiddleLastNameAction.perform(
            mockedParameters, mockedParameters, mockedContext);

        assertEquals("John", result.get(FIRST_NAME));
        assertEquals("", result.get(MIDDLE_NAME));
        assertEquals("Doe", result.get(LAST_NAME));
    }

    @Test
    void testPerformWithTwoPartNameLastNameFirst() {
        when(mockedParameters.getRequiredString(FULL_NAME)).thenReturn("Doe John");
        when(mockedParameters.getRequiredBoolean(IS_FIRST_NAME_FIRST)).thenReturn(false);

        Map<String, String> result = TextHelperGetFirstMiddleLastNameAction.perform(
            mockedParameters, mockedParameters, mockedContext);

        assertEquals("John", result.get(FIRST_NAME));
        assertEquals("", result.get(MIDDLE_NAME));
        assertEquals("Doe", result.get(LAST_NAME));
    }

    @Test
    void testPerformWithThreePartNameFirstNameFirst() {
        when(mockedParameters.getRequiredString(FULL_NAME)).thenReturn("John Michael Doe");
        when(mockedParameters.getRequiredBoolean(IS_FIRST_NAME_FIRST)).thenReturn(true);

        Map<String, String> result = TextHelperGetFirstMiddleLastNameAction.perform(
            mockedParameters, mockedParameters, mockedContext);

        assertEquals("John", result.get(FIRST_NAME));
        assertEquals("Michael", result.get(MIDDLE_NAME));
        assertEquals("Doe", result.get(LAST_NAME));
    }

    @Test
    void testPerformWithThreePartNameLastNameFirst() {
        when(mockedParameters.getRequiredString(FULL_NAME)).thenReturn("Doe Michael John");
        when(mockedParameters.getRequiredBoolean(IS_FIRST_NAME_FIRST)).thenReturn(false);

        Map<String, String> result = TextHelperGetFirstMiddleLastNameAction.perform(
            mockedParameters, mockedParameters, mockedContext);

        assertEquals("John", result.get(FIRST_NAME));
        assertEquals("Michael", result.get(MIDDLE_NAME));
        assertEquals("Doe", result.get(LAST_NAME));
    }

    @Test
    void testPerformNamesWithMultipleSpaces() {
        when(mockedParameters.getRequiredString(FULL_NAME)).thenReturn("John  Doe");
        when(mockedParameters.getRequiredBoolean(IS_FIRST_NAME_FIRST)).thenReturn(true);

        Map<String, String> result = TextHelperGetFirstMiddleLastNameAction.perform(
            mockedParameters, mockedParameters, mockedContext);

        assertNotNull(result);
    }

    @Test
    void testPerformThrowExceptionForSinglePartName() {
        when(mockedParameters.getRequiredString(FULL_NAME)).thenReturn("John");
        when(mockedParameters.getRequiredBoolean(IS_FIRST_NAME_FIRST)).thenReturn(true);

        assertThrows(ArrayIndexOutOfBoundsException.class,
            () -> TextHelperGetFirstMiddleLastNameAction.perform(
                mockedParameters, mockedParameters, mockedContext));
    }

    @Test
    void testPerformHandleEmptyString() {
        when(mockedParameters.getRequiredString(FULL_NAME)).thenReturn("");
        when(mockedParameters.getRequiredBoolean(IS_FIRST_NAME_FIRST)).thenReturn(true);

        assertThrows(ArrayIndexOutOfBoundsException.class,
            () -> TextHelperGetFirstMiddleLastNameAction.perform(
                mockedParameters, mockedParameters, mockedContext));
    }

    @Test
    void testPerformNamesWithSpecialCharacters() {
        when(mockedParameters.getRequiredString(FULL_NAME)).thenReturn("Jean-Paul Sartre");
        when(mockedParameters.getRequiredBoolean(IS_FIRST_NAME_FIRST)).thenReturn(true);

        Map<String, String> result = TextHelperGetFirstMiddleLastNameAction.perform(
            mockedParameters, mockedParameters, mockedContext);

        assertEquals("Jean-Paul", result.get(FIRST_NAME));
        assertEquals("", result.get(MIDDLE_NAME));
        assertEquals("Sartre", result.get(LAST_NAME));
    }

    @Test
    void shouldVerifyAllMapKeysPresent() {
        when(mockedParameters.getRequiredString(FULL_NAME)).thenReturn("John Doe");
        when(mockedParameters.getRequiredBoolean(IS_FIRST_NAME_FIRST)).thenReturn(true);

        Map<String, String> result = TextHelperGetFirstMiddleLastNameAction.perform(
            mockedParameters, mockedParameters, mockedContext);

        assertTrue(result.containsKey(FIRST_NAME));
        assertTrue(result.containsKey(MIDDLE_NAME));
        assertTrue(result.containsKey(LAST_NAME));
        assertEquals(3, result.size());
    }

    @Test
    void shouldNotModifyMiddleNameForTwoPartNames() {
        when(mockedParameters.getRequiredString(FULL_NAME)).thenReturn("John Doe");
        when(mockedParameters.getRequiredBoolean(IS_FIRST_NAME_FIRST)).thenReturn(true);

        Map<String, String> result = TextHelperGetFirstMiddleLastNameAction.perform(
            mockedParameters, mockedParameters, mockedContext);

        assertEquals("", result.get(MIDDLE_NAME));
        assertNotNull(result.get(MIDDLE_NAME));
    }
}
