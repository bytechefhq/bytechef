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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.DataTableStorageUsage;
import com.bytechef.platform.data.table.exception.DataTableStorageLimitExceededException;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
class DataTableStorageServiceIntTest {

    private static final String BASE_NAME = "storage";
    private static final long DEV_ENVIRONMENT_ID = 0;
    private static final String PHYSICAL_NAME = "dt_0_storage";

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private DataTableRowService dataTableRowService;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private DataTableStorageService dataTableStorageService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long originalMaxSizeBytes;

    @BeforeEach
    void beforeEach() {
        originalMaxSizeBytes = getDataTable().getMaxSizeBytes();

        dataTableService.dropTable(BASE_NAME, DEV_ENVIRONMENT_ID);
        dataTableService.createTable(
            BASE_NAME, null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);

        jdbcTemplate.execute(
            "ALTER TABLE \"" + PHYSICAL_NAME + "\" SET (autovacuum_enabled = false, toast.autovacuum_enabled = false)");
    }

    @AfterEach
    void afterEach() {
        setLimit(originalMaxSizeBytes);

        dataTableService.dropTable(BASE_NAME, DEV_ENVIRONMENT_ID);
    }

    @Test
    void testGetUsageComputesThePercentageOfTheLimit() {
        insertRows(10);

        long allocatedBytes = getAllocatedBytes();

        setLimit(allocatedBytes * 2);

        DataTableStorageUsage dataTableStorageUsage = dataTableStorageService.getUsage();

        assertEquals(allocatedBytes, dataTableStorageUsage.usedBytes());
        assertEquals(allocatedBytes * 2, dataTableStorageUsage.limitBytes());
        assertEquals(50.0, dataTableStorageUsage.percentage());
        assertFalse(dataTableStorageUsage.unlimited());
    }

    @Test
    void testGetUsageIsUnlimitedWhenTheLimitIsZero() {
        insertRows(10);

        setLimit(0);

        DataTableStorageUsage dataTableStorageUsage = dataTableStorageService.getUsage();

        assertTrue(dataTableStorageUsage.unlimited());
        assertEquals(0.0, dataTableStorageUsage.percentage());
        assertEquals(getAllocatedBytes(), dataTableStorageUsage.usedBytes());
    }

    @Test
    void testGetUsageReportsLiveRowsWhenAllocatedSpaceExceedsTheLimit() {
        insertRowsAndDeleteAllButTheLast();

        long liveBytes = getLiveBytes();

        setLimit(liveBytes * 2);

        assertTrue(getAllocatedBytes() > liveBytes * 2);

        DataTableStorageUsage dataTableStorageUsage = dataTableStorageService.getUsage();

        assertEquals(liveBytes, dataTableStorageUsage.usedBytes());
        assertEquals(50.0, dataTableStorageUsage.percentage());
    }

    @Test
    void testCheckLimitThrowsWhenTheLimitIsReached() {
        insertRows(10);

        setLimit(getLiveBytes());

        assertThrows(DataTableStorageLimitExceededException.class, () -> dataTableStorageService.checkLimit(1));
    }

    @Test
    void testCheckLimitPassesWhenDeletedRowsLeaveRoomInAllocatedSpace() {
        insertRowsAndDeleteAllButTheLast();

        long allocatedBytes = getAllocatedBytes();

        setLimit(allocatedBytes);

        assertTrue(getLiveBytes() + 10_000 <= allocatedBytes);

        assertDoesNotThrow(() -> dataTableStorageService.checkLimit(10_000));
    }

    @Test
    void testCheckLimitThrowsWhenLiveRowsLeaveNoRoom() {
        insertRowsAndDeleteAllButTheLast();

        long liveBytes = getLiveBytes();

        setLimit(liveBytes + 100);

        assertTrue(getAllocatedBytes() > liveBytes + 100);

        assertDoesNotThrow(() -> dataTableStorageService.checkLimit(100));
        assertThrows(
            DataTableStorageLimitExceededException.class, () -> dataTableStorageService.checkLimit(101));
    }

    @Test
    void testCheckLimitPassesWhenUnlimited() {
        insertRows(10);

        setLimit(0);

        assertDoesNotThrow(() -> dataTableStorageService.checkLimit(Long.MAX_VALUE / 2));
    }

    @Test
    void testGetTableBytesMeasuresTheLiveRowsOfTheTable() {
        assertEquals(0, dataTableStorageService.getTableBytes(BASE_NAME, DEV_ENVIRONMENT_ID));

        dataTableRowService.insertRows(BASE_NAME, List.of(Map.of("title", "x")), DEV_ENVIRONMENT_ID);

        long oneRowBytes = dataTableStorageService.getTableBytes(BASE_NAME, DEV_ENVIRONMENT_ID);

        assertTrue(oneRowBytes > 0);

        dataTableRowService.insertRows(BASE_NAME, List.of(Map.of("title", "y".repeat(250))), DEV_ENVIRONMENT_ID);

        assertTrue(dataTableStorageService.getTableBytes(BASE_NAME, DEV_ENVIRONMENT_ID) > oneRowBytes + 250);
    }

    @Test
    void testGetTableBytesRejectsAnInvalidTableName() {
        assertThrows(
            IllegalArgumentException.class,
            () -> dataTableStorageService.getTableBytes("orders\"; DROP TABLE x; --", DEV_ENVIRONMENT_ID));
    }

    private long getAllocatedBytes() {
        Long allocatedBytes = jdbcTemplate.queryForObject(DataTableStorageServiceImpl.USAGE_SQL, Long.class);

        return allocatedBytes == null ? 0 : allocatedBytes;
    }

    private ApplicationProperties.DataTable getDataTable() {
        return applicationProperties.getDataTable();
    }

    private long getLiveBytes() {
        List<String> physicalTableNames = jdbcTemplate.queryForList(
            DataTableStorageServiceImpl.TABLE_NAMES_SQL, String.class);

        return physicalTableNames.stream()
            .mapToLong(this::getTableBytes)
            .sum();
    }

    private long getTableBytes(String physicalTableName) {
        String[] physicalTableNameParts = physicalTableName.split("_", 3);

        return dataTableStorageService.getTableBytes(
            physicalTableNameParts[2], Long.parseLong(physicalTableNameParts[1]));
    }

    private void insertRows(int count) {
        List<Map<String, Object>> rowValues = IntStream.range(0, count)
            .<Map<String, Object>>mapToObj(index -> Map.of("title", index + "-" + "x".repeat(200)))
            .toList();

        dataTableRowService.insertRows(BASE_NAME, rowValues, DEV_ENVIRONMENT_ID);
    }

    private void insertRowsAndDeleteAllButTheLast() {
        insertRows(500);

        jdbcTemplate.update(
            "DELETE FROM \"" + PHYSICAL_NAME + "\" WHERE id < (SELECT MAX(id) FROM \"" + PHYSICAL_NAME + "\")");
    }

    private void setLimit(long maxSizeBytes) {
        ApplicationProperties.DataTable dataTable = getDataTable();

        dataTable.setMaxSizeBytes(maxSizeBytes);
    }
}
