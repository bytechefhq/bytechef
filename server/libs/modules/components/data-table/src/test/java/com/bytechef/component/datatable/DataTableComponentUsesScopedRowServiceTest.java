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

package com.bytechef.component.datatable;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The weakness of an explicit owner filter is that a new call site can simply omit it, and nothing complains. This
 * turns that from a convention into a build failure.
 *
 * <p>
 * A source scan rather than a bytecode one: the rule is about what a reviewer reads, the file set is small and fixed,
 * and it costs the module no new dependency.
 *
 * @author Ivica Cardic
 */
class DataTableComponentUsesScopedRowServiceTest {

    private static final List<String> ROW_SERVICE_CALLS = List.of(
        "dataTableRowService.listRows(", "dataTableRowService.getRow(", "dataTableRowService.insertRow(",
        "dataTableRowService.updateRow(", "dataTableRowService.deleteRow(", "dataTableRowService.exportCsv(",
        "dataTableRowService.importCsv(");

    @Test
    void testEveryRowServiceCallPassesARowOwnerFilter() throws IOException {
        Path sourceRoot = Path.of("src/main/java/com/bytechef/component/datatable");

        assertTrue(Files.isDirectory(sourceRoot), "Source root not found, working directory is wrong: " + sourceRoot);

        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            List<String> offenders = paths.filter(path -> {
                String pathName = path.toString();

                return pathName.endsWith(".java");
            })
                .flatMap(path -> findUnscopedCalls(path).stream())
                .toList();

            assertTrue(offenders.isEmpty(), "Row service calls with no RowOwnerFilter: " + offenders);
        }
    }

    private static List<String> findUnscopedCalls(Path path) {
        String source;

        try {
            source = Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read " + path, exception);
        }

        List<String> offenders = new ArrayList<>();

        for (String call : ROW_SERVICE_CALLS) {
            int index = source.indexOf(call);

            while (index >= 0) {
                String arguments = readArguments(source, index + call.length() - 1);

                if (!arguments.contains("rowOwnerFilter") && !arguments.contains("RowOwnerFilter")) {
                    Path fileName = path.getFileName();

                    offenders.add(fileName + " -> " + call);
                }

                index = source.indexOf(call, index + 1);
            }
        }

        return offenders;
    }

    /**
     * Returns the text between the opening parenthesis at {@code openIndex} and its match, counting nesting so that a
     * nested call does not end the argument list early.
     */
    private static String readArguments(String source, int openIndex) {
        int depth = 0;

        for (int index = openIndex; index < source.length(); index++) {
            char character = source.charAt(index);

            if (character == '(') {
                depth++;
            } else if (character == ')') {
                depth--;

                if (depth == 0) {
                    return source.substring(openIndex + 1, index);
                }
            }
        }

        throw new IllegalStateException("Unbalanced parentheses from index " + openIndex);
    }
}
