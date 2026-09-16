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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.data.table.configuration.domain.DataTable;
import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
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
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
@SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
class DataTableServiceTest {

    private static final long DATA_TABLE_ID = 7L;

    @Mock
    private DataTableRepository dataTableRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private DataTableServiceImpl dataTableService;

    @BeforeEach
    void setUp() {
        dataTableService = new DataTableServiceImpl(
            dataTableRepository, jdbcTemplate);
    }

    /**
     * {@code DataTableDdlOwnerColumnsTest} pins what the shared statements say; this pins that {@code createTable} is
     * the caller of them, so the two cannot be right separately and wrong together.
     */
    @Test
    void testCreateTableExecutesTheSharedCreateStatement() {
        when(dataTableRepository.save(any(DataTable.class))).thenReturn(new DataTable(1L, "conversations"));

        List<ColumnSpec> columnSpecs = List.of(new ColumnSpec("title", ColumnType.STRING));

        dataTableService.createTable(
            "conversations", null, columnSpecs, 0);

        ArgumentCaptor<String> sqlArgumentCaptor = ArgumentCaptor.forClass(String.class);

        verify(jdbcTemplate, atLeastOnce()).execute(sqlArgumentCaptor.capture());

        List<String> executedSqls = sqlArgumentCaptor.getAllValues();

        assertTrue(
            executedSqls.contains(DataTableServiceImpl.buildCreateTableSql("dt_0_conversations", columnSpecs)),
            "createTable must build its DDL through buildCreateTableSql: " + executedSqls);

    }

    @Test
    void testCreateTableRejectsAReservedColumnName() {
        DataTableException dataTableException = assertThrows(
            DataTableException.class,
            () -> dataTableService.createTable(
                "conversations", null, List.of(new ColumnSpec("external_id", ColumnType.STRING)), 0));

        assertEquals(DataTableErrorType.COLUMN_NAME_INVALID.getErrorKey(), dataTableException.getErrorKey());
    }

    @Test
    void testDropTableShouldDeleteMetadataWhenNoPhysicalTablesRemain() {
        givenRegistryRow();

        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
            .thenReturn(List.of());

        dataTableService.dropTable("mytable", 1L);

        verify(jdbcTemplate).execute(contains("DROP TABLE IF EXISTS"));
        verify(dataTableRepository).deleteById(DATA_TABLE_ID);
    }

    @Test
    void testDropTableShouldPreserveMetadataWhenPhysicalTablesExistInOtherEnvironments() {
        givenRegistryRow();

        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
            .thenReturn(List.of("dt_2_mytable"));

        dataTableService.dropTable("mytable", 1L);

        verify(jdbcTemplate).execute(contains("DROP TABLE IF EXISTS"));
        verify(dataTableRepository, never()).deleteById(anyLong());
    }

    /**
     * A registry row for the table being dropped. Without one, {@code dropTable} resolves nothing and returns before
     * touching the database at all, which would make both drop tests pass vacuously.
     */
    private void givenRegistryRow() {
        DataTable dataTable = new DataTable(DATA_TABLE_ID, "mytable");

        when(dataTableRepository.findByName("mytable"))
            .thenReturn(Optional.of(dataTable));
    }
}
