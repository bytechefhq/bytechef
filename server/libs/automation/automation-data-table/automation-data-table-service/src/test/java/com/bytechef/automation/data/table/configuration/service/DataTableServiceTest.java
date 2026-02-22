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

package com.bytechef.automation.data.table.configuration.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.data.table.configuration.domain.DataTable;
import com.bytechef.automation.data.table.configuration.domain.WorkspaceDataTable;
import com.bytechef.automation.data.table.configuration.repository.DataTableRepository;
import com.bytechef.automation.data.table.configuration.repository.WorkspaceDataTableRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Mock
    private WorkspaceDataTableRepository workspaceDataTableRepository;

    private DataTableServiceImpl dataTableService;

    @BeforeEach
    void setUp() {
        dataTableService = new DataTableServiceImpl(dataTableRepository, jdbcTemplate, workspaceDataTableRepository);
    }

    @Test
    void testDropTableShouldDeleteMetadataWhenNoPhysicalTablesRemain() {
        DataTable dataTable = new DataTable(1L, "mytable");
        WorkspaceDataTable workspaceDataTable = new WorkspaceDataTable(1L, 100L);

        when(jdbcTemplate.queryForObject(
            anyString(), eq(Integer.class), anyString()))
                .thenReturn(0);
        when(dataTableRepository.findByName("mytable"))
            .thenReturn(Optional.of(dataTable));
        when(workspaceDataTableRepository.findByDataTableId(1L))
            .thenReturn(List.of(workspaceDataTable));

        dataTableService.dropTable("mytable", 1L);

        verify(workspaceDataTableRepository).deleteAll(List.of(workspaceDataTable));
        verify(dataTableRepository).deleteByName("mytable");
    }

    @Test
    void testDropTableShouldPreserveMetadataWhenPhysicalTablesExistInOtherEnvironments() {
        when(jdbcTemplate.queryForObject(
            anyString(), eq(Integer.class), anyString()))
                .thenReturn(1);

        dataTableService.dropTable("mytable", 1L);

        verify(dataTableRepository, never()).findByName(anyString());
        verify(workspaceDataTableRepository, never()).deleteAll(any());
        verify(dataTableRepository, never()).deleteByName(anyString());
    }
}
