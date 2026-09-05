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

package com.bytechef.platform.workflow.execution.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.dto.WorkflowExecutionRowDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
@SuppressFBWarnings(
    value = "SQL_INJECTION_SPRING_JDBC",
    justification = "Test class exercising parameterized query implementation; no actual SQL injection risk")
class WorkflowExecutionRowRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Captor
    private ArgumentCaptor<String> queryCaptor;

    @Captor
    private ArgumentCaptor<Object[]> argsCaptor;

    private WorkflowExecutionRowRepository workflowExecutionRowRepository;

    @BeforeEach
    void beforeEach() {
        workflowExecutionRowRepository = new WorkflowExecutionRowRepositoryImpl(jdbcTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testFailedJoblessTriggersAreUnionedInAndOrderedWithJobsByStartDate() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(2L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(
                new WorkflowExecutionRowDTO(WorkflowExecutionRowDTO.Kind.TRIGGER_EXECUTION, 77L),
                new WorkflowExecutionRowDTO(WorkflowExecutionRowDTO.Kind.JOB, 5L)));

        Page<WorkflowExecutionRowDTO> page = workflowExecutionRowRepository.findAll(
            null, null, null, List.of(9L), 1, List.of("workflow-1"), true, List.of("enc-a", "enc-b"),
            PageRequest.of(0, 20));

        verify(jdbcTemplate).query(queryCaptor.capture(), any(RowMapper.class), argsCaptor.capture());

        String query = queryCaptor.getValue();
        Object[] args = argsCaptor.getValue();

        assertThat(query).contains("UNION ALL");
        assertThat(query).contains("trigger_execution_job.job_id IS NULL");
        assertThat(query).contains("trigger_execution.status = ?");
        assertThat(query).contains("trigger_execution.workflow_execution_id IN(?,?)");
        assertThat(query).contains("ORDER BY start_date DESC NULLS LAST, id DESC LIMIT ? OFFSET ?");
        assertThat(args).containsSubsequence(
            9L, "workflow-1", TriggerExecution.Status.FAILED.ordinal(), "enc-a", "enc-b");
        assertThat(args[args.length - 2]).isEqualTo(20);
        assertThat(args[args.length - 1]).isEqualTo(0L);

        assertThat(page.getContent()).extracting(WorkflowExecutionRowDTO::kind)
            .containsExactly(WorkflowExecutionRowDTO.Kind.TRIGGER_EXECUTION, WorkflowExecutionRowDTO.Kind.JOB);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testTriggersStayOutWithoutWorkflowExecutionIds() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(1L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
            .thenReturn(List.of(new WorkflowExecutionRowDTO(WorkflowExecutionRowDTO.Kind.JOB, 5L)));

        workflowExecutionRowRepository.findAll(
            null, null, null, List.of(9L), 1, List.of("workflow-1"), true, List.of(), PageRequest.of(0, 20));

        verify(jdbcTemplate).query(queryCaptor.capture(), any(RowMapper.class), argsCaptor.capture());

        assertThat(queryCaptor.getValue()).doesNotContain("UNION ALL");
        assertThat(queryCaptor.getValue()).doesNotContain("trigger_execution");
    }

    @Test
    void testAnEmptyCountShortCircuitsToAnEmptyPage() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(0L);

        Page<WorkflowExecutionRowDTO> page = workflowExecutionRowRepository.findAll(
            null, null, null, List.of(9L), 1, List.of(), true, List.of("enc-a"), PageRequest.of(0, 20));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }
}
