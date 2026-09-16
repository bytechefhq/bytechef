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
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

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
        when(dataTableRepository.findByWorkspaceIdAndName(1L, "conversations")).thenReturn(Optional.empty());
        when(dataTableRepository.save(any(DataTable.class))).thenReturn(new DataTable(1051L, "conversations"));

        List<ColumnSpec> columnSpecs = List.of(new ColumnSpec("title", ColumnType.STRING));

        long dataTableId = dataTableService.createTable(1L, "conversations", null, columnSpecs, 0);

        ArgumentCaptor<String> sqlArgumentCaptor = ArgumentCaptor.forClass(String.class);

        verify(jdbcTemplate, atLeastOnce()).execute(sqlArgumentCaptor.capture());

        List<String> executedSqls = sqlArgumentCaptor.getAllValues();

        assertEquals(1051L, dataTableId);
        assertTrue(
            executedSqls.contains(DataTableServiceImpl.buildCreateTableSql("dt_0_1051", columnSpecs)),
            "createTable must build its DDL through buildCreateTableSql: " + executedSqls);
    }

    @Test
    void testCreateTableReusesTheWorkspaceRowInASecondEnvironment() {
        when(dataTableRepository.findByWorkspaceIdAndName(1L, "conversations"))
            .thenReturn(Optional.of(new DataTable(1051L, "conversations")));

        List<ColumnSpec> columnSpecs = List.of(new ColumnSpec("title", ColumnType.STRING));

        long dataTableId = dataTableService.createTable(1L, "conversations", null, columnSpecs, 2);

        assertEquals(1051L, dataTableId);
        verify(dataTableRepository, never()).save(any(DataTable.class));
        verify(jdbcTemplate).execute(DataTableServiceImpl.buildCreateTableSql("dt_2_1051", columnSpecs));
    }

    @Test
    void testCreateTableRejectsAReservedColumnName() {
        DataTableException dataTableException = assertThrows(
            DataTableException.class,
            () -> dataTableService.createTable(
                1L, "conversations", null, List.of(new ColumnSpec("external_id", ColumnType.STRING)), 0));

        assertEquals(DataTableErrorType.COLUMN_NAME_INVALID.getErrorKey(), dataTableException.getErrorKey());
    }

    @Test
    void testDropTableShouldDeleteMetadataWhenNoPhysicalTablesRemain() {
        givenRegistryRow();

        dataTableService.dropTable(DATA_TABLE_ID, 1L);

        verify(jdbcTemplate).execute("DROP TABLE IF EXISTS \"dt_1_7\"");
        verify(dataTableRepository).deleteById(DATA_TABLE_ID);
    }

    @Test
    void testDropTableShouldPreserveMetadataWhenPhysicalTablesExistInOtherEnvironments() {
        givenRegistryRow();

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any()))
            .thenAnswer(invocation -> "dt_2_7".equals(invocation.getArgument(2)) ? 1 : 0);

        dataTableService.dropTable(DATA_TABLE_ID, 1L);

        verify(jdbcTemplate).execute(contains("DROP TABLE IF EXISTS"));
        verify(dataTableRepository, never()).deleteById(anyLong());
    }

    @Test
    void testDropTableOfAnUnknownIdDoesNothing() {
        when(dataTableRepository.findById(DATA_TABLE_ID)).thenReturn(Optional.empty());

        dataTableService.dropTable(DATA_TABLE_ID, 1L);

        verify(jdbcTemplate, never()).execute(anyString());
        verify(dataTableRepository, never()).deleteById(anyLong());
    }

    @Test
    void testFetchDataTableWithoutAWorkspaceLooksUpTheUnscopedRow() {
        DataTable dataTable = new DataTable(DATA_TABLE_ID, "mytable");

        when(dataTableRepository.findByWorkspaceIdIsNullAndName("mytable")).thenReturn(Optional.of(dataTable));

        assertEquals(Optional.of(dataTable), dataTableService.fetchDataTable(null, "MyTable"));
    }

    @Test
    void testRenameTableTranslatesAConcurrentDuplicateName() {
        givenRegistryRow();

        when(dataTableRepository.findByWorkspaceIdIsNullAndName("orders")).thenReturn(Optional.empty());
        when(dataTableRepository.save(any(DataTable.class))).thenThrow(new DuplicateKeyException("duplicate"));

        DataTableException dataTableException = assertThrows(
            DataTableException.class, () -> dataTableService.renameTable(DATA_TABLE_ID, "orders"));

        assertEquals("Data table 'orders' already exists in this workspace", dataTableException.getMessage());
        assertEquals(DataTableErrorType.DATA_TABLE_ALREADY_EXISTS.getErrorKey(), dataTableException.getErrorKey());
    }

    /**
     * A registry row for the table being dropped. Without one, {@code dropTable} resolves nothing and returns before
     * touching the database at all, which would make both drop tests pass vacuously.
     */
    private void givenRegistryRow() {
        DataTable dataTable = new DataTable(DATA_TABLE_ID, "mytable");

        when(dataTableRepository.findById(DATA_TABLE_ID))
            .thenReturn(Optional.of(dataTable));
    }
}
