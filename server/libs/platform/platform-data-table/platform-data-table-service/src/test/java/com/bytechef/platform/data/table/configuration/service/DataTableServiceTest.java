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

package com.bytechef.platform.data.table.configuration.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.data.table.configuration.audit.DataTableAuditPublisher;
import com.bytechef.platform.data.table.configuration.domain.DataTable;
import com.bytechef.platform.data.table.configuration.repository.DataTableRepository;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
@SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
class DataTableServiceTest {

    @Mock
    private DataTableRepository dataTableRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private DataTableServiceImpl dataTableService;

    @BeforeEach
    void setUp() {
        dataTableService = new DataTableServiceImpl(
            mock(DataTableAuditPublisher.class), dataTableRepository, jdbcTemplate);
    }

    @Test
    void testCreateTableEmitsReservedOwnerColumns() {
        when(dataTableRepository.findByName("conversations")).thenReturn(
            Optional.of(new DataTable(1L, "conversations")));

        dataTableService.createTable(
            "conversations", null, List.of(new ColumnSpec("title", ColumnType.STRING)), 0);

        ArgumentCaptor<String> sqlArgumentCaptor = ArgumentCaptor.forClass(String.class);

        verify(jdbcTemplate, atLeastOnce()).execute(sqlArgumentCaptor.capture());

        String createTableSql = sqlArgumentCaptor.getAllValues()
            .stream()
            .filter(sql -> sql.startsWith("CREATE TABLE"))
            .findFirst()
            .orElseThrow();

        assertTrue(createTableSql.contains("\"owner_id\" BIGINT"));
        assertTrue(createTableSql.contains("\"owner_type\" INT"));
    }

    @Test
    void testCreateTableRejectsAReservedColumnName() {
        assertThrows(
            IllegalArgumentException.class,
            () -> dataTableService.createTable(
                "conversations", null, List.of(new ColumnSpec("owner_id", ColumnType.STRING)), 0));
    }

    @Test
    void testDropTableShouldDeleteMetadataWhenNoPhysicalTablesRemain() {
        when(jdbcTemplate.queryForObject(
            anyString(), eq(Integer.class), anyString()))
                .thenReturn(0);

        dataTableService.dropTable("mytable", 1L);

        verify(jdbcTemplate).execute(contains("DROP TABLE IF EXISTS"));
        verify(dataTableRepository).deleteByName("mytable");
    }

    @Test
    void testDropTableShouldPreserveMetadataWhenPhysicalTablesExistInOtherEnvironments() {
        when(jdbcTemplate.queryForObject(
            anyString(), eq(Integer.class), anyString()))
                .thenReturn(1);

        dataTableService.dropTable("mytable", 1L);

        verify(jdbcTemplate).execute(contains("DROP TABLE IF EXISTS"));
        verify(dataTableRepository, never()).findByName(anyString());
        verify(dataTableRepository, never()).deleteByName(anyString());
    }
}
