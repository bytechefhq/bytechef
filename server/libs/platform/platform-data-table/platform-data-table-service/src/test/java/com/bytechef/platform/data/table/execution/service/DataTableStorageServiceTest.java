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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.data.table.domain.DataTableStorageUsage;
import com.bytechef.platform.data.table.exception.DataTableStorageLimitExceededException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
class DataTableStorageServiceTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    private DataTableStorageServiceImpl createService(long limit, long used) {
        return createService(limit, used, used);
    }

    private DataTableStorageServiceImpl createService(long limit, long allocatedBytes, long liveBytes) {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getDataTable()
            .setMaxSizeBytes(limit);

        when(jdbcTemplate.queryForObject(eq(DataTableStorageServiceImpl.USAGE_SQL), eq(Long.class)))
            .thenReturn(allocatedBytes);
        when(jdbcTemplate.queryForList(eq(DataTableStorageServiceImpl.TABLE_NAMES_SQL), eq(String.class)))
            .thenReturn(List.of("dt_0_orders"));
        when(jdbcTemplate.queryForObject(startsWith("SELECT COALESCE(SUM(pg_column_size"), eq(Long.class)))
            .thenReturn(liveBytes);

        return new DataTableStorageServiceImpl(applicationProperties, jdbcTemplate);
    }

    @Test
    void testGetUsageComputesPercentage() {
        DataTableStorageUsage usage = createService(52_428_800L, 26_214_400L).getUsage();

        assertThat(usage.usedBytes()).isEqualTo(26_214_400L);
        assertThat(usage.limitBytes()).isEqualTo(52_428_800L);
        assertThat(usage.percentage()).isEqualTo(50.0);
        assertThat(usage.unlimited()).isFalse();
    }

    @Test
    void testGetUsageUnlimitedWhenLimitZero() {
        DataTableStorageUsage usage = createService(0L, 26_214_400L).getUsage();

        assertThat(usage.unlimited()).isTrue();
        assertThat(usage.percentage()).isEqualTo(0.0);
    }

    @Test
    void testCheckWithinLimitThrowsWhenOver() {
        assertThatThrownBy(() -> createService(52_428_800L, 52_428_800L).checkWithinLimit(1))
            .isInstanceOf(DataTableStorageLimitExceededException.class);
    }

    @Test
    void testCheckWithinLimitPassesWhenDeletedRowsLeaveRoomInAllocatedSpace() {
        createService(52_428_800L, 52_428_800L, 10_485_760L).checkWithinLimit(1_048_576);
    }

    @Test
    void testCheckWithinLimitThrowsWhenLiveRowsLeaveNoRoom() {
        assertThatThrownBy(() -> createService(52_428_800L, 60_000_000L, 52_000_000L).checkWithinLimit(1_048_576))
            .isInstanceOf(DataTableStorageLimitExceededException.class);
    }

    @Test
    void testGetUsageReportsLiveRowsWhenAllocatedSpaceExceedsTheLimit() {
        DataTableStorageUsage usage = createService(52_428_800L, 60_000_000L, 26_214_400L).getUsage();

        assertThat(usage.usedBytes()).isEqualTo(26_214_400L);
        assertThat(usage.percentage()).isEqualTo(50.0);
    }

    @Test
    void testGetTableBytesMeasuresTheLiveRowsOfTheTable() {
        assertThat(createService(52_428_800L, 0L, 4_096L).getTableBytes("dt_0_orders")).isEqualTo(4_096L);
    }

    @Test
    void testGetTableBytesRejectsAnInvalidTableName() {
        DataTableStorageServiceImpl dataTableStorageService = createService(52_428_800L, 0L, 0L);

        assertThatThrownBy(() -> dataTableStorageService.getTableBytes("orders\"; DROP TABLE x; --"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testCheckWithinLimitPassesWhenUnlimited() {
        createService(0L, 999_999_999L).checkWithinLimit(1_000_000);
    }
}
