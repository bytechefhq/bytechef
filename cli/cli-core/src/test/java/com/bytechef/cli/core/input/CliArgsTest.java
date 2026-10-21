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

package com.bytechef.cli.core.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bytechef.cli.core.error.CliException;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class CliArgsTest {

    @Test
    void testParseDateTime() {
        assertNull(CliArgs.parseDateTime(null));
        assertEquals(
            OffsetDateTime.parse("2026-01-02T03:04:05Z"), CliArgs.parseDateTime("2026-01-02T03:04:05Z"));
    }

    @Test
    void testParseDateTimeInvalidThrowsExitCode1() {
        CliException exception = assertThrows(CliException.class, () -> CliArgs.parseDateTime("not-a-date"));

        assertEquals(1, exception.exitCode());
    }

    @Test
    void testSplitCsv() {
        assertNull(CliArgs.splitCsv(null));
        assertEquals(List.of("a", "b", "c"), CliArgs.splitCsv("a, b ,c"));
    }

    @Test
    void testSplitCsvLong() {
        assertNull(CliArgs.splitCsvLong(null));
        assertEquals(List.of(1L, 2L, 3L), CliArgs.splitCsvLong("1,2, 3"));
    }

    @Test
    void testSplitCsvLongInvalidThrowsExitCode1() {
        CliException exception = assertThrows(CliException.class, () -> CliArgs.splitCsvLong("1,x"));

        assertEquals(1, exception.exitCode());
    }
}
