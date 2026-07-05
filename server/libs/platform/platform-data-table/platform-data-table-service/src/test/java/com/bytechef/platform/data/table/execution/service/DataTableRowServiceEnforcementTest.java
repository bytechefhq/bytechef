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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.platform.data.table.exception.DataTableStorageLimitExceededException;
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
            .checkWithinLimit(0);

        assertThatThrownBy(() -> dataTableRowService.insertRow("orders", Map.of("name", "x"), 1))
            .isInstanceOf(DataTableStorageLimitExceededException.class);

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void testUpdateRowBlockedWhenOverLimit() {
        doThrow(new DataTableStorageLimitExceededException(60_000_000L, 52_428_800L))
            .when(dataTableStorageService)
            .checkWithinLimit(0);

        assertThatThrownBy(() -> dataTableRowService.updateRow("orders", 1, Map.of("name", "x"), 1))
            .isInstanceOf(DataTableStorageLimitExceededException.class);

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void testImportCsvBlockedWhenOverLimit() {
        doThrow(new DataTableStorageLimitExceededException(60_000_000L, 52_428_800L))
            .when(dataTableStorageService)
            .checkWithinLimit(org.mockito.ArgumentMatchers.anyLong());

        assertThatThrownBy(() -> dataTableRowService.importCsv("orders", "name\nx\n", 1))
            .isInstanceOf(DataTableStorageLimitExceededException.class);

        verifyNoInteractions(jdbcTemplate);
    }
}
