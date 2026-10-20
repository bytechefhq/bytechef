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

package com.bytechef.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Ivica Cardic
 */
class AbstractEnvironmentHeaderConverterTest {

    private final TestEnvironmentConverter converter = new TestEnvironmentConverter();

    @ParameterizedTest(name = "[{0}]")
    @ValueSource(strings = {
        "DEVELOPMENT", "development", "Development", " development ", "\tdevelopment\n"
    })
    void testConvertAcceptsAnyLetterCaseAndSurroundingWhitespace(String source) {
        assertThat(converter.convert(source)).isEqualTo(TestEnvironment.DEVELOPMENT);
    }

    @ParameterizedTest(name = "[{0}]")
    @ValueSource(strings = {
        "", " ", "   ", "\t"
    })
    void testConvertTreatsABlankValueAsAbsent(String source) {
        assertThat(converter.convert(source)).isNull();
    }

    @Test
    void testConvertRejectsAnUnknownName() {
        assertThatThrownBy(() -> converter.convert("staging-x"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private enum TestEnvironment {
        DEVELOPMENT, STAGING, PRODUCTION
    }

    private static final class TestEnvironmentConverter extends AbstractEnvironmentHeaderConverter<TestEnvironment> {

        private TestEnvironmentConverter() {
            super(TestEnvironment.class);
        }
    }
}
