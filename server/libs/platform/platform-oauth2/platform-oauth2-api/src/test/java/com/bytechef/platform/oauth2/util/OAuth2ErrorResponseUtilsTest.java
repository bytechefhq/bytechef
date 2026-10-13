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

package com.bytechef.platform.oauth2.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.test.extension.ObjectMapperSetupExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class OAuth2ErrorResponseUtilsTest {

    @Test
    void testDescribeReportsStatusErrorAndDescription() {
        assertThat(
            OAuth2ErrorResponseUtils.describe(
                400, "{\"error\":\"invalid_grant\",\"error_description\":\"Code expired\",\"debug\":\"secret\"}"))
                    .isEqualTo("HTTP 400: invalid_grant (Code expired)");
    }

    @Test
    void testDescribeReportsOnlyTheFieldsThatArePresent() {
        assertThat(OAuth2ErrorResponseUtils.describe(401, "{\"error\":\"invalid_client\"}"))
            .isEqualTo("HTTP 401: invalid_client");
        assertThat(OAuth2ErrorResponseUtils.describe(400, "{\"error_description\":\"Bad redirect\"}"))
            .isEqualTo("HTTP 400: Bad redirect");
    }

    @Test
    void testDescribeNeverEchoesANonJsonOrUnrelatedBody() {
        assertThat(OAuth2ErrorResponseUtils.describe(500, "<html>internal token=abc</html>"))
            .isEqualTo("HTTP 500");
        assertThat(OAuth2ErrorResponseUtils.describe(500, "{\"debug\":\"token=abc\"}"))
            .isEqualTo("HTTP 500");
        assertThat(OAuth2ErrorResponseUtils.describe(502, "[\"error\"]"))
            .isEqualTo("HTTP 502");
        assertThat(OAuth2ErrorResponseUtils.describe(503, null))
            .isEqualTo("HTTP 503");
    }

    @Test
    void testDescribeReadsTheMessageOfANestedErrorObject() {
        assertThat(
            OAuth2ErrorResponseUtils.describe(
                400, "{\"error\":{\"code\":40001,\"message\":\"Invalid code\",\"trace\":\"t-1\"}}"))
                    .isEqualTo("HTTP 400: Invalid code");
    }

    @Test
    void testDescribeReadsATopLevelMessage() {
        assertThat(OAuth2ErrorResponseUtils.describe(401, "{\"message\":\"Unauthorized client\"}"))
            .isEqualTo("HTTP 401: Unauthorized client");
        assertThat(
            OAuth2ErrorResponseUtils.describe(400, "{\"error\":\"invalid_grant\",\"message\":\"Code expired\"}"))
                .isEqualTo("HTTP 400: invalid_grant (Code expired)");
    }

    @Test
    void testDescribePrefersTheStandardDescriptionOverAMessage() {
        assertThat(
            OAuth2ErrorResponseUtils.describe(
                400, "{\"error_description\":\"Code expired\",\"message\":\"other\"}"))
                    .isEqualTo("HTTP 400: Code expired");
    }

    @Test
    void testDescribeCapsALongMessage() {
        String description = OAuth2ErrorResponseUtils.describe(400, "{\"message\":\"" + "x".repeat(5000) + "\"}");

        assertThat(description).hasSize("HTTP 400: ".length() + OAuth2ErrorResponseUtils.MAX_FIELD_LENGTH);
    }

    @Test
    void testSanitizeForLogEscapesLineBreaksAndControlCharacters() {
        assertThat(
            OAuth2ErrorResponseUtils.sanitizeForLog(
                "bad\r\nINFO forged entry\tx\u0000y\u001b[31mz\u2028w\u200e"))
                    .isEqualTo("bad\\r\\nINFO forged entry\\tx\\u0000y\\u001b[31mz\\u2028w\\u200e")
                    .doesNotContain("\n", "\r");
        assertThat(OAuth2ErrorResponseUtils.sanitizeForLog("{\"error\":\"invalid_grant\"}"))
            .isEqualTo("{\"error\":\"invalid_grant\"}");
        assertThat(OAuth2ErrorResponseUtils.sanitizeForLog(null)).isNull();
    }

    @Test
    void testDescribeCapsLongFields() {
        String description = OAuth2ErrorResponseUtils.describe(
            400, "{\"error_description\":\"" + "x".repeat(5000) + "\"}");

        assertThat(description).hasSize("HTTP 400: ".length() + OAuth2ErrorResponseUtils.MAX_FIELD_LENGTH);
    }
}
