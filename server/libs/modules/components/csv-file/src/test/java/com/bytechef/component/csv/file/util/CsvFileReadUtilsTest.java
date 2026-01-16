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

package com.bytechef.component.csv.file.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class CsvFileReadUtilsTest {

    @Test
    public void testGetIteratorWithEmptyFileAndHeaderRow() {
        BufferedReader bufferedReader = new BufferedReader(new StringReader(""));
        ReadConfiguration configuration = new ReadConfiguration(",", "\"", true, false, 0, Integer.MAX_VALUE, false);

        assertThrows(IllegalArgumentException.class, () -> {
            CsvFileReadUtils.getIterator(bufferedReader, configuration);
        });
    }

    @Test
    public void testGetIteratorWithContentAndHeaderRow() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new StringReader("header1,header2\nvalue1,value2"));
        ReadConfiguration configuration = new ReadConfiguration(",", "\"", true, false, 0, Integer.MAX_VALUE, false);

        assertNotNull(CsvFileReadUtils.getIterator(bufferedReader, configuration));
    }
}
