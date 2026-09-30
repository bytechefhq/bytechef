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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.data.table.domain.DataTableRef;
import com.bytechef.platform.data.table.exception.DataTableStorageLimitExceededException;
import com.bytechef.platform.data.table.execution.domain.CreateStrategy;
import com.bytechef.platform.data.table.execution.domain.NewRow;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

class DataTableRowServiceEnforcementTest {

    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final DataTableStorageService dataTableStorageService = mock(DataTableStorageService.class);

    private final DataTableRowServiceImpl dataTableRowService =
        new DataTableRowServiceImpl(eventPublisher, jdbcTemplate, dataTableStorageService);

    @Test
    void testInsertRowBlockedWhenOverLimit() {
        doThrow(new DataTableStorageLimitExceededException(60_000_000L, 52_428_800L))
            .when(dataTableStorageService)
            .checkWithinLimit(anyLong());

        assertThatThrownBy(
            () -> dataTableRowService.insertRow(dataTableRef(),
                Map.of("name", "x")))
                    .isInstanceOf(DataTableStorageLimitExceededException.class);

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void testUpdateRowBlockedWhenOverLimit() {
        doThrow(new DataTableStorageLimitExceededException(60_000_000L, 52_428_800L))
            .when(dataTableStorageService)
            .checkWithinLimit(0);

        assertThatThrownBy(
            () -> dataTableRowService.updateRow(dataTableRef(), 1,
                Map.of("name", "x")))
                    .isInstanceOf(DataTableStorageLimitExceededException.class);

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void testInsertRowThatWouldCrossTheLimitIsBlocked() {
        DataTableRowServiceImpl nearLimitDataTableRowService = createNearLimitDataTableRowService();

        assertThatThrownBy(
            () -> nearLimitDataTableRowService.insertRow(dataTableRef(), Map.of("name", "x".repeat(100))))
                .isInstanceOf(DataTableStorageLimitExceededException.class);

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void testUpsertRowThatWouldCrossTheLimitIsBlocked() {
        DataTableRowServiceImpl nearLimitDataTableRowService = createNearLimitDataTableRowService();

        assertThatThrownBy(
            () -> nearLimitDataTableRowService.upsertRow(dataTableRef(), "ORD-1", Map.of("name", "x".repeat(100))))
                .isInstanceOf(DataTableStorageLimitExceededException.class);

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void testInsertRowsThatWouldCrossTheLimitIsBlocked() {
        DataTableRowServiceImpl nearLimitDataTableRowService = createNearLimitDataTableRowService();

        assertThatThrownBy(
            () -> nearLimitDataTableRowService.insertRows(
                dataTableRef(), List.of(new NewRow(Map.of("name", "x".repeat(100)), null)), CreateStrategy.INSERT))
                    .isInstanceOf(DataTableStorageLimitExceededException.class);

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void testImportCsvBlockedWhenOverLimit() {
        doThrow(new DataTableStorageLimitExceededException(60_000_000L, 52_428_800L))
            .when(dataTableStorageService)
            .checkWithinLimit(anyLong());

        assertThatThrownBy(
            () -> dataTableRowService.importCsv(dataTableRef(), "name\nx\n"))
                .isInstanceOf(DataTableStorageLimitExceededException.class);

        verifyNoInteractions(jdbcTemplate);
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private DataTableRowServiceImpl createNearLimitDataTableRowService() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getDataTable()
            .setMaxSizeBytes(1_000L);

        JdbcTemplate storageJdbcTemplate = mock(JdbcTemplate.class);

        when(storageJdbcTemplate.queryForObject(eq(DataTableStorageServiceImpl.USAGE_SQL), eq(Long.class)))
            .thenReturn(990L);

        return new DataTableRowServiceImpl(
            eventPublisher, jdbcTemplate, new DataTableStorageServiceImpl(applicationProperties, storageJdbcTemplate));
    }

    private static DataTableRef dataTableRef() {
        return new DataTableRef(1051L, 0L);
    }
}
