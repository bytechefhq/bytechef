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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.platform.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.event.DataTableWebhookEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
@SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
class DataTableRowServiceTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private DataTableRowServiceImpl dataTableRowService;

    @BeforeEach
    void setUp() {
        dataTableRowService = new DataTableRowServiceImpl(applicationEventPublisher, jdbcTemplate);
    }

    @Test
    void testDeleteRowPublishesTheDeletedRowValues() {
        stubQueries(new DataTableRow(7, Map.of("status", "CLOSED")));

        assertTrue(dataTableRowService.deleteRow("conversations", 7, 1));

        assertEquals(Map.of("id", 7L, "values", Map.of("status", "CLOSED")), publishedPayload());
    }

    @Test
    void testDeleteRowReadsAndDeletesInASingleStatement() {
        stubQueries(new DataTableRow(7, Map.of("status", "CLOSED")));

        dataTableRowService.deleteRow("conversations", 7, 1);

        List<String> executedSqls = executedSqls();

        assertTrue(
            executedSqls.stream()
                .anyMatch(sql -> sql.startsWith("DELETE FROM") && sql.contains(" RETURNING ")),
            "expected the row to be returned by the delete itself, got " + executedSqls);

        assertTrue(
            executedSqls.stream()
                .noneMatch(sql -> sql.startsWith("SELECT \"id\"")),
            "the row must not be read in a statement separate from the delete, got " + executedSqls);

        assertEquals(
            1,
            executedSqls.stream()
                .filter(sql -> sql.contains("ORDER BY ordinal_position"))
                .count(),
            "the column metadata must be read once per delete, got " + executedSqls);

        verify(jdbcTemplate, never()).update(anyString(), any(PreparedStatementSetter.class));
    }

    @Test
    void testDeleteRowOfMissingRowPublishesNothing() {
        stubQueries(null);

        assertFalse(dataTableRowService.deleteRow("conversations", 7, 1));

        verifyNoInteractions(applicationEventPublisher);
    }

    private List<String> executedSqls() {
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        verify(jdbcTemplate, atLeastOnce())
            .query(sqlCaptor.capture(), any(PreparedStatementSetter.class), any(RowMapper.class));

        return sqlCaptor.getAllValues();
    }

    private Map<String, Object> publishedPayload() {
        ArgumentCaptor<DataTableWebhookEvent> eventCaptor = ArgumentCaptor.forClass(DataTableWebhookEvent.class);

        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

        DataTableWebhookEvent dataTableWebhookEvent = eventCaptor.getValue();

        assertEquals(DataTableWebhookType.RECORD_DELETED, dataTableWebhookEvent.getType());

        return dataTableWebhookEvent.getPayload();
    }

    @SuppressWarnings("unchecked")
    private void stubQueries(DataTableRow dataTableRow) {
        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
            .thenAnswer(invocation -> {
                String sql = invocation.getArgument(0);

                if (sql.contains("column_name = 'id'")) {
                    return List.of(1);
                }

                if (sql.contains("ORDER BY ordinal_position")) {
                    return List.of(new ColumnSpec("id", ColumnType.INTEGER), new ColumnSpec("status",
                        ColumnType.STRING));
                }

                return dataTableRow == null ? List.of() : List.of(dataTableRow);
            });
    }
}
