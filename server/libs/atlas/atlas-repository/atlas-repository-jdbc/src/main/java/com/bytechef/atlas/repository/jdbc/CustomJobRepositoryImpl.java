
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
import java.util.Date;
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
        String status, LocalDateTime startTime, LocalDateTime endTime, String workflowId, List<String> workflowIds,
        Pageable pageable) {

        Page<Job> page;
        Query query = buildQuery(status, startTime, endTime, workflowId, workflowIds, pageable, true);

        Long total = jdbcTemplate.queryForObject(query.query, Long.class, query.arguments);

        if (total == null || total == 0) {
            page = Page.empty();
        } else {
            query = buildQuery(status, startTime, endTime, workflowId, workflowIds, pageable, false);

            List<Job> jobs = jdbcTemplate.query(query.query, (rs, rowNum) -> {
                Job job = new Job();

                job.setCurrentTask(rs.getInt("current_task"));

                Timestamp endTimeTimestamp = rs.getTimestamp("end_time");

                if (endTimeTimestamp != null) {
                    job.setEndTime(new Date(endTimeTimestamp.getTime()));
                }

                job.setLabel(rs.getString("label"));
                job.setId(rs.getLong("id"));
                job.setPriority(rs.getInt("priority"));

                Timestamp startTimeTimestamp = rs.getTimestamp("start_time");

                job.setStartTime(new Date(startTimeTimestamp.getTime()));
                job.setStatus(Job.Status.valueOf(rs.getString("status")));
                job.setWorkflowId(rs.getString("workflow_id"));

                return job;
            }, query.arguments);

            page = new PageImpl<>(jobs, pageable, total);
        }

        return page;
    }

    private Query buildQuery(
        String status, LocalDateTime startTime, LocalDateTime endTime, String workflowId, List<String> workflowIds,
        Pageable pageable, boolean countQuery) {

        String query;

        if (countQuery) {
            query = "SELECT COUNT(job.id) FROM job ";
        } else {
            query = "SELECT job.* FROM job ";
        }

        List<Object> arguments = new ArrayList<>();

        if (StringUtils.hasText(status) || startTime != null || endTime != null || StringUtils.hasText(workflowId) ||
            !CollectionUtils.isEmpty(workflowIds)) {

            query += "WHERE ";
        }

        if (StringUtils.hasText(status)) {
            query += "status = ? ";

            arguments.add(status);
        }

        if (startTime != null && StringUtils.hasText(status)) {
            query += "AND ";
        }

        if (startTime != null) {
            query += "start_time >= ? ";

            arguments.add(startTime);
        }

        if (endTime != null && (StringUtils.hasText(status) || startTime != null)) {
            query += "AND ";
        }

        if (endTime != null) {
            query += "end_time <= ? ";

            arguments.add(endTime);
        }

        if (StringUtils.hasText(workflowId) && (StringUtils.hasText(status) || startTime != null || endTime != null)) {
            query += "AND ";
        }

        if (StringUtils.hasText(workflowId)) {
            query += "workflow_id = ? ";

            arguments.add(workflowId);
        }

        if (!CollectionUtils.isEmpty(workflowIds) &&
            (StringUtils.hasText(status) || startTime != null || endTime != null || StringUtils.hasText(workflowId))) {

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
