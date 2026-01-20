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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Custom repository implementation for principal job queries with pagination. Uses parameterized queries to prevent SQL
 * injection.
 *
 * @author Ivica Cardic
 */
public class CustomPrincipalJobRepositoryImpl implements CustomPrincipalJobRepository {

    private final JdbcTemplate jdbcTemplate;

    @SuppressFBWarnings("EI")
    public CustomPrincipalJobRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @SuppressFBWarnings(
        value = "SQL_INJECTION_SPRING_JDBC",
        justification = "Query is safely built using parameterized placeholders; all user input passed via arguments array")
    public Page<Long> findAllJobIds(
        Integer status, Instant startDate, Instant endDate, List<Long> instanceIds, int type,
        @NonNull List<String> workflowIds, Pageable pageable) {

        Page<Long> page;
        Query query = buildQuery(status, startDate, endDate, instanceIds, type, workflowIds, pageable, true);

        Long total = jdbcTemplate.queryForObject(query.query, Long.class, query.arguments);

        if (total == null || total == 0) {
            page = Page.empty();
        } else {
            query = buildQuery(status, startDate, endDate, instanceIds, type, workflowIds, pageable, false);

            List<Long> jobs = jdbcTemplate.query(query.query, (rs, rowNum) -> rs.getLong("job_id"), query.arguments);

            page = new PageImpl<>(jobs, pageable, total);
        }

        return page;
    }

    private Query buildQuery(
        Integer status, Instant startDate, Instant endDate, List<Long> instanceIds, int type,
        List<String> workflowIds, Pageable pageable, boolean countQuery) {

        String query;

        if (countQuery) {
            query = "SELECT COUNT(principal_job.id) FROM principal_job ";
        } else {
            query = "SELECT principal_job.job_id FROM principal_job ";
        }

        query += "JOIN job ON principal_job.job_id = job.id WHERE type = ? ";

        List<Object> arguments = new ArrayList<>();

        arguments.add(type);

        if (status != null) {
            query += "AND status = ? ";

            arguments.add(status);
        }

        if (startDate != null) {
            query += "AND start_date >= ? ";

            arguments.add(Timestamp.from(startDate));
        }

        if (endDate != null) {
            query += "AND end_date <= ? ";

            arguments.add(Timestamp.from(endDate));
        }

        if (instanceIds != null && !instanceIds.isEmpty()) {
            query += "AND principal_id IN(%s) ".formatted(
                String.join(",", Collections.nCopies(instanceIds.size(), "?")));

            arguments.addAll(instanceIds);
        }

        if (!CollectionUtils.isEmpty(workflowIds)) {
            query +=
                "AND workflow_id IN(%s) ".formatted(String.join(",", Collections.nCopies(workflowIds.size(), "?")));

            arguments.addAll(workflowIds);
        }

        if (!countQuery) {
            query += "ORDER BY job_id DESC ";
        }

        if (!countQuery && pageable != null) {
            query += "LIMIT ? OFFSET ?";

            arguments.add(pageable.getPageSize());
            arguments.add(pageable.getOffset());
        }

        return new Query(query, arguments.toArray());
    }

    record Query(String query, Object[] arguments) {
    }
}
