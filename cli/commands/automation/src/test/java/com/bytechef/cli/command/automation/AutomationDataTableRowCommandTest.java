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

package com.bytechef.cli.command.automation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.cli.CliApplication;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
class AutomationDataTableRowCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void testRowListForwardsFiltersVerbatim() throws Exception {
        try (StubApi stub = StubApi.start(200, "{\"content\":[],\"totalElements\":0}")) {
            int code = CliApplication.execute(
                "automation", "data-table", "row", "list", "--name", "orders", "--filter", "total:GTE:5",
                "--filter", "sku:IN:a,b", "--sort", "total:DESC", "--page-size", "10",
                "--host", stub.host(), "--token", "btc_x");

            assertEquals(0, code);
            assertTrue(
                stub.lastPath()
                    .startsWith("/api/automation/v1/data-tables/orders/rows?"),
                stub.lastPath());
            assertTrue(
                stub.lastPath()
                    .contains("filter=total%3AGTE%3A5"),
                stub.lastPath());
            assertTrue(
                stub.lastPath()
                    .contains("filter=sku%3AIN%3Aa%2Cb"),
                stub.lastPath());
            assertTrue(
                stub.lastPath()
                    .contains("sort=total%3ADESC"),
                stub.lastPath());
            assertTrue(
                stub.lastPath()
                    .contains("pageSize=10"),
                stub.lastPath());
        }
    }

    @Test
    void testRowClearRefusesWithoutYes() throws Exception {
        try (StubApi stub = StubApi.start(200, "{\"deletedCount\":0}")) {
            int code = CliApplication.execute(
                "automation", "data-table", "row", "clear", "--name", "orders", "--host", stub.host(),
                "--token", "btc_x");

            assertEquals(1, code);
            assertNull(stub.lastPath());
        }
    }

    @Test
    void testRowClearSucceedsWithYes() throws Exception {
        try (StubApi stub = StubApi.start(200, "{\"deletedCount\":3}")) {
            int code = CliApplication.execute(
                "automation", "data-table", "row", "clear", "--name", "orders", "--yes", "true", "--host",
                stub.host(), "--token", "btc_x");

            assertEquals(0, code);
            assertEquals("/api/automation/v1/data-tables/orders/rows/clear", stub.lastPath());
        }
    }

    @Test
    void testRowImportSendsCsvBody() throws Exception {
        Path csv = Files.createTempFile("rows", ".csv");

        Files.writeString(csv, "total\n1\n");

        try (StubApi stub = StubApi.start(200, "{\"importedCount\":1}")) {
            int code = CliApplication.execute(
                "automation", "data-table", "row", "import", "--name", "orders", "--file", csv.toString(),
                "--host", stub.host(), "--token", "btc_x");

            assertEquals(0, code);
            assertEquals("/api/automation/v1/data-tables/orders/rows/import", stub.lastPath());
            assertEquals("total\n1\n", stub.lastBody());
        }
    }

    @Test
    void testRowGetHitsIdPath() throws Exception {
        try (StubApi stub = StubApi.start(200, "{\"id\":7,\"values\":{}}")) {
            int code = CliApplication.execute(
                "automation", "data-table", "row", "get", "--name", "orders", "--id", "7", "--host", stub.host(),
                "--token", "btc_x");

            assertEquals(0, code);
            assertEquals("/api/automation/v1/data-tables/orders/rows/7", stub.lastPath());
        }
    }

    @Test
    void testRowGetHitsExternalIdPath() throws Exception {
        try (StubApi stub = StubApi.start(200, "{\"id\":7,\"externalId\":\"ext-1\",\"values\":{}}")) {
            int code = CliApplication.execute(
                "automation", "data-table", "row", "get", "--name", "orders", "--external-id", "ext-1", "--host",
                stub.host(), "--token", "btc_x");

            assertEquals(0, code);
            assertEquals("/api/automation/v1/data-tables/orders/rows/by-external-id/ext-1", stub.lastPath());
        }
    }

    @Test
    void testRowGetRejectsBothIdAndExternalId() throws Exception {
        try (StubApi stub = StubApi.start(200, "{\"id\":7,\"values\":{}}")) {
            int code = CliApplication.execute(
                "automation", "data-table", "row", "get", "--name", "orders", "--id", "7", "--external-id",
                "ext-1", "--host", stub.host(), "--token", "btc_x");

            assertEquals(1, code);
            assertNull(stub.lastPath());
        }
    }

    @Test
    void testRowGetRejectsNeitherIdNorExternalId() throws Exception {
        try (StubApi stub = StubApi.start(200, "{\"id\":7,\"values\":{}}")) {
            int code = CliApplication.execute(
                "automation", "data-table", "row", "get", "--name", "orders", "--host", stub.host(), "--token",
                "btc_x");

            assertEquals(1, code);
            assertNull(stub.lastPath());
        }
    }

    @Test
    void testRowInsertSendsValuesAndExternalId() throws Exception {
        try (StubApi stub = StubApi.start(201, "{\"id\":8,\"values\":{}}")) {
            int code = CliApplication.execute(
                "automation", "data-table", "row", "insert", "--name", "orders", "--values",
                "{\"total\":5,\"sku\":\"a\"}", "--external-id", "ext-2", "--host", stub.host(), "--token",
                "btc_x");

            assertEquals(0, code);
            assertEquals("/api/automation/v1/data-tables/orders/rows", stub.lastPath());
            assertTrue(
                stub.lastBody()
                    .contains("\"sku\":\"a\""),
                stub.lastBody());
            assertTrue(
                stub.lastBody()
                    .contains("\"externalId\":\"ext-2\""),
                stub.lastBody());
        }
    }

    @Test
    void testRowInsertRejectsMalformedValuesJson() throws Exception {
        try (StubApi stub = StubApi.start(201, "{\"id\":8,\"values\":{}}")) {
            int code = CliApplication.execute(
                "automation", "data-table", "row", "insert", "--name", "orders", "--values", "not-json", "--host",
                stub.host(), "--token", "btc_x");

            assertEquals(1, code);
            assertNull(stub.lastPath());
        }
    }

    @Test
    void testRowUpdateSendsValues() throws Exception {
        try (StubApi stub = StubApi.start(200, "{\"id\":8,\"values\":{}}")) {
            int code = CliApplication.execute(
                "automation", "data-table", "row", "update", "--name", "orders", "--id", "8", "--values",
                "{\"total\":9}", "--host", stub.host(), "--token", "btc_x");

            assertEquals(0, code);
            assertEquals("/api/automation/v1/data-tables/orders/rows/8", stub.lastPath());
            assertTrue(
                stub.lastBody()
                    .contains("\"total\":9"),
                stub.lastBody());
        }
    }

    @Test
    void testRowUpsertSendsValues() throws Exception {
        try (StubApi stub = StubApi.start(200, "{\"id\":8,\"externalId\":\"ext-3\",\"values\":{}}")) {
            int code = CliApplication.execute(
                "automation", "data-table", "row", "upsert", "--name", "orders", "--external-id", "ext-3",
                "--values", "{\"total\":9}", "--host", stub.host(), "--token", "btc_x");

            assertEquals(0, code);
            assertEquals(
                "/api/automation/v1/data-tables/orders/rows/by-external-id/ext-3", stub.lastPath());
            assertTrue(
                stub.lastBody()
                    .contains("\"total\":9"),
                stub.lastBody());
        }
    }

    @Test
    void testRowDeleteById() throws Exception {
        try (StubApi stub = StubApi.start(204, "")) {
            int code = CliApplication.execute(
                "automation", "data-table", "row", "delete", "--name", "orders", "--id", "8", "--host",
                stub.host(), "--token", "btc_x");

            assertEquals(0, code);
            assertEquals("/api/automation/v1/data-tables/orders/rows/8", stub.lastPath());
        }
    }

    @Test
    void testRowDeleteByIds() throws Exception {
        try (StubApi stub = StubApi.start(200, "{\"deletedCount\":2}")) {
            int code = CliApplication.execute(
                "automation", "data-table", "row", "delete", "--name", "orders", "--ids", "1,2", "--host",
                stub.host(), "--token", "btc_x");

            assertEquals(0, code);
            assertTrue(
                stub.lastPath()
                    .startsWith("/api/automation/v1/data-tables/orders/rows?"),
                stub.lastPath());
            assertTrue(
                stub.lastPath()
                    .contains("ids=1%2C2"),
                stub.lastPath());
        }
    }

    @Test
    void testRowDeleteByExternalId() throws Exception {
        try (StubApi stub = StubApi.start(204, "")) {
            int code = CliApplication.execute(
                "automation", "data-table", "row", "delete", "--name", "orders", "--external-id", "ext-4",
                "--host", stub.host(), "--token", "btc_x");

            assertEquals(0, code);
            assertEquals(
                "/api/automation/v1/data-tables/orders/rows/by-external-id/ext-4", stub.lastPath());
        }
    }

    @Test
    void testRowDeleteRejectsMultipleSelectors() throws Exception {
        try (StubApi stub = StubApi.start(204, "")) {
            int code = CliApplication.execute(
                "automation", "data-table", "row", "delete", "--name", "orders", "--id", "8", "--external-id",
                "ext-4", "--host", stub.host(), "--token", "btc_x");

            assertEquals(1, code);
            assertNull(stub.lastPath());
        }
    }

    @Test
    void testRowDeleteRejectsNoSelectors() throws Exception {
        try (StubApi stub = StubApi.start(204, "")) {
            int code = CliApplication.execute(
                "automation", "data-table", "row", "delete", "--name", "orders", "--host", stub.host(), "--token",
                "btc_x");

            assertEquals(1, code);
            assertNull(stub.lastPath());
        }
    }

    @Test
    void testRowBatchSendsRowsAndStrategy() throws Exception {
        Path batchFile = tempDir.resolve("batch.json");

        Files.writeString(batchFile, "[{\"values\":{\"total\":1}},{\"values\":{\"total\":2},\"externalId\":\"e1\"}]");

        try (StubApi stub = StubApi.start(200, "{\"insertedCount\":2}")) {
            int code = CliApplication.execute(
                "automation", "data-table", "row", "batch", "--name", "orders", "--file", batchFile.toString(),
                "--strategy", "upsert", "--host", stub.host(), "--token", "btc_x");

            assertEquals(0, code);
            assertEquals("/api/automation/v1/data-tables/orders/rows/batch", stub.lastPath());
            assertTrue(
                stub.lastBody()
                    .contains("\"createStrategy\":\"UPSERT\""),
                stub.lastBody());
            assertTrue(
                stub.lastBody()
                    .contains("\"e1\""),
                stub.lastBody());
        }
    }

    @Test
    void testRowBatchRejectsMissingFile() throws Exception {
        try (StubApi stub = StubApi.start(200, "{\"insertedCount\":0}")) {
            int code = CliApplication.execute(
                "automation", "data-table", "row", "batch", "--name", "orders", "--file", "/no/such/file.json",
                "--host", stub.host(), "--token", "btc_x");

            assertEquals(1, code);
            assertNull(stub.lastPath());
        }
    }

    @Test
    void testRowExportWritesTheRawCsvBodyVerbatimToFile() throws Exception {
        Path exportFile = tempDir.resolve("export.csv");

        String csv = "externalId,total,note\nex-1,5,\"has, a comma and a \"\"quote\"\"\"\n";

        try (StubApi stub = StubApi.start(200, csv)) {
            int code = CliApplication.execute(
                "automation", "data-table", "row", "export", "--name", "orders", "--file", exportFile.toString(),
                "--host", stub.host(), "--token", "btc_x", "--environment", "STAGING");

            assertEquals(0, code);
            assertEquals("/api/automation/v1/data-tables/orders/rows/export", stub.lastPath());
            assertEquals("STAGING", stub.lastEnvironment());
            assertEquals("Bearer btc_x", stub.lastAuthorization());
            assertEquals(csv, Files.readString(exportFile));
        }
    }

    @Test
    void testRowExportWritesToStdoutWhenNoFileGiven() throws Exception {
        String csv = "externalId,total\nex-1,5\n";

        try (StubApi stub = StubApi.start(200, csv)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintStream original = System.out;

            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

            int code;

            try {
                code = CliApplication.execute(
                    "automation", "data-table", "row", "export", "--name", "orders", "--host", stub.host(),
                    "--token", "btc_x");
            } finally {
                System.setOut(original);
            }

            assertEquals(0, code);
            assertEquals(csv, out.toString(StandardCharsets.UTF_8));
        }
    }

    @Test
    void testRowExportMapsNotFoundToExitCode3() throws Exception {
        try (StubApi stub = StubApi.start(404, "")) {
            int code = CliApplication.execute(
                "automation", "data-table", "row", "export", "--name", "orders", "--host", stub.host(), "--token",
                "btc_x");

            assertEquals(3, code);
        }
    }

    @Test
    void testUnauthorizedReturnsExitCode2() throws Exception {
        try (StubApi stub = StubApi.start(401, "")) {
            int code = CliApplication.execute(
                "automation", "data-table", "row", "get", "--name", "orders", "--id", "7", "--host", stub.host(),
                "--token", "bad");

            assertEquals(2, code);
        }
    }
}
