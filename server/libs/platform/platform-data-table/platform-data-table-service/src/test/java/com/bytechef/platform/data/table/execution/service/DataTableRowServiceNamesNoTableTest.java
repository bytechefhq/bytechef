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

package com.bytechef.platform.data.table.execution.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * The row service reaches whatever physical table its {@code DataTableRef} names, and takes the ref from the caller. It
 * must have no way of naming one itself: the moment it can build a physical name it is choosing an owner, and the owner
 * it would choose is not the one resolution settled on. That is exactly how every embedded read came to return every
 * account's rows.
 *
 * <p>
 * A source scan, because the rule is about the shape of the file: one class, one forbidden reference, and no new
 * dependency for the module.
 *
 * @author Ivica Cardic
 */
class DataTableRowServiceNamesNoTableTest {

    private static final Path ROW_SERVICE_SOURCE = Path.of(
        "src/main/java/com/bytechef/platform/data/table/execution/service/DataTableRowServiceImpl.java");

    @Test
    void testTheRowServiceNeverBuildsAPhysicalTableName() throws IOException {
        assertThat(Files.isRegularFile(ROW_SERVICE_SOURCE))
            .as("row service source not found, working directory is wrong: %s", ROW_SERVICE_SOURCE)
            .isTrue();

        assertThat(Files.readString(ROW_SERVICE_SOURCE))
            .as("the row service must take its physical name from the resolved ref, never build one")
            .doesNotContain("PhysicalTableNaming")
            .doesNotContain("new DataTableRef(")
            .doesNotContain("new DataTableRef(");
    }
}
