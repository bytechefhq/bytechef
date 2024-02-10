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

package com.bytechef.platform.workflow.execution.repository;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.commons.util.CollectionUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
public class CustomInstanceJobRepositoryImpl implements CustomInstanceJobRepository {

    private final JdbcTemplate jdbcTemplate;

    @SuppressFBWarnings("EI")
    public CustomInstanceJobRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Page<Long> findAllJobIds(
        String status, LocalDateTime startDate, LocalDateTime endDate, Long instanceId, int type,
        @NonNull List<String> workflowIds, Pageable pageable) {

        Page<Long> page;
        Query query = buildQuery(status, startDate, endDate, instanceId, type, workflowIds, pageable, true);

        Long total = jdbcTemplate.queryForObject(query.query, Long.class, query.arguments);

        if (total == null || total == 0) {
            page = Page.empty();
        } else {
            query = buildQuery(status, startDate, endDate, instanceId, type, workflowIds, pageable, false);

            List<Long> jobs = jdbcTemplate.query(query.query, (rs, rowNum) -> rs.getLong("job_id"), query.arguments);

            page = new PageImpl<>(jobs, pageable, total);
        }

        return page;
    }

    private Query buildQuery(
        String status, LocalDateTime startDate, LocalDateTime endDate, Long instanceId, int type,
        List<String> workflowIds, Pageable pageable, boolean countQuery) {

        String query;

        if (countQuery) {
            query = "SELECT COUNT(instance_job.id) FROM instance_job ";
        } else {
            query = "SELECT instance_job.job_id FROM instance_job ";
        }

        query += "JOIN job ON instance_job.job_id = job.id WHERE type = ? ";

        List<Object> arguments = new ArrayList<>();

        arguments.add(type);

        if (StringUtils.isNotBlank(status)) {
            query += "AND status = ? ";

            Job.Status jobStatus = Job.Status.valueOf(status);

            arguments.add(jobStatus.getId());
        }

        if (startDate != null) {
            query += "AND start_date >= ? ";

            arguments.add(startDate);
        }

        if (endDate != null) {
            query += "AND end_date <= ? ";

            arguments.add(endDate);
        }

        if (instanceId != null) {
            query += "AND instance_id = ? ";

            arguments.add(instanceId);
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
            query += "LIMIT %s OFFSET %s".formatted(pageable.getPageSize(), pageable.getOffset());
        }

        return new Query(query, arguments.toArray());
    }

    record Query(String query, Object[] arguments) {
    }
}
