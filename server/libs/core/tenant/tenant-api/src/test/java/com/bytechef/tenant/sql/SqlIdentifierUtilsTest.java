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

package com.bytechef.tenant.sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class SqlIdentifierUtilsTest {

    @Test
    void testQuoteIdentifierWrapsInDoubleQuotes() {
        assertThat(SqlIdentifierUtils.quoteIdentifier("bytechef_000001")).isEqualTo("\"bytechef_000001\"");
    }

    @Test
    void testQuoteIdentifierDoublesEmbeddedQuotes() {
        assertThat(SqlIdentifierUtils.quoteIdentifier("a\"b")).isEqualTo("\"a\"\"b\"");
    }

    @Test
    void testQuoteIdentifierRejectsAnEmptyIdentifier() {
        assertThatThrownBy(() -> SqlIdentifierUtils.quoteIdentifier(""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testQuoteIdentifierRejectsANulCharacter() {
        assertThatThrownBy(() -> SqlIdentifierUtils.quoteIdentifier("a\u0000b"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
