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
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.DataTableRef;
import com.bytechef.platform.data.table.exception.DataTableStorageLimitExceededException;
import com.bytechef.platform.data.table.execution.domain.CreateStrategy;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.NewRow;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

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
            .checkWithinLimit(anyLong());

        assertThatThrownBy(
            () -> spyWithCurrentRow(dataTableRowService, Map.of()).updateRow(dataTableRef(), 1,
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
    void testUpdateRowThatWouldCrossTheLimitIsBlocked() {
        DataTableRowServiceImpl nearLimitDataTableRowService = spyWithCurrentRow(
            createNearLimitDataTableRowService(), Map.of());

        assertThatThrownBy(
            () -> nearLimitDataTableRowService.updateRow(dataTableRef(), 1, Map.of("name", "x".repeat(100))))
                .isInstanceOf(DataTableStorageLimitExceededException.class);

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void testUpdateRowNearTheLimitChecksOnlyTheGrowthOfTheRow() {
        doThrow(new DataTableStorageLimitExceededException(52_000_000L, 52_428_800L))
            .when(dataTableStorageService)
            .checkWithinLimit(anyLong());

        DataTableRowServiceImpl spiedDataTableRowService = spyWithCurrentRow(
            dataTableRowService, Map.of("name", "x".repeat(100)));

        assertThatThrownBy(
            () -> spiedDataTableRowService.updateRow(dataTableRef(), 1, Map.of("name", "x".repeat(10_100))))
                .isInstanceOf(DataTableStorageLimitExceededException.class);

        verify(dataTableStorageService).checkWithinLimit(10_100L);
        verify(dataTableStorageService).checkWithinLimit(10_000L);
    }

    @Test
    void testUpdateRowThatDoesNotGrowTheRowPassesTheStorageCheckNearTheLimit() {
        doThrow(new DataTableStorageLimitExceededException(52_000_000L, 52_428_800L))
            .when(dataTableStorageService)
            .checkWithinLimit(anyLong());

        DataTableRowServiceImpl spiedDataTableRowService = spyWithCurrentRow(
            dataTableRowService, Map.of("name", "x".repeat(10_000)));

        Throwable throwable = catchThrowable(
            () -> spiedDataTableRowService.updateRow(dataTableRef(), 1, Map.of("name", "y")));

        assertThat(throwable).isNotInstanceOf(DataTableStorageLimitExceededException.class);

        verify(dataTableStorageService, times(1)).checkWithinLimit(anyLong());
    }

    @Test
    void testUpsertRowThatWouldCrossTheLimitIsBlocked() {
        DataTableRowServiceImpl nearLimitDataTableRowService = spy(createNearLimitDataTableRowService());

        doReturn(Optional.empty()).when(nearLimitDataTableRowService)
            .fetchRowByExternalId(any(), anyString());

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

    @Test
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    void testImportCsvChecksTheEstimatedSizeOfTheParsedRows() {
        when(jdbcTemplate.query(
            startsWith("SELECT column_name, data_type FROM information_schema.columns"),
            any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn((List) List.of(new ColumnSpec("id", ColumnType.INTEGER),
                    new ColumnSpec("name", ColumnType.STRING)));
        doThrow(new DataTableStorageLimitExceededException(52_000_000L, 52_428_800L))
            .when(dataTableStorageService)
            .checkWithinLimit(longThat(incomingBytes -> incomingBytes > 100));

        assertThatThrownBy(
            () -> dataTableRowService.importCsv(dataTableRef(), "name\nx\ny\n"))
                .isInstanceOf(DataTableStorageLimitExceededException.class);

        verify(dataTableStorageService).checkWithinLimit(130L);
    }

    @Test
    void testUpsertRowOfAnExistingRowNearTheLimitChecksOnlyTheGrowth() {
        doThrow(new DataTableStorageLimitExceededException(52_000_000L, 52_428_800L))
            .when(dataTableStorageService)
            .checkWithinLimit(anyLong());

        DataTableRowServiceImpl spiedDataTableRowService = spyWithExistingRows(
            Map.of("ORD-1", Map.of("name", "x".repeat(100))));

        assertThatThrownBy(
            () -> spiedDataTableRowService.upsertRow(dataTableRef(), "ORD-1", Map.of("name", "x".repeat(10_100))))
                .isInstanceOf(DataTableStorageLimitExceededException.class);

        verify(dataTableStorageService).checkWithinLimit(10_000L);
    }

    @Test
    void testUpsertRowThatDoesNotGrowAnExistingRowPassesTheStorageCheckNearTheLimit() {
        doThrow(new DataTableStorageLimitExceededException(52_000_000L, 52_428_800L))
            .when(dataTableStorageService)
            .checkWithinLimit(anyLong());

        DataTableRowServiceImpl spiedDataTableRowService = spyWithExistingRows(
            Map.of("ORD-1", Map.of("name", "x".repeat(10_000))));

        Throwable throwable = catchThrowable(
            () -> spiedDataTableRowService.upsertRow(dataTableRef(), "ORD-1", Map.of("name", "y")));

        assertThat(throwable).isNotInstanceOf(DataTableStorageLimitExceededException.class);

        verify(dataTableStorageService, times(1)).checkWithinLimit(anyLong());
    }

    @Test
    void testInsertRowsUpsertChargesAnExternalIdRepeatedInTheBatchOnce() {
        doThrow(new DataTableStorageLimitExceededException(52_000_000L, 52_428_800L))
            .when(dataTableStorageService)
            .checkWithinLimit(anyLong());

        DataTableRowServiceImpl spiedDataTableRowService = spyWithExistingRows(
            Map.of("ORD-1", Map.of("name", "x".repeat(1_000))));

        assertThatThrownBy(
            () -> spiedDataTableRowService.insertRows(
                dataTableRef(),
                List.of(
                    new NewRow(Map.of("name", "y"), "ORD-1"),
                    new NewRow(Map.of("name", "y"), "ORD-1"),
                    new NewRow(Map.of("name", "z".repeat(1_500)), "ORD-3")),
                CreateStrategy.UPSERT))
                    .isInstanceOf(DataTableStorageLimitExceededException.class);

        verify(dataTableStorageService).checkWithinLimit(570L);
    }

    @Test
    void testInsertRowsUpsertNearTheLimitChargesExistingKeysTheirGrowthAndNewKeysInFull() {
        doThrow(new DataTableStorageLimitExceededException(52_000_000L, 52_428_800L))
            .when(dataTableStorageService)
            .checkWithinLimit(anyLong());

        DataTableRowServiceImpl spiedDataTableRowService = spyWithExistingRows(
            Map.of("ORD-1", Map.of("name", "x".repeat(100))));

        assertThatThrownBy(
            () -> spiedDataTableRowService.insertRows(
                dataTableRef(),
                List.of(
                    new NewRow(Map.of("name", "x".repeat(100)), "ORD-1"),
                    new NewRow(Map.of("name", "y"), "ORD-2")),
                CreateStrategy.UPSERT))
                    .isInstanceOf(DataTableStorageLimitExceededException.class);

        verify(dataTableStorageService).checkWithinLimit(70L);
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private DataTableRowServiceImpl createNearLimitDataTableRowService() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getDataTable()
            .setMaxSizeBytes(1_000L);

        JdbcTemplate storageJdbcTemplate = mock(JdbcTemplate.class);

        when(storageJdbcTemplate.queryForObject(eq(DataTableStorageServiceImpl.USAGE_SQL), eq(Long.class)))
            .thenReturn(990L);
        when(storageJdbcTemplate.queryForList(eq(DataTableStorageServiceImpl.TABLE_NAMES_SQL), eq(String.class)))
            .thenReturn(List.of("dt_0_7"));
        when(storageJdbcTemplate.queryForObject(startsWith("SELECT COALESCE(SUM(pg_column_size"), eq(Long.class)))
            .thenReturn(990L);

        return new DataTableRowServiceImpl(
            eventPublisher, jdbcTemplate, new DataTableStorageServiceImpl(applicationProperties, storageJdbcTemplate));
    }

    private static DataTableRef dataTableRef() {
        return new DataTableRef(1051L, 0L);
    }

    private static DataTableRowServiceImpl spyWithCurrentRow(
        DataTableRowServiceImpl dataTableRowService, Map<String, Object> currentValues) {

        DataTableRowServiceImpl spiedDataTableRowService = spy(dataTableRowService);

        doReturn(new DataTableRow(1, null, currentValues)).when(spiedDataTableRowService)
            .getRow(dataTableRef(), 1);

        return spiedDataTableRowService;
    }

    private DataTableRowServiceImpl spyWithExistingRows(Map<String, Map<String, Object>> existingValuesByExternalId) {
        DataTableRowServiceImpl spiedDataTableRowService = spy(dataTableRowService);

        doReturn(Optional.empty()).when(spiedDataTableRowService)
            .fetchRowByExternalId(any(), anyString());

        existingValuesByExternalId.forEach((externalId, values) -> doReturn(
            Optional.of(new DataTableRow(1, externalId, values))).when(spiedDataTableRowService)
                .fetchRowByExternalId(any(), eq(externalId)));

        return spiedDataTableRowService;
    }
}
