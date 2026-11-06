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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.cli.CliApplication;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AutomationDataTableCommandTest {

    @Test
    void testTableCreateSendsColumnsAndTags() throws Exception {
        try (StubApi stub = StubApi.start(201, "{\"name\":\"orders\",\"columns\":[]}")) {
            int code = CliApplication.execute(
                "automation", "data-table", "create", "--workspace-id", "1", "--name", "orders",
                "--column", "total:NUMBER", "--column", "sku:STRING", "--tag", "hot",
                "--host", stub.host(), "--token", "btc_x", "--environment", "STAGING");

            assertEquals(0, code);
            assertEquals("/api/automation/v1/workspaces/1/data-tables", stub.lastPath());
            assertEquals("STAGING", stub.lastEnvironment());
            assertTrue(
                stub.lastBody()
                    .contains("\"type\":\"NUMBER\""),
                stub.lastBody());
            assertTrue(
                stub.lastBody()
                    .contains("\"sku\""),
                stub.lastBody());
            assertTrue(
                stub.lastBody()
                    .contains("\"hot\""),
                stub.lastBody());
        }
    }

    @Test
    void testTableCreateRejectsColumnWithoutColon() throws Exception {
        try (StubApi stub = StubApi.start(201, "{\"name\":\"orders\",\"columns\":[]}")) {
            int code = CliApplication.execute(
                "automation", "data-table", "create", "--workspace-id", "1", "--name", "orders", "--column",
                "total", "--host", stub.host(), "--token", "btc_x");

            assertEquals(1, code);
            assertNull(stub.lastPath());
        }
    }

    @Test
    void testTableCreateRejectsUnknownColumnType() throws Exception {
        try (StubApi stub = StubApi.start(201, "{\"name\":\"orders\",\"columns\":[]}")) {
            int code = CliApplication.execute(
                "automation", "data-table", "create", "--workspace-id", "1", "--name", "orders", "--column",
                "total:MONEY", "--host", stub.host(), "--token", "btc_x");

            assertEquals(1, code);
            assertNull(stub.lastPath());
        }
    }

    @Test
    void testTableCreateRejectsMissingColumn() throws Exception {
        try (StubApi stub = StubApi.start(201, "{\"name\":\"orders\",\"columns\":[]}")) {
            int code = CliApplication.execute(
                "automation", "data-table", "create", "--workspace-id", "1", "--name", "orders", "--host",
                stub.host(), "--token", "btc_x");

            assertEquals(1, code);
            assertNull(stub.lastPath());
        }
    }

    @Test
    void testTableListForwardsWorkspaceAndTag() throws Exception {
        try (StubApi stub = StubApi.start(200, "[]")) {
            int code = CliApplication.execute(
                "automation", "data-table", "list", "--workspace-id", "1", "--tag", "hot", "--host", stub.host(),
                "--token", "btc_x");

            assertEquals(0, code);
            assertTrue(
                stub.lastPath()
                    .startsWith("/api/automation/v1/workspaces/1/data-tables"),
                stub.lastPath());
            assertTrue(
                stub.lastPath()
                    .contains("tag=hot"),
                stub.lastPath());
        }
    }

    @Test
    void testTableGetHitsNamePath() throws Exception {
        try (StubApi stub = StubApi.start(200, "{\"name\":\"orders\",\"columns\":[]}")) {
            int code = CliApplication.execute(
                "automation", "data-table", "get", "--name", "orders", "--host", stub.host(), "--token", "btc_x");

            assertEquals(0, code);
            assertEquals("/api/automation/v1/data-tables/orders", stub.lastPath());
        }
    }

    @Test
    void testTableUpdateSendsDescriptionAndTags() throws Exception {
        try (StubApi stub = StubApi.start(200, "{\"name\":\"orders\",\"columns\":[]}")) {
            int code = CliApplication.execute(
                "automation", "data-table", "update", "--name", "orders", "--description", "Orders",
                "--tag", "hot", "--tag", "cold", "--host", stub.host(), "--token", "btc_x");

            assertEquals(0, code);
            assertEquals("/api/automation/v1/data-tables/orders", stub.lastPath());
            assertTrue(
                stub.lastBody()
                    .contains("\"Orders\""),
                stub.lastBody());
            assertTrue(
                stub.lastBody()
                    .contains("\"cold\""),
                stub.lastBody());
        }
    }

    @Test
    void testTableUpdateOmitsTagsWhenNoneGiven() throws Exception {
        try (StubApi stub = StubApi.start(200, "{\"name\":\"orders\",\"columns\":[]}")) {
            int code = CliApplication.execute(
                "automation", "data-table", "update", "--name", "orders", "--description", "Orders",
                "--host", stub.host(), "--token", "btc_x");

            assertEquals(0, code);
            assertTrue(
                stub.lastBody()
                    .contains("\"description\""),
                stub.lastBody());
            assertFalse(
                stub.lastBody()
                    .contains("\"tags\""),
                stub.lastBody());
        }
    }

    @Test
    void testTableDeletePrintsConfirmation() throws Exception {
        try (StubApi stub = StubApi.start(204, "")) {
            int code = CliApplication.execute(
                "automation", "data-table", "delete", "--name", "orders", "--host", stub.host(), "--token",
                "btc_x");

            assertEquals(0, code);
            assertEquals("/api/automation/v1/data-tables/orders", stub.lastPath());
        }
    }

    @Test
    void testColumnAddSendsNameAndType() throws Exception {
        try (StubApi stub = StubApi.start(200, "{\"name\":\"orders\",\"columns\":[]}")) {
            int code = CliApplication.execute(
                "automation", "data-table", "column", "add", "--name", "orders", "--column", "sku:STRING",
                "--host", stub.host(), "--token", "btc_x");

            assertEquals(0, code);
            assertEquals("/api/automation/v1/data-tables/orders/columns", stub.lastPath());
            assertTrue(
                stub.lastBody()
                    .contains("\"sku\""),
                stub.lastBody());
            assertTrue(
                stub.lastBody()
                    .contains("\"STRING\""),
                stub.lastBody());
        }
    }

    @Test
    void testColumnRemoveHitsColumnPath() throws Exception {
        try (StubApi stub = StubApi.start(204, "")) {
            int code = CliApplication.execute(
                "automation", "data-table", "column", "remove", "--name", "orders", "--column", "sku", "--host",
                stub.host(), "--token", "btc_x");

            assertEquals(0, code);
            assertEquals("/api/automation/v1/data-tables/orders/columns/sku", stub.lastPath());
        }
    }

    @Test
    void testColumnRenameSendsNewName() throws Exception {
        try (StubApi stub = StubApi.start(200, "{\"name\":\"orders\",\"columns\":[]}")) {
            int code = CliApplication.execute(
                "automation", "data-table", "column", "rename", "--name", "orders", "--column", "sku",
                "--new-name", "productSku", "--host", stub.host(), "--token", "btc_x");

            assertEquals(0, code);
            assertEquals("/api/automation/v1/data-tables/orders/columns/sku/rename", stub.lastPath());
            assertTrue(
                stub.lastBody()
                    .contains("\"productSku\""),
                stub.lastBody());
        }
    }

    @Test
    void testUnauthorizedReturnsExitCode2() throws Exception {
        try (StubApi stub = StubApi.start(401, "")) {
            int code = CliApplication.execute(
                "automation", "data-table", "get", "--name", "orders", "--host", stub.host(), "--token", "bad");

            assertEquals(2, code);
        }
    }
}
