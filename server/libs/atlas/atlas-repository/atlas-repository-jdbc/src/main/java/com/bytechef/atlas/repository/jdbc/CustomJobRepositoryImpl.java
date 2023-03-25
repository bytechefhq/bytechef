
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.atlas.repository.jdbc;

import com.bytechef.atlas.domain.Job;
import com.bytechef.commons.util.LocalDateTimeUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    public Page<Job> findAll(
        String status, LocalDateTime startDate, LocalDateTime endDate, String workflowId, List<String> workflowIds,
        Pageable pageable) {

        Page<Job> page;
        Query query = buildQuery(status, startDate, endDate, workflowId, workflowIds, pageable, true);

        Long total = jdbcTemplate.queryForObject(query.query, Long.class, query.arguments);

        if (total == null || total == 0) {
            page = Page.empty();
        } else {
            query = buildQuery(status, startDate, endDate, workflowId, workflowIds, pageable, false);

            List<Job> jobs = jdbcTemplate.query(query.query, (rs, rowNum) -> {
                Job job = new Job();

                job.setCurrentTask(rs.getInt("current_task"));

                Timestamp endDateTimestamp = rs.getTimestamp("end_date");

                if (endDateTimestamp != null) {
                    job.setEndDate(LocalDateTimeUtils.getLocalDateTime(endDateTimestamp));
                }

                job.setLabel(rs.getString("label"));
                job.setId(rs.getLong("id"));
                job.setPriority(rs.getInt("priority"));

                Timestamp startDateTimestamp = rs.getTimestamp("start_date");

                if (startDateTimestamp != null) {
                    job.setEndDate(LocalDateTimeUtils.getLocalDateTime(startDateTimestamp));
                }

                job.setStatus(Job.Status.valueOf(rs.getString("status")));
                job.setWorkflowId(rs.getString("workflow_id"));

                return job;
            }, query.arguments);

            page = new PageImpl<>(jobs, pageable, total);
        }

        return page;
    }

    private Query buildQuery(
        String status, LocalDateTime startDate, LocalDateTime endDate, String workflowId, List<String> workflowIds,
        Pageable pageable, boolean countQuery) {

        String query;

        if (countQuery) {
            query = "SELECT COUNT(job.id) FROM job ";
        } else {
            query = "SELECT job.* FROM job ";
        }

        List<Object> arguments = new ArrayList<>();

        if (StringUtils.hasText(status) || startDate != null || endDate != null || StringUtils.hasText(workflowId) ||
            !CollectionUtils.isEmpty(workflowIds)) {

            query += "WHERE ";
        }

        if (StringUtils.hasText(status)) {
            query += "status = ? ";

            arguments.add(status);
        }

        if (startDate != null && StringUtils.hasText(status)) {
            query += "AND ";
        }

        if (startDate != null) {
            query += "start_date >= ? ";

            arguments.add(startDate);
        }

        if (endDate != null && (StringUtils.hasText(status) || startDate != null)) {
            query += "AND ";
        }

        if (endDate != null) {
            query += "end_date <= ? ";

            arguments.add(endDate);
        }

        if (StringUtils.hasText(workflowId) && (StringUtils.hasText(status) || startDate != null || endDate != null)) {
            query += "AND ";
        }

        if (StringUtils.hasText(workflowId)) {
            query += "workflow_id = ? ";

            arguments.add(workflowId);
        }

        if (!CollectionUtils.isEmpty(workflowIds) &&
            (StringUtils.hasText(status) || startDate != null || endDate != null || StringUtils.hasText(workflowId))) {

            query += "AND ";
        }

        if (!CollectionUtils.isEmpty(workflowIds)) {
            query += "workflow_id IN(%s) ".formatted(String.join(",", Collections.nCopies(workflowIds.size(), "?")));

            arguments.addAll(workflowIds);
        }

        if (!countQuery) {
            query += "ORDER BY id DESC LIMIT %s OFFSET %s".formatted(pageable.getPageSize(), pageable.getOffset());
        }

        return new Query(query, arguments.toArray());
    }

    record Query(String query, Object[] arguments) {
    }
}
