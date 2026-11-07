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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.platform.data.table.exception.DataTableStorageLimitExceededException;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

        assertThatThrownBy(() -> dataTableRowService.insertRow("orders", Map.of("name", "x"), 1))
            .isInstanceOf(DataTableStorageLimitExceededException.class);

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void testUpdateRowBlockedWhenOverLimit() {
        doThrow(new DataTableStorageLimitExceededException(60_000_000L, 52_428_800L))
            .when(dataTableStorageService)
            .checkWithinLimit(anyLong());

        DataTableRowServiceImpl spiedDataTableRowService = spyWithCurrentRow(Map.of());

        assertThatThrownBy(() -> spiedDataTableRowService.updateRow("orders", 1, Map.of("name", "x"), 1))
            .isInstanceOf(DataTableStorageLimitExceededException.class);

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void testInsertRowChecksTheEstimatedSizeOfTheRow() {
        doThrow(new DataTableStorageLimitExceededException(52_000_000L, 52_428_800L))
            .when(dataTableStorageService)
            .checkWithinLimit(anyLong());

        String largeValue = "x".repeat(10_000);

        assertThatThrownBy(() -> dataTableRowService.insertRow("orders", Map.of("name", largeValue), 1))
            .isInstanceOf(DataTableStorageLimitExceededException.class);

        ArgumentCaptor<Long> incomingBytesCaptor = ArgumentCaptor.forClass(Long.class);

        verify(dataTableStorageService).checkWithinLimit(incomingBytesCaptor.capture());

        assertThat(incomingBytesCaptor.getValue()).isGreaterThan(10_000L);
    }

    @Test
    void testUpdateRowNearTheLimitChecksOnlyTheGrowthOfTheRow() {
        doThrow(new DataTableStorageLimitExceededException(52_000_000L, 52_428_800L))
            .when(dataTableStorageService)
            .checkWithinLimit(anyLong());

        DataTableRowServiceImpl spiedDataTableRowService = spyWithCurrentRow(Map.of("name", "x".repeat(100)));

        assertThatThrownBy(
            () -> spiedDataTableRowService.updateRow("orders", 1, Map.of("name", "x".repeat(10_100)), 1))
                .isInstanceOf(DataTableStorageLimitExceededException.class);

        verify(dataTableStorageService).checkWithinLimit(10_100L);
        verify(dataTableStorageService).checkWithinLimit(10_000L);
    }

    @Test
    void testUpdateRowThatDoesNotGrowTheRowPassesTheStorageCheckNearTheLimit() {
        doThrow(new DataTableStorageLimitExceededException(52_000_000L, 52_428_800L))
            .when(dataTableStorageService)
            .checkWithinLimit(anyLong());

        DataTableRowServiceImpl spiedDataTableRowService = spyWithCurrentRow(Map.of("name", "x".repeat(10_000)));

        Throwable throwable = catchThrowable(
            () -> spiedDataTableRowService.updateRow("orders", 1, Map.of("name", "y"), 1));

        assertThat(throwable).isNotInstanceOf(DataTableStorageLimitExceededException.class);

        verify(dataTableStorageService, times(1)).checkWithinLimit(anyLong());
    }

    @Test
    void testImportCsvBlockedWhenOverLimit() {
        doThrow(new DataTableStorageLimitExceededException(60_000_000L, 52_428_800L))
            .when(dataTableStorageService)
            .checkWithinLimit(anyLong());

        assertThatThrownBy(() -> dataTableRowService.importCsv("orders", "name\nx\n", 1))
            .isInstanceOf(DataTableStorageLimitExceededException.class);

        verifyNoInteractions(jdbcTemplate);
    }

    private DataTableRowServiceImpl spyWithCurrentRow(Map<String, Object> currentValues) {
        DataTableRowServiceImpl spiedDataTableRowService = spy(dataTableRowService);

        doReturn(new DataTableRow(1, currentValues)).when(spiedDataTableRowService)
            .getRow("orders", 1, 1);

        return spiedDataTableRowService;
    }
}
