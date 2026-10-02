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

package com.bytechef.platform.ai.sensitivedata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.platform.ai.sensitivedata.CustomPatternValidator.CustomPatternRejectedException;
import com.bytechef.platform.ai.sensitivedata.CustomPatternValidator.Defence;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * @author Ivica Cardic
 */
class CustomPatternValidatorTest {

    @Test
    void testABoundedPatternIsAccepted() {
        assertThatCode(() -> CustomPatternValidator.validate("ACME_ACCOUNT_ID", "\\bACME-\\d{4}-[A-Z]{2}\\b"))
            .doesNotThrowAnyException();
    }

    @Test
    void testAnUnboundedPlusIsRejected() {
        assertThatThrownBy(() -> CustomPatternValidator.validate("ACME_ID", "\\bACME-\\d+\\b"))
            .isInstanceOf(CustomPatternRejectedException.class)
            .hasMessageContaining("upper bound")
            .extracting(exception -> ((CustomPatternRejectedException) exception).getDefence())
            .isEqualTo(Defence.SYNTAX);
    }

    @Test
    void testAnUnboundedStarIsRejected() {
        assertThatThrownBy(() -> CustomPatternValidator.validate("ACME_ID", "\\bACME-[0-9]*\\b"))
            .isInstanceOf(CustomPatternRejectedException.class)
            .hasMessageContaining("upper bound");
    }

    @Test
    void testAnOpenEndedRangeIsRejected() {
        assertThatThrownBy(() -> CustomPatternValidator.validate("ACME_ID", "\\bACME-\\d{8,}\\b"))
            .isInstanceOf(CustomPatternRejectedException.class)
            .hasMessageContaining("{8,}");
    }

    @Test
    void testAQuantifierCharacterInsideACharacterClassIsALiteral() {
        assertThatCode(() -> CustomPatternValidator.validate("ACME_SIGN", "\\bACME[a+b]{1,4}\\b"))
            .doesNotThrowAnyException();
    }

    @Test
    void testAnEscapedQuantifierCharacterIsALiteral() {
        assertThatCode(() -> CustomPatternValidator.validate("ACME_LITERAL", "\\bACME\\+\\d{1,4}\\b"))
            .doesNotThrowAnyException();
    }

    @Test
    void testANameOutsideTheTokenGrammarIsRejected() {
        assertThatThrownBy(() -> CustomPatternValidator.validate("acme_id", "\\bACME-\\d{4}\\b"))
            .isInstanceOf(CustomPatternRejectedException.class)
            .extracting(exception -> ((CustomPatternRejectedException) exception).getDefence())
            .isEqualTo(Defence.NAME);
    }

    @Test
    void testARuleCannotShadowABuiltInPiiType() {
        assertThatThrownBy(() -> CustomPatternValidator.validate("EMAIL_ADDRESS", "\\bfoo\\d{1,4}\\b"))
            .isInstanceOf(CustomPatternRejectedException.class)
            .hasMessageContaining("redefine");
    }

    @Test
    void testARuleCannotShadowABuiltInSecretType() {
        assertThatThrownBy(() -> CustomPatternValidator.validate("OPENAI_KEY", "\\bfoo\\d{1,4}\\b"))
            .isInstanceOf(CustomPatternRejectedException.class)
            .hasMessageContaining("redefine");
    }

    @Test
    void testAnOverlongPatternIsRejected() {
        assertThatThrownBy(() -> CustomPatternValidator.validate("ACME_ID", "a".repeat(600)))
            .isInstanceOf(CustomPatternRejectedException.class)
            .extracting(exception -> ((CustomPatternRejectedException) exception).getDefence())
            .isEqualTo(Defence.LENGTH);
    }

    @Test
    void testAnInvalidRegexIsRejectedRatherThanPropagating() {
        assertThatThrownBy(() -> CustomPatternValidator.validate("ACME_ID", "\\bACME-[0-9\\b"))
            .isInstanceOf(CustomPatternRejectedException.class)
            .extracting(exception -> ((CustomPatternRejectedException) exception).getDefence())
            .isEqualTo(Defence.SYNTAX);
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testABoundedButExplosivePatternIsCaughtByTiming() {
        assertThatThrownBy(
            () -> CustomPatternValidator.validate("ACME_SLOW", "(x{1,40}x{1,40}){1,40}y"))
                .isInstanceOf(CustomPatternRejectedException.class)
                .extracting(exception -> ((CustomPatternRejectedException) exception).getDefence())
                .isEqualTo(Defence.TIMING);
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testValidationOnlyEverThrowsItsOwnRejection() {
        for (String regex : new String[] {
            "(x{1,40}x{1,40}){1,40}y", "\\bACME-\\d+\\b", "\\bACME-[0-9\\b", "a".repeat(600), "(a{1,2}|a{1,3}){1,90}$"
        }) {
            assertThatCode(() -> {
                try {
                    CustomPatternValidator.validate("ACME_PROBE", regex);
                } catch (CustomPatternRejectedException expected) {
                }
            })
                .as("validating '%s' must not escape as anything but a rejection", regex)
                .doesNotThrowAnyException();
        }
    }

    @Test
    void testTheRejectionNamesWhichDefenceCaughtIt() {
        assertThat(Defence.values()).containsExactly(Defence.NAME, Defence.LENGTH, Defence.SYNTAX, Defence.TIMING);
    }
}
