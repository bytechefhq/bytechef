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

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.dto.WorkflowExecutionRowDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * One parameterized query: the job branch mirrors the filters of the job-only listing, the trigger branch is unioned in
 * only when there are trigger workflow execution ids to match, and the page is cut from the union. A row that has not
 * started yet, or that failed before it could start, is placed by its creation date so it stays with its peers instead
 * of sinking to the last page.
 *
 * @author Ivica Cardic
 */
@Repository
public class WorkflowExecutionRowRepositoryImpl implements WorkflowExecutionRowRepository {

    private final JdbcTemplate jdbcTemplate;

    @SuppressFBWarnings("EI")
    public WorkflowExecutionRowRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @SuppressFBWarnings(
        value = "SQL_INJECTION_SPRING_JDBC",
        justification = "Query is safely built using parameterized placeholders; all user input passed via arguments array")
    public Page<WorkflowExecutionRowDTO> findAll(
        Integer status, Instant startDate, Instant endDate, List<Long> principalIds, int type,
        List<String> workflowIds, boolean onlyRootJobs, List<String> failedTriggerWorkflowExecutionIds,
        Pageable pageable) {

        Query rowsQuery = buildRowsQuery(
            status, startDate, endDate, principalIds, type, workflowIds, onlyRootJobs,
            failedTriggerWorkflowExecutionIds);

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM (" + rowsQuery.query + ") execution_rows", Long.class, rowsQuery.arguments);

        if (total == null || total == 0) {
            return Page.empty(pageable);
        }

        List<Object> arguments = new ArrayList<>(List.of(rowsQuery.arguments));

        arguments.add(pageable.getPageSize());
        arguments.add(pageable.getOffset());

        List<WorkflowExecutionRowDTO> rows = jdbcTemplate.query(
            "SELECT kind, id FROM (" + rowsQuery.query + ") execution_rows " +
                "ORDER BY start_date DESC NULLS LAST, id DESC LIMIT ? OFFSET ?",
            (rs, rowNum) -> new WorkflowExecutionRowDTO(WorkflowExecutionRowDTO.Kind.valueOf(rs.getString("kind")),
                rs.getLong("id")),
            arguments.toArray());

        return new PageImpl<>(rows, pageable, total);
    }

    private Query buildRowsQuery(
        Integer status, Instant startDate, Instant endDate, List<Long> principalIds, int type,
        List<String> workflowIds, boolean onlyRootJobs, List<String> failedTriggerWorkflowExecutionIds) {

        List<Object> arguments = new ArrayList<>();

        String query =
            "SELECT '" + WorkflowExecutionRowDTO.Kind.JOB + "' AS kind, job.id AS id, " +
                "COALESCE(job.start_date, job.created_date) AS start_date " +
                "FROM principal_job JOIN job ON principal_job.job_id = job.id WHERE principal_job.type = ? ";

        arguments.add(type);

        if (onlyRootJobs) {
            query += "AND job.parent_task_execution_id IS NULL ";
        }

        if (status != null) {
            query += "AND job.status = ? ";

            arguments.add(status);
        }

        if (startDate != null) {
            query += "AND CAST(job.start_date AS DATE) = CAST(? AS DATE) ";

            arguments.add(Timestamp.from(startDate));
        }

        if (endDate != null) {
            query += "AND CAST(job.end_date AS DATE) = CAST(? AS DATE) ";

            arguments.add(Timestamp.from(endDate));
        }

        if (principalIds != null && !principalIds.isEmpty()) {
            query += "AND principal_job.principal_id IN(%s) ".formatted(placeholders(principalIds.size()));

            arguments.addAll(principalIds);
        }

        if (!CollectionUtils.isEmpty(workflowIds)) {
            query += "AND job.workflow_id IN(%s) ".formatted(placeholders(workflowIds.size()));

            arguments.addAll(workflowIds);
        }

        if (!failedTriggerWorkflowExecutionIds.isEmpty()) {
            query += "UNION ALL SELECT '" + WorkflowExecutionRowDTO.Kind.TRIGGER_EXECUTION + "' AS kind, " +
                "trigger_execution.id AS id, " +
                "COALESCE(trigger_execution.start_date, trigger_execution.created_date) AS start_date " +
                "FROM trigger_execution " +
                "LEFT JOIN trigger_execution_job ON trigger_execution_job.trigger_execution_id = trigger_execution.id "
                +
                "WHERE trigger_execution_job.job_id IS NULL AND trigger_execution.status = ? " +
                "AND trigger_execution.workflow_execution_id IN(%s) ".formatted(
                    placeholders(failedTriggerWorkflowExecutionIds.size()));

            arguments.add(TriggerExecution.Status.FAILED.ordinal());
            arguments.addAll(failedTriggerWorkflowExecutionIds);

            if (startDate != null) {
                query += "AND CAST(trigger_execution.start_date AS DATE) = CAST(? AS DATE) ";

                arguments.add(Timestamp.from(startDate));
            }

            if (endDate != null) {
                query += "AND CAST(trigger_execution.end_date AS DATE) = CAST(? AS DATE) ";

                arguments.add(Timestamp.from(endDate));
            }
        }

        return new Query(query, arguments.toArray());
    }

    private static String placeholders(int count) {
        return String.join(",", Collections.nCopies(count, "?"));
    }

    private record Query(String query, Object[] arguments) {
    }
}
