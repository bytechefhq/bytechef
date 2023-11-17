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
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.LocalDateTimeUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
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
public class CustomJobRepositoryImpl implements CustomJobRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI")
    public CustomJobRepositoryImpl(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Page<Job> findAll(
        String status, LocalDateTime startDate, LocalDateTime endDate, Long instanceId, int type,
        @NonNull List<String> workflowIds, Pageable pageable) {

        Page<Job> page;
        Query query = buildQuery(status, startDate, endDate, instanceId, type, workflowIds, pageable, true);

        Long total = jdbcTemplate.queryForObject(query.query, Long.class, query.arguments);

        if (total == null || total == 0) {
            page = Page.empty();
        } else {
            query = buildQuery(status, startDate, endDate, instanceId, type, workflowIds, pageable, false);

            List<Job> jobs = jdbcTemplate.query(query.query, (rs, rowNum) -> toJob(rs), query.arguments);

            page = new PageImpl<>(jobs, pageable, total);
        }

        return page;
    }

    private Query buildQuery(
        String status, LocalDateTime startDate, LocalDateTime endDate, Long instanceId, int type,
        List<String> workflowIds, Pageable pageable, boolean countQuery) {

        String query;

        if (countQuery) {
            query = "SELECT COUNT(job.id) FROM job ";
        } else {
            query = "SELECT job.* FROM job ";
        }

        if (instanceId != null) {
            query += "JOIN instance_job ON instance_job.job_id = job.id ";
        }

        List<Object> arguments = new ArrayList<>();

        if (StringUtils.isNotBlank(status) || startDate != null || endDate != null || instanceId != null ||
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

        if (instanceId != null && (StringUtils.isNotBlank(status) || startDate != null || endDate != null)) {
            query += "AND ";
        }

        if (instanceId != null) {
            query += "instance_id = ? AND type = ? ";

            arguments.add(instanceId);
            arguments.add(type);
        }

        if (!CollectionUtils.isEmpty(workflowIds) &&
            (StringUtils.isNotBlank(status) || startDate != null || endDate != null || instanceId != null)) {

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

    private Job toJob(ResultSet rs) throws SQLException {
        Job job = new Job();

        job.setCurrentTask(rs.getInt("current_task"));

        Timestamp endDateTimestamp = rs.getTimestamp("end_date");

        if (endDateTimestamp != null) {
            job.setEndDate(LocalDateTimeUtils.getLocalDateTime(endDateTimestamp));
        }

        job.setLabel(rs.getString("label"));
        job.setId(rs.getLong("id"));
        job.setMetadata(JsonUtils.readMap(rs.getString("metadata"), objectMapper));
        job.setPriority(rs.getInt("priority"));

        Timestamp startDateTimestamp = rs.getTimestamp("start_date");

        if (startDateTimestamp != null) {
            job.setStartDate(LocalDateTimeUtils.getLocalDateTime(startDateTimestamp));
        }

        job.setStatus(Job.Status.valueOf(rs.getInt("status")));
        job.setWorkflowId(rs.getString("workflow_id"));

        return job;
    }

    record Query(String query, Object[] arguments) {
    }
}
