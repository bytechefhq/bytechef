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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class TextHelperIsNumericActionTest {

    private static boolean run(String text) {
        Parameters mockedParameters = MockParametersFactory.create(Map.of(TEXT, text));

        return TextHelperIsNumericAction.perform(mockedParameters, null, null);
    }

    @Test
    void shouldAcceptInteger() {
        assertTrue(run("123"));
    }

    @Test
    void shouldAcceptNegativeInteger() {
        assertTrue(run("-123"));
    }

    @Test
    void shouldAcceptDecimal() {
        assertTrue(run("123.45"));
    }

    @Test
    void shouldAcceptNegativeDecimal() {
        assertTrue(run("-123.45"));
    }

    @Test
    void shouldAcceptLeadingPlus() {
        assertTrue(run("+123"));
    }

    @Test
    void shouldAcceptScientificNotation() {
        assertTrue(run("1e10"));
        assertTrue(run("1E-10"));
    }

    @Test
    void shouldAcceptHexadecimal() {
        assertTrue(run("0xFF"));
        assertTrue(run("-0x1A"));
    }

    @Test
    void shouldAcceptZeroVariants() {
        assertTrue(run("0"));
        assertTrue(run("0.0"));
    }

    @Test
    void shouldRejectEmptyString() {
        assertFalse(run(""));
    }

    @Test
    void shouldRejectBlankString() {
        assertFalse(run(" "));
        assertFalse(run("\t"));
    }

    @Test
    void shouldRejectAlphabeticText() {
        assertFalse(run("abc"));
    }

    @Test
    void shouldRejectAlphanumericMix() {
        assertFalse(run("123abc"));
        assertFalse(run("abc123"));
    }

    @Test
    void shouldRejectMultipleDecimals() {
        assertFalse(run("1.2.3"));
    }

    @Test
    void shouldRejectMultipleSigns() {
        assertFalse(run("--1"));
        assertFalse(run("+-1"));
    }

    @Test
    void shouldRejectMalformedScientificNotation() {
        assertFalse(run("1e"));
        assertFalse(run("e10"));
        assertFalse(run("1e1.0"));
    }

    @Test
    void shouldRejectOnlyDecimalPoint() {
        assertFalse(run("."));
    }
}
