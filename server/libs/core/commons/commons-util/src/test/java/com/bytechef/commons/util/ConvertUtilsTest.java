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

package com.bytechef.commons.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Igor Beslic
 */
public class ConvertUtilsTest {

    @Test
    public void testConvertString() {

        for (String integerString : List.of("435", "+435", "435 ", " 435 ", "  435")) {
            Assertions.assertThat(
                ConvertUtils.convertString(integerString))
                .isInstanceOf(
                    Integer.class)
                .isEqualTo(
                    435);
        }

        for (String integerString : List.of("-125", " -125", "-125 ", " -125 ", "  -125")) {
            Assertions.assertThat(
                ConvertUtils.convertString(integerString))
                .isInstanceOf(
                    Integer.class)
                .isEqualTo(
                    -125);
        }

        Assertions.assertThat(
            ConvertUtils.convertString("452.45"))
            .isInstanceOf(
                Double.class)
            .isEqualTo(
                452.45);

        for (String booleanString : List.of("TRUE", "true", "TruE", "truE", "trUE")) {
            Assertions.assertThat(
                ConvertUtils.convertString(booleanString))
                .isInstanceOf(
                    Boolean.class)
                .isEqualTo(
                    Boolean.TRUE);
        }

        for (String booleanString : List.of("FALSE", "false", "FAlse", "FALse", "falsE")) {
            Assertions.assertThat(
                ConvertUtils.convertString(booleanString))
                .isInstanceOf(
                    Boolean.class)
                .isEqualTo(
                    Boolean.FALSE);
        }

        Assertions.assertThat(
            ConvertUtils.convertString("2011-12-03T10:15:30"))
            .isInstanceOf(
                LocalDateTime.class)
            .isEqualTo(
                LocalDateTime.of(2011, 12, 3, 10, 15, 30, 0));

        Assertions.assertThat(
            ConvertUtils.convertString("2011-12-03"))
            .isInstanceOf(
                LocalDate.class)
            .isEqualTo(
                LocalDate.of(2011, 12, 3));

        Assertions.assertThat(
            ConvertUtils.convertString("non convertable value"))
            .isInstanceOf(
                String.class)
            .isEqualTo(
                "non convertable value");

        Assertions.assertThat(
            ConvertUtils.convertString(null))
            .isNull();
    }

}
