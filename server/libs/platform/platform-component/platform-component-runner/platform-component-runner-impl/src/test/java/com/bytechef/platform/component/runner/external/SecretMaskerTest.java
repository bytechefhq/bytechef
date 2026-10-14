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

package com.bytechef.platform.component.runner.external;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class SecretMaskerTest {

    @Test
    void testAValueIsReplacedEverywhereItOccurs() {
        SecretMasker secretMasker = SecretMasker.of(List.of("xoxb-secret"));

        assertThat(secretMasker.mask("token=xoxb-secret and again xoxb-secret"))
            .isEqualTo("token=****** and again ******");
    }

    @Test
    void testValuesShorterThanSixCharactersAreNotMasked() {
        SecretMasker secretMasker = SecretMasker.of(List.of("true", "abcde", "abcdef"));

        assertThat(secretMasker.mask("true abcde abcdef")).isEqualTo("true abcde ******");
    }

    @Test
    void testTheLongestValueIsMaskedFirst() {
        SecretMasker secretMasker = SecretMasker.of(List.of("secret", "secret-and-more"));

        assertThat(secretMasker.mask("x secret-and-more y secret z")).isEqualTo("x ****** y ****** z");
    }

    @Test
    void testNullsAndDuplicatesAreIgnored() {
        SecretMasker secretMasker = SecretMasker.of(Arrays.asList(null, "abcdef", "abcdef"));

        assertThat(secretMasker.mask("abcdef")).isEqualTo("******");
    }

    @Test
    void testEachLineOfAMultiLineValueIsMaskedAndSoIsTheWholeValue() {
        String pem = "-----BEGIN KEY-----\nabcdefghijkl\n-----END KEY-----";

        SecretMasker secretMasker = SecretMasker.of(List.of(pem));

        assertThat(secretMasker.mask("-----BEGIN KEY-----")).isEqualTo("******");
        assertThat(secretMasker.mask("abcdefghijkl")).isEqualTo("******");
        assertThat(secretMasker.mask("-----END KEY-----")).isEqualTo("******");
        assertThat(secretMasker.mask("x " + pem + " y")).isEqualTo("x ****** y");
    }

    @Test
    void testNoneLeavesTextUntouched() {
        assertThat(SecretMasker.NONE.mask("anything at all")).isEqualTo("anything at all");
    }
}
