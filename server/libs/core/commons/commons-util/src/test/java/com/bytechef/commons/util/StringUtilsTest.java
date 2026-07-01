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

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class StringUtilsTest {

    @Test
    void testAppendWithNewlineEmptyStringBuilder() {
        StringBuilder stringBuilder = new StringBuilder();

        StringUtils.appendWithNewline("first line", stringBuilder);

        assertThat(stringBuilder.toString()).isEqualTo("first line");
    }

    @Test
    void testAppendWithNewlineNonEmptyStringBuilder() {
        StringBuilder stringBuilder = new StringBuilder("existing content");

        StringUtils.appendWithNewline("new line", stringBuilder);

        assertThat(stringBuilder.toString()).isEqualTo("existing content\nnew line");
    }

    @Test
    void testAppendWithNewlineMultipleAppends() {
        StringBuilder stringBuilder = new StringBuilder();

        StringUtils.appendWithNewline("line 1", stringBuilder);
        StringUtils.appendWithNewline("line 2", stringBuilder);
        StringUtils.appendWithNewline("line 3", stringBuilder);

        assertThat(stringBuilder.toString()).isEqualTo("line 1\nline 2\nline 3");
    }

    @Test
    void testParseBooleanTrue() {
        assertThat(BooleanUtils.parseBoolean("true")).isTrue();
        assertThat(BooleanUtils.parseBoolean("TRUE")).isTrue();
        assertThat(BooleanUtils.parseBoolean("True")).isTrue();
    }

    @Test
    void testParseBooleanT() {
        assertThat(BooleanUtils.parseBoolean("t")).isTrue();
        assertThat(BooleanUtils.parseBoolean("T")).isTrue();
    }

    @Test
    void testParseBooleanYes() {
        assertThat(BooleanUtils.parseBoolean("yes")).isTrue();
        assertThat(BooleanUtils.parseBoolean("YES")).isTrue();
        assertThat(BooleanUtils.parseBoolean("Yes")).isTrue();
    }

    @Test
    void testParseBooleanY() {
        assertThat(BooleanUtils.parseBoolean("y")).isTrue();
        assertThat(BooleanUtils.parseBoolean("Y")).isTrue();
    }

    @Test
    void testParseBooleanOne() {
        assertThat(BooleanUtils.parseBoolean("1")).isTrue();
    }

    @Test
    void testParseBooleanFalse() {
        assertThat(BooleanUtils.parseBoolean("false")).isFalse();
        assertThat(BooleanUtils.parseBoolean("FALSE")).isFalse();
        assertThat(BooleanUtils.parseBoolean("False")).isFalse();
    }

    @Test
    void testParseBooleanF() {
        assertThat(BooleanUtils.parseBoolean("f")).isFalse();
        assertThat(BooleanUtils.parseBoolean("F")).isFalse();
    }

    @Test
    void testParseBooleanNo() {
        assertThat(BooleanUtils.parseBoolean("no")).isFalse();
        assertThat(BooleanUtils.parseBoolean("NO")).isFalse();
        assertThat(BooleanUtils.parseBoolean("No")).isFalse();
    }

    @Test
    void testParseBooleanN() {
        assertThat(BooleanUtils.parseBoolean("n")).isFalse();
        assertThat(BooleanUtils.parseBoolean("N")).isFalse();
    }

    @Test
    void testParseBooleanZero() {
        assertThat(BooleanUtils.parseBoolean("0")).isFalse();
    }

    @Test
    void testParseBooleanInvalidString() {
        // Boolean.parseBoolean returns false for invalid strings
        assertThat(BooleanUtils.parseBoolean("invalid")).isFalse();
        assertThat(BooleanUtils.parseBoolean("maybe")).isFalse();
        assertThat(BooleanUtils.parseBoolean("2")).isFalse();
    }

    @Test
    void testParseBigDecimalInteger() {
        assertThat(NumberUtils.parseBigDecimal("123")).isEqualTo(new BigDecimal("123"));
    }

    @Test
    void testParseBigDecimalDecimal() {
        assertThat(NumberUtils.parseBigDecimal("123.456")).isEqualTo(new BigDecimal("123.456"));
    }

    @Test
    void testParseBigDecimalNegative() {
        assertThat(NumberUtils.parseBigDecimal("-123.456")).isEqualTo(new BigDecimal("-123.456"));
    }

    @Test
    void testParseBigDecimalScientificNotation() {
        assertThat(NumberUtils.parseBigDecimal("1.23E+3")).isEqualTo(new BigDecimal("1.23E+3"));
    }

    @Test
    void testParseBigDecimalZero() {
        assertThat(NumberUtils.parseBigDecimal("0")).isEqualTo(BigDecimal.ZERO);
        assertThat(NumberUtils.parseBigDecimal("0.0")).isEqualTo(new BigDecimal("0.0"));
    }

    @Test
    void testParseBigDecimalInvalidString() {
        assertThat(NumberUtils.parseBigDecimal("invalid")).isNull();
        assertThat(NumberUtils.parseBigDecimal("12.34.56")).isNull();
        assertThat(NumberUtils.parseBigDecimal("")).isNull();
    }

    @Test
    void testParseBigDecimalEmptyString() {
        assertThat(NumberUtils.parseBigDecimal("")).isNull();
    }

    @Test
    void testParseLongInteger() {
        assertThat(NumberUtils.parseLong("123")).isEqualTo(123L);
    }

    @Test
    void testParseLongNegative() {
        assertThat(NumberUtils.parseLong("-456")).isEqualTo(-456L);
    }

    @Test
    void testParseLongMaxValue() {
        String maxLong = String.valueOf(Long.MAX_VALUE);

        assertThat(NumberUtils.parseLong(maxLong)).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void testParseLongMinValue() {
        String minLong = String.valueOf(Long.MIN_VALUE);

        assertThat(NumberUtils.parseLong(minLong)).isEqualTo(Long.MIN_VALUE);
    }

    @Test
    void testParseLongFromDecimal() {
        // Should parse decimal and convert to long
        assertThat(NumberUtils.parseLong("123.456")).isEqualTo(123L);
        assertThat(NumberUtils.parseLong("999.999")).isEqualTo(999L);
    }

    @Test
    void testParseLongZero() {
        assertThat(NumberUtils.parseLong("0")).isEqualTo(0L);
    }

    @Test
    void testParseLongInvalidString() {
        assertThat(NumberUtils.parseLong("invalid")).isNull();
        assertThat(NumberUtils.parseLong("12.34.56")).isNull();
        assertThat(NumberUtils.parseLong("")).isNull();
    }

    @Test
    void testParseLongEmptyString() {
        assertThat(NumberUtils.parseLong("")).isNull();
    }

    @Test
    void testParseSqlDateISOFormat() {
        java.sql.Date date = DateUtils.parseSqlDate("2025-01-18");

        assertThat(date).isNotNull();
        assertThat(date.toString()).isEqualTo("2025-01-18");
    }

    @Test
    void testParseSqlDateCustomFormat() {
        java.sql.Date date = DateUtils.parseSqlDate("2025-1-18");

        assertThat(date).isNotNull();
        assertThat(date.toString()).isEqualTo("2025-01-18");
    }

    @Test
    void testParseSqlDateFromEpochMillis() {
        long epochMillis = 1705536000000L; // 2024-01-18 00:00:00 UTC

        java.sql.Date date = DateUtils.parseSqlDate(String.valueOf(epochMillis));

        assertThat(date).isNotNull();
        assertThat(date.getTime()).isEqualTo(epochMillis);
    }

    @Test
    void testParseSqlDateFromDateTime() {
        java.sql.Date date = DateUtils.parseSqlDate("2025-01-18 12:30:45");

        assertThat(date).isNotNull();
        assertThat(date.toString()).isEqualTo("2025-01-18");
    }

    @Test
    void testParseSqlDateInvalidString() {
        assertThat(DateUtils.parseSqlDate("invalid")).isNull();
        assertThat(DateUtils.parseSqlDate("not-a-date")).isNull();
    }

    @Test
    void testParseSqlDateEmptyString() {
        assertThat(DateUtils.parseSqlDate("")).isNull();
    }

    @Test
    void testParseSqlTimestampISO8601() {
        Timestamp timestamp = DateUtils.parseSqlTimestamp("2025-01-18T12:30:45Z");

        assertThat(timestamp).isNotNull();
    }

    @Test
    void testParseSqlTimestampISO8601WithMillis() {
        Timestamp timestamp = DateUtils.parseSqlTimestamp("2025-01-18T12:30:45.123Z");

        assertThat(timestamp).isNotNull();
    }

    @Test
    void testParseSqlTimestampLocalDateTime() {
        Timestamp timestamp = DateUtils.parseSqlTimestamp("2025-01-18 12:30:45");

        assertThat(timestamp).isNotNull();
        assertThat(timestamp.toString()).startsWith("2025-01-18 12:30:45");
    }

    @Test
    void testParseSqlTimestampLocalDateTimeShort() {
        Timestamp timestamp = DateUtils.parseSqlTimestamp("2025-01-18 12:30");

        assertThat(timestamp).isNotNull();
        assertThat(timestamp.toString()).startsWith("2025-01-18 12:30:00");
    }

    @Test
    void testParseSqlTimestampFromEpochMillis() {
        long epochMillis = 1705582245000L; // 2024-01-18 12:30:45 UTC

        Timestamp timestamp = DateUtils.parseSqlTimestamp(String.valueOf(epochMillis));

        assertThat(timestamp).isNotNull();
        assertThat(timestamp.getTime()).isEqualTo(epochMillis);
    }

    @Test
    void testParseSqlTimestampInvalidString() {
        assertThat(DateUtils.parseSqlTimestamp("invalid")).isNull();
        assertThat(DateUtils.parseSqlTimestamp("not-a-timestamp")).isNull();
    }

    @Test
    void testParseSqlTimestampEmptyString() {
        assertThat(DateUtils.parseSqlTimestamp("")).isNull();
    }

    @Test
    void testSanitizeOneParamNullValue() {
        assertThat(StringUtils.sanitize(null)).isNull();
    }

    @Test
    void testSanitizeOneParamEmptyString() {
        assertThat(StringUtils.sanitize("")).isEmpty();
    }

    @Test
    void testSanitizeOneParamBlankString() {
        assertThat(StringUtils.sanitize("   ")).isEmpty();
    }

    @Test
    void testSanitizeOneParamNormalValue() {
        String input = "normal-session-id-12345";

        assertThat(StringUtils.sanitize(input)).isEqualTo(input);
    }

    @Test
    void testSanitizeOneParamCarriageReturn() {
        String input = "value\rwith\rcarriage\rreturns";

        assertThat(StringUtils.sanitize(input)).isEqualTo("value_with_carriage_returns");
    }

    @Test
    void testSanitizeOneParamLineFeed() {
        String input = "value\nwith\nnewlines";

        assertThat(StringUtils.sanitize(input)).isEqualTo("value_with_newlines");
    }

    @Test
    void testSanitizeOneParamCRLF() {
        String input = "value\r\nwith\r\ncrlf";

        assertThat(StringUtils.sanitize(input)).isEqualTo("value_with_crlf");
    }

    @Test
    void testSanitizeOneParamLogInjectionAttempt() {
        String maliciousInput = "session123\n[INFO] User admin logged in successfully";

        String sanitized = StringUtils.sanitize(maliciousInput);

        assertThat(sanitized).doesNotContain("\n");
        assertThat(sanitized).isEqualTo("session123_[INFO]_User_admin_logged_in_successfully");
    }

    @Test
    void testSanitizeOneParamComplexLogInjection() {
        String maliciousInput = "value\r\n2025-01-17 12:00:00.000 [main] INFO - Fake log entry\r\nAnother fake";

        String sanitized = StringUtils.sanitize(maliciousInput);

        assertThat(sanitized).doesNotContain("\r");
        assertThat(sanitized).doesNotContain("\n");
        assertThat(sanitized)
            .isEqualTo("value_2025-01-17_12_00_00.000_[main]_INFO_-_Fake_log_entry_Another_fake");
    }

    @Test
    void testSanitizeOneParamOnlyCarriageReturns() {
        String input = "\r\r\r";

        assertThat(StringUtils.sanitize(input)).isEmpty();
    }

    @Test
    void testSanitizeOneParamOnlyNewlines() {
        String input = "\n\n\n";

        assertThat(StringUtils.sanitize(input)).isEmpty();
    }

    @Test
    void testSanitizeOneParamMixedContent() {
        String input = "start\rvalue\nmiddle\r\nend";

        assertThat(StringUtils.sanitize(input)).isEqualTo("start_value_middle_end");
    }

    @Test
    void testSanitizeOneParamPreservesOtherWhitespace() {
        String input = "value with\ttabs and   spaces";

        assertThat(StringUtils.sanitize(input)).isEqualTo("value_with_tabs_and_spaces");
    }

    @Test
    void testSanitizeOneParamWindowsForbiddenCharacters() {
        String input = "file\\name:with*forbidden?chars\"<>|";

        assertThat(StringUtils.sanitize(input)).isEqualTo("file_name_with_forbidden_chars_");
    }

    @Test
    void testSanitizeTwoParamsNullValue() {
        assertThat(StringUtils.sanitize(null, 10)).isNull();
    }

    @Test
    void testSanitizeTwoParamsEmptyString() {
        assertThat(StringUtils.sanitize("", 10)).isEmpty();
    }

    @Test
    void testSanitizeTwoParamsBlankString() {
        assertThat(StringUtils.sanitize("   ", 10)).isEmpty();
    }

    @Test
    void testSanitizeTwoParamsNormalValue() {
        String input = "normal-session-id-12345";

        assertThat(StringUtils.sanitize(input, -1)).isEqualTo(input);
    }

    @Test
    void testSanitizeTwoParamsWithLengthLimit() {
        String input = "this is a very long string that should be truncated";

        assertThat(StringUtils.sanitize(input, 20)).isEqualTo("this_is_a_very_long_");
    }

    @Test
    void testSanitizeTwoParamsNoLengthLimit() {
        String input = "this is a very long string that should not be truncated";

        assertThat(StringUtils.sanitize(input, -1))
            .isEqualTo("this_is_a_very_long_string_that_should_not_be_truncated");
    }

    @Test
    void testSanitizeTwoParamsCarriageReturn() {
        String input = "value\rwith\rcarriage\rreturns";

        assertThat(StringUtils.sanitize(input, -1)).isEqualTo("value_with_carriage_returns");
    }

    @Test
    void testSanitizeTwoParamsLineFeed() {
        String input = "value\nwith\nnewlines";

        assertThat(StringUtils.sanitize(input, -1)).isEqualTo("value_with_newlines");
    }

    @Test
    void testSanitizeTwoParamsCRLF() {
        String input = "value\r\nwith\r\ncrlf";

        assertThat(StringUtils.sanitize(input, -1)).isEqualTo("value_with_crlf");
    }

    @Test
    void testSanitizeTwoParamsCRLFWithLengthLimit() {
        String input = "value\r\nwith\r\ncrlf";

        assertThat(StringUtils.sanitize(input, 15)).isEqualTo("value_with_crlf");
    }

    @Test
    void testSanitizeTwoParamsWindowsForbiddenCharacters() {
        String input = "file\\name:with*forbidden?chars\"<>|.txt";

        assertThat(StringUtils.sanitize(input, -1)).isEqualTo("file_name_with_forbidden_chars_.txt");
    }

    @Test
    void testSanitizeTwoParamsMultipleWhitespaces() {
        String input = "multiple    spaces   and\ttabs";

        assertThat(StringUtils.sanitize(input, -1)).isEqualTo("multiple_spaces_and_tabs");
    }

    @Test
    void testSanitizeTwoParamsMultipleUnderscores() {
        String input = "multiple___underscores____here";

        assertThat(StringUtils.sanitize(input, -1)).isEqualTo("multiple_underscores_here");
    }

    @Test
    void testSanitizeTwoParamsLeadingTrailingSpaces() {
        String input = "   leading and trailing spaces   ";

        assertThat(StringUtils.sanitize(input, -1)).isEqualTo("leading_and_trailing_spaces");
    }

    @Test
    void testSanitizeTwoParamsComplexMixedContent() {
        String input = "  file\\name:with*forbidden?chars\n\rand\ttabs  ";

        assertThat(StringUtils.sanitize(input, -1))
            .isEqualTo("file_name_with_forbidden_chars_and_tabs");
    }

    @Test
    void testSanitizeTwoParamsLengthLimitShorterThanString() {
        String input = "short";

        assertThat(StringUtils.sanitize(input, 100)).isEqualTo("short");
    }

    @Test
    void testSanitizeTwoParamsLengthLimitZero() {
        String input = "test";

        assertThat(StringUtils.sanitize(input, 0)).isEmpty();
    }

    @Test
    void testSanitizeTwoParamsExactLengthLimit() {
        String input = "exactly20characters!";

        assertThat(StringUtils.sanitize(input, 20)).hasSize(20);
    }

    @Test
    void testSanitizeTwoParamsPreservesNormalCharacters() {
        String input = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

        assertThat(StringUtils.sanitize(input, -1)).isEqualTo(input);
    }
}
