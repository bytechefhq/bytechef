/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.atlas.execution.repository.jdbc;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.commons.util.CollectionUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
public class CustomJobRepositoryImpl implements CustomJobRepository {

    private final JdbcTemplate jdbcTemplate;

    @SuppressFBWarnings("EI")
    public CustomJobRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long count(String status, LocalDateTime startDate, LocalDateTime endDate, List<String> workflowIds) {
        Query query = buildQuery(status, startDate, endDate, workflowIds, null, true);

        return Validate.notNull(jdbcTemplate.queryForObject(query.query, Long.class, query.arguments), "count");
    }

    @Override
    public List<Job> findAll(
        String status, LocalDateTime startDate, LocalDateTime endDate, List<String> workflowIds) {

        Query query = buildQuery(status, startDate, endDate, workflowIds, null, false);

        return jdbcTemplate.query(query.query, (rs, rowNum) -> new Job(rs), query.arguments);
    }

    @Override
    public Page<Job> findAll(
        String status, LocalDateTime startDate, LocalDateTime endDate, List<String> workflowIds,
        Pageable pageable) {

        Page<Job> page;
        Query query = buildQuery(status, startDate, endDate, workflowIds, pageable, true);

        Long total = jdbcTemplate.queryForObject(query.query, Long.class, query.arguments);

        if (total == null || total == 0) {
            page = Page.empty();
        } else {
            query = buildQuery(status, startDate, endDate, workflowIds, pageable, false);

            List<Job> jobs = jdbcTemplate.query(query.query, (rs, rowNum) -> new Job(rs), query.arguments);

            page = new PageImpl<>(jobs, pageable, total);
        }

        return page;
    }

    private Query buildQuery(
        String status, LocalDateTime startDate, LocalDateTime endDate, List<String> workflowIds, Pageable pageable,
        boolean countQuery) {

        String query;

        if (countQuery) {
            query = "SELECT COUNT(job.id) FROM job ";
        } else {
            query = "SELECT job.* FROM job ";
        }

        List<Object> arguments = new ArrayList<>();

        if (StringUtils.isNotBlank(status) || startDate != null || endDate != null ||
            !CollectionUtils.isEmpty(workflowIds)) {

            query += "WHERE ";
        }

        if (StringUtils.isNotBlank(status)) {
            query += "status = ? ";

            Job.Status jobStatus = Job.Status.valueOf(status);

            arguments.add(jobStatus.getId());
        }

        if (startDate != null && StringUtils.isNotBlank(status)) {
            query += "AND ";
        }

        if (startDate != null) {
            query += "start_date >= ? ";

            arguments.add(startDate);
        }

        if (endDate != null && (StringUtils.isNotBlank(status) || startDate != null)) {
            query += "AND ";
        }

        if (endDate != null) {
            query += "end_date <= ? ";

            arguments.add(endDate);
        }

        if (!CollectionUtils.isEmpty(workflowIds) &&
            (StringUtils.isNotBlank(status) || startDate != null || endDate != null)) {

            query += "AND ";
        }

        if (!CollectionUtils.isEmpty(workflowIds)) {
            query += "workflow_id IN(%s) ".formatted(String.join(",", Collections.nCopies(workflowIds.size(), "?")));

            arguments.addAll(workflowIds);
        }

        if (!countQuery) {
            query += "ORDER BY id DESC ";
        }

        if (!countQuery && pageable != null) {
            query += "LIMIT %s OFFSET %s".formatted(pageable.getPageSize(), pageable.getOffset());
        }

        return new Query(query, arguments.toArray());
    }

    record Query(String query, Object[] arguments) {
    }
}
