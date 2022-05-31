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

package com.integri.atlas.engine.job.repository.jdbc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integri.atlas.engine.Constants;
import com.integri.atlas.engine.data.Page;
import com.integri.atlas.engine.data.ResultPage;
import com.integri.atlas.engine.job.Job;
import com.integri.atlas.engine.job.JobSummary;
import com.integri.atlas.engine.job.SimpleJob;
import com.integri.atlas.engine.job.repository.JobRepository;
import com.integri.atlas.engine.json.Json;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.Assert;

/**
 * @author Arik Cohe
 * @author Ivica Cardic
 */
public class JdbcJobRepository implements JobRepository {

    protected NamedParameterJdbcTemplate jdbcTemplate;
    protected ObjectMapper objectMapper;

    public static final int DEFAULT_PAGE_SIZE = 20;

    @Override
    public int countRunningJobs() {
        return jdbcTemplate.queryForObject("select count(*) from job where status='STARTED'", Map.of(), Integer.class);
    }

    @Override
    public int countCompletedJobsToday() {
        return jdbcTemplate.queryForObject(
            "select count(*) from job where status='COMPLETED' and end_time >= current_date",
            Map.of(),
            Integer.class
        );
    }

    @Override
    public int countCompletedJobsYesterday() {
        return jdbcTemplate.queryForObject(
            "select count(*) from job where status='COMPLETED' and end_time >= current_date-1 and end_time < current_date",
            Map.of(),
            Integer.class
        );
    }

    @Override
    public void create(Job aJob) {
        MapSqlParameterSource sqlParameterSource = createSqlParameterSource(aJob);

        jdbcTemplate.update(
            "insert into job (id,create_time,start_time,status,current_task,workflow_id,label,priority,inputs,webhooks,outputs,parent_task_execution_id) values (:id,:createTime,:startTime,:status,:currentTask,:workflowId,:label,:priority,:inputs,:webhooks,:outputs,:parentTaskExecutionId)",
            sqlParameterSource
        );
    }

    @Override
    public void delete(String id) {
        jdbcTemplate.update("delete from job where id = :id", Map.of("id", id));
    }

    @Override
    public List<Job> findAll() {
        return jdbcTemplate.query("select * from job", this::jobRowMapper);
    }

    @Override
    public Job findById(String aId) {
        List<Job> query = jdbcTemplate.query(
            "select * from job where id = :id",
            Collections.singletonMap("id", aId),
            this::jobRowMapper
        );

        Assert.isTrue(query.size() == 1, "expected 1 result. got " + query.size());

        return query.get(0);
    }

    @Override
    public Optional<Job> findLatestJob() {
        List<Job> query = jdbcTemplate.query("select * from job order by create_time desc limit 1", this::jobRowMapper);

        if (query.size() == 0) {
            return Optional.empty();
        }

        return Optional.of(query.get(0));
    }

    @Override
    public Job findByTaskExecutionId(String taskExecutionId) {
        Map<String, String> params = Collections.singletonMap("id", taskExecutionId);

        List<Job> list = jdbcTemplate.query(
            "select * from job j where j.id = (select job_id from task_execution jt where jt.id=:id)",
            params,
            this::jobRowMapper
        );

        Assert.isTrue(list.size() < 2, "expecting 1 result, got: " + list.size());

        return list.size() == 1 ? list.get(0) : null;
    }

    @Override
    public Page<JobSummary> findAllJobSummaries(int pageNumber) {
        Integer totalItems = jdbcTemplate.getJdbcOperations().queryForObject("select count(*) from job", Integer.class);
        int offset = (pageNumber - 1) * DEFAULT_PAGE_SIZE;
        int limit = DEFAULT_PAGE_SIZE;
        List<JobSummary> items = jdbcTemplate.query(
            String.format("select * from job order by create_time desc limit %s offset %s", limit, offset),
            this::jobSummaryRowMapper
        );
        ResultPage<JobSummary> resultPage = new ResultPage<>(JobSummary.class);

        resultPage.setItems(items);
        resultPage.setNumber(items.size() > 0 ? pageNumber : 0);
        resultPage.setTotalItems(totalItems);
        resultPage.setTotalPages(items.size() > 0 ? totalItems / DEFAULT_PAGE_SIZE + 1 : 0);

        return resultPage;
    }

