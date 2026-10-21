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

package com.bytechef.cli.core.output;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Renders command results either as pretty-printed JSON or as an aligned text table.
 *
 * @author Ivica Cardic
 */
public class OutputRenderer {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .enable(SerializationFeature.INDENT_OUTPUT);

    private final PrintStream printStream;

    public OutputRenderer(OutputStream outputStream) {
        this.printStream = new PrintStream(outputStream, true, StandardCharsets.UTF_8);
    }

    /**
     * Renders {@code value} as JSON, or as a table when {@code output} is {@code "table"}. Table rendering works on any
     * list-shaped result (a JSON array, or a paged object with a {@code content} array); anything else falls back to
     * JSON.
     */
    public void render(Object value, String output) {
        if ("table".equalsIgnoreCase(output)) {
            renderTableFromValue(value);
        } else {
            renderJson(value);
        }
    }

    public void renderJson(Object value) {
        try {
            printStream.println(OBJECT_MAPPER.writeValueAsString(value));
        } catch (Exception e) {
            throw new RuntimeException("Failed to render JSON: " + e.getMessage(), e);
        }
    }

    private void renderTableFromValue(Object value) {
        JsonNode node = OBJECT_MAPPER.valueToTree(value);

        JsonNode rowsNode = null;

        if (node.isArray()) {
            rowsNode = node;
        } else if (node.has("content") && node.get("content")
            .isArray()) {
            rowsNode = node.get("content");
        }

        if (rowsNode == null) {
            renderJson(value);

            return;
        }

        if (rowsNode.isEmpty()) {
            message("(no results)");

            return;
        }

        Set<String> columns = new LinkedHashSet<>();

        for (JsonNode row : rowsNode) {
            if (row.isObject()) {
                row.fieldNames()
                    .forEachRemaining(field -> {
                        if (row.get(field)
                            .isValueNode()) {
                            columns.add(field);
                        }
                    });
            }
        }

        if (columns.isEmpty()) {
            renderScalarTable(rowsNode);

            return;
        }

        List<String> headers = new ArrayList<>();

        for (String column : columns) {
            headers.add(column.toUpperCase(Locale.ROOT));
        }

        List<List<String>> tableRows = new ArrayList<>();

        for (JsonNode row : rowsNode) {
            List<String> cells = new ArrayList<>();

            for (String column : columns) {
                JsonNode cell = row.get(column);

                cells.add(cell == null || cell.isNull() ? "" : cell.asText());
            }

            tableRows.add(cells);
        }

        renderTable(headers, tableRows);
    }

    private void renderScalarTable(JsonNode rowsNode) {
        List<List<String>> tableRows = new ArrayList<>();

        for (JsonNode row : rowsNode) {
            tableRows.add(List.of(row.isNull() ? "" : row.asText()));
        }

        renderTable(List.of("VALUE"), tableRows);
    }

    public void renderTable(List<String> headers, List<List<String>> rows) {
        int[] widths = new int[headers.size()];

        for (int i = 0; i < headers.size(); i++) {
            widths[i] = headers.get(i)
                .length();
        }

        for (List<String> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                widths[i] = Math.max(
                    widths[i], row.get(i)
                        .length());
            }
        }

        printStream.println(formatRow(headers, widths));

        for (List<String> row : rows) {
            printStream.println(formatRow(row, widths));
        }
    }

    public void message(String text) {
        printStream.println(text);
    }

    private static String formatRow(List<String> cells, int[] widths) {
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                stringBuilder.append("  ");
            }

            stringBuilder.append(String.format("%-" + widths[i] + "s", cells.get(i)));
        }

        return stringBuilder.toString();
    }
}
