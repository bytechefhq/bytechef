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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class OutputRendererTest {

    @Test
    void testRenderJsonPrettyPrints() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        OutputRenderer renderer = new OutputRenderer(buffer);

        renderer.renderJson(Map.of("id", 7, "status", "COMPLETED"));

        String output = buffer.toString(StandardCharsets.UTF_8);

        assertTrue(output.contains("\"status\""), "should contain key");
        assertTrue(output.contains("\n"), "should be pretty (multi-line)");
    }

    @Test
    void testRenderTableFromJsonArray() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        OutputRenderer renderer = new OutputRenderer(buffer);

        renderer.render(List.of(Map.of("id", 1, "name", "Slack"), Map.of("id", 2, "name", "GitHub")), "table");

        String output = buffer.toString(StandardCharsets.UTF_8);

        assertTrue(output.contains("ID"), "uppercased header expected");
        assertTrue(output.contains("NAME"));
        assertTrue(output.contains("Slack"));
        assertTrue(output.contains("GitHub"));
    }

    @Test
    void testRenderTableFromPagedContent() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        OutputRenderer renderer = new OutputRenderer(buffer);

        renderer.render(Map.of("content", List.of(Map.of("id", 7, "status", "COMPLETED")), "totalElements", 1),
            "table");

        String output = buffer.toString(StandardCharsets.UTF_8);

        assertTrue(output.contains("STATUS"));
        assertTrue(output.contains("COMPLETED"));
    }

    @Test
    void testRenderTableFallsBackToJsonForNonList() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        OutputRenderer renderer = new OutputRenderer(buffer);

        renderer.render(Map.of("id", 7, "status", "COMPLETED"), "table");

        String output = buffer.toString(StandardCharsets.UTF_8);

        assertTrue(output.contains("\"status\""), "non-list should fall back to JSON");
    }

    @Test
    void testRenderJsonWhenOutputNotTable() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        OutputRenderer renderer = new OutputRenderer(buffer);

        renderer.render(List.of(Map.of("id", 1)), "json");

        String output = buffer.toString(StandardCharsets.UTF_8);

        assertTrue(output.contains("\"id\""), "json output expected");
    }

    @Test
    void testRenderTableAlignsColumns() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        OutputRenderer renderer = new OutputRenderer(buffer);

        renderer.renderTable(
            List.of("ID", "STATUS"), List.of(List.of("7", "COMPLETED"), List.of("42", "FAILED")));

        String output = buffer.toString(StandardCharsets.UTF_8);

        assertTrue(output.contains("ID"));
        assertTrue(output.contains("COMPLETED"));
        assertTrue(output.contains("FAILED"));
    }
}