    @Override
    public Job merge(Job job) {
        MapSqlParameterSource sqlParameterSource = createSqlParameterSource(job);

        jdbcTemplate.update(
            "update job set status=:status,start_time=:startTime,end_time=:endTime,current_task=:currentTask,workflow_id=:workflowId,label=:label,outputs=:outputs where id = :id ",
            sqlParameterSource
        );

        return job;
    }

    public void setJdbcTemplate(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    protected MapSqlParameterSource createSqlParameterSource(Job job) {
        SimpleJob simpleJob = new SimpleJob(job);

        Assert.notNull(job, "job must not be null");
        Assert.notNull(job.getId(), "job id must not be null");
        Assert.notNull(job.getCreateTime(), "job createTime must not be null");
        Assert.notNull(job.getStatus(), "job status must not be null");

        MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource();

        sqlParameterSource.addValue("id", simpleJob.getId());
        sqlParameterSource.addValue("status", simpleJob.getStatus().toString());
        sqlParameterSource.addValue("currentTask", simpleJob.getCurrentTask());
        sqlParameterSource.addValue("workflowId", simpleJob.getWorkflowId());
        sqlParameterSource.addValue("label", simpleJob.getLabel());
        sqlParameterSource.addValue("createTime", simpleJob.getCreateTime());
        sqlParameterSource.addValue("startTime", simpleJob.getStartTime());
        sqlParameterSource.addValue("endTime", simpleJob.getEndTime());
        sqlParameterSource.addValue("priority", simpleJob.getPriority());
        sqlParameterSource.addValue("inputs", Json.serialize(objectMapper, simpleJob.getInputs()));
        sqlParameterSource.addValue("outputs", Json.serialize(objectMapper, simpleJob.getOutputs()));
        sqlParameterSource.addValue("webhooks", Json.serialize(objectMapper, simpleJob.getWebhooks()));
        sqlParameterSource.addValue("parentTaskExecutionId", simpleJob.getParentTaskExecutionId());

        return sqlParameterSource;
    }

    protected Job jobRowMapper(ResultSet resultSet, int index) throws SQLException {
        Map<String, Object> map = new HashMap<>();

        map.put("id", resultSet.getString("id"));
        map.put("status", resultSet.getString("status"));
        map.put("currentTask", resultSet.getInt("current_task"));
        map.put("workflowId", resultSet.getString("workflow_id"));
        map.put("label", resultSet.getString("label"));
        map.put("createTime", resultSet.getTimestamp("create_time"));
        map.put("startTime", resultSet.getTimestamp("start_time"));
        map.put("endTime", resultSet.getTimestamp("end_time"));
        map.put("priority", resultSet.getInt("priority"));
        map.put("inputs", Json.deserialize(objectMapper, resultSet.getString("inputs"), Map.class));
        map.put("outputs", Json.deserialize(objectMapper, resultSet.getString("outputs"), Map.class));
        map.put("webhooks", Json.deserialize(objectMapper, resultSet.getString("webhooks"), List.class));
        map.put(Constants.PARENT_TASK_EXECUTION_ID, resultSet.getString("parent_task_execution_id"));

        return new SimpleJob(map);
    }

    protected JobSummary jobSummaryRowMapper(ResultSet resultSet, int index) throws SQLException {
        Map<String, Object> map = new HashMap<>();

        map.put("id", resultSet.getString("id"));
        map.put("status", resultSet.getString("status"));
        map.put("currentTask", resultSet.getInt("current_task"));
        map.put("workflowId", resultSet.getString("workflow_id"));
        map.put("label", resultSet.getString("label"));
        map.put("createTime", resultSet.getTimestamp("create_time"));
        map.put("startTime", resultSet.getTimestamp("start_time"));
        map.put("endTime", resultSet.getTimestamp("end_time"));
        map.put("priority", resultSet.getInt("priority"));
        map.put("inputs", Json.deserialize(objectMapper, resultSet.getString("inputs"), Map.class));
        map.put("outputs", Json.deserialize(objectMapper, resultSet.getString("outputs"), Map.class));
        map.put("webhooks", Json.deserialize(objectMapper, resultSet.getString("webhooks"), List.class));
        map.put(Constants.PARENT_TASK_EXECUTION_ID, resultSet.getString("parent_task_execution_id"));

        return new JobSummary(new SimpleJob(map));
    }
}
