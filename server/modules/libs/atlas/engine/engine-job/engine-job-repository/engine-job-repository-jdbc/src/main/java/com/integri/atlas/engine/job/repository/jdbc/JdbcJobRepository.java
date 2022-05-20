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
import com.integri.atlas.engine.DSL;
import com.integri.atlas.engine.data.Page;
import com.integri.atlas.engine.data.ResultPage;
import com.integri.atlas.engine.job.Job;
import com.integri.atlas.engine.job.JobSummary;
import com.integri.atlas.engine.job.SimpleJob;
import com.integri.atlas.engine.job.repository.JobRepository;
import com.integri.atlas.engine.json.Json;
import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.task.execution.repository.TaskExecutionRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.util.Assert;

/**
 * @author Arik Cohe
 * @author Ivica Cardic
 */
public class JdbcJobRepository implements JobRepository {

    protected NamedParameterJdbcOperations jdbcOperations;
    protected ObjectMapper objectMapper;
    // TODO remove to JobService
    protected TaskExecutionRepository taskExecutionRepository;

    public static final int DEFAULT_PAGE_SIZE = 20;

    @Override
    public int countRunningJobs() {
        return jdbcOperations.queryForObject("select count(*) from job where status='STARTED'", Map.of(), Integer.class);
    }

    @Override
    public int countCompletedJobsToday() {
        return jdbcOperations.queryForObject(
            "select count(*) from job where status='COMPLETED' and end_time >= current_date",
            Map.of(),
            Integer.class
        );
    }

    @Override
    public int countCompletedJobsYesterday() {
        return jdbcOperations.queryForObject(
            "select count(*) from job where status='COMPLETED' and end_time >= current_date-1 and end_time < current_date",
            Map.of(),
            Integer.class
        );
    }

    @Override
    public void create(Job aJob) {
        MapSqlParameterSource sqlParameterSource = createSqlParameterSource(aJob);

        jdbcOperations.update(
            "insert into job (id,create_time,start_time,status,current_task,workflow_id,label,priority,inputs,webhooks,outputs,parent_task_execution_id) values (:id,:createTime,:startTime,:status,:currentTask,:workflowId,:label,:priority,:inputs,:webhooks,:outputs,:parentTaskExecutionId)",
            sqlParameterSource
        );
    }

    @Override
    public void delete(String id) {
        jdbcOperations.update("delete from job where id = :id", Map.of("id", id));
    }

    @Override
    public List<Job> findAll() {
        return jdbcOperations.query("select * from job", this::jobRowMappper);
    }

    @Override
    public Job getById(String aId) {
        List<Job> query = jdbcOperations.query(
            "select * from job where id = :id",
            Collections.singletonMap("id", aId),
            this::jobRowMappper
        );
        Assert.isTrue(query.size() == 1, "expected 1 result. got " + query.size());
        return query.get(0);
    }

    @Override
    public Optional<Job> getLatest() {
        List<Job> query = jdbcOperations.query("select * from job order by create_time desc limit 1", this::jobRowMappper);
        if (query.size() == 0) {
            return Optional.empty();
        }
        return Optional.of(query.get(0));
    }

    @Override
    public Job getByTaskId(String aTaskId) {
        Map<String, String> params = Collections.singletonMap("id", aTaskId);
        List<Job> list = jdbcOperations.query(
            "select * from job j where j.id = (select job_id from task_execution jt where jt.id=:id)",
            params,
            this::jobRowMappper
        );
        Assert.isTrue(list.size() < 2, "expecting 1 result, got: " + list.size());
        return list.size() == 1 ? list.get(0) : null;
    }

    @Override
    public Page<JobSummary> getPage(int aPageNumber) {
        Integer totalItems = jdbcOperations.getJdbcOperations().queryForObject("select count(*) from job", Integer.class);
        int offset = (aPageNumber - 1) * DEFAULT_PAGE_SIZE;
        int limit = DEFAULT_PAGE_SIZE;
        List<JobSummary> items = jdbcOperations.query(
            String.format("select * from job order by create_time desc limit %s offset %s", limit, offset),
            this::jobSummaryRowMappper
        );
        ResultPage<JobSummary> resultPage = new ResultPage<>(JobSummary.class);
        resultPage.setItems(items);
        resultPage.setNumber(items.size() > 0 ? aPageNumber : 0);
        resultPage.setTotalItems(totalItems);
        resultPage.setTotalPages(items.size() > 0 ? totalItems / DEFAULT_PAGE_SIZE + 1 : 0);
        return resultPage;
    }

    @Override
    public Job merge(Job aJob) {
        MapSqlParameterSource sqlParameterSource = createSqlParameterSource(aJob);

        jdbcOperations.update(
            "update job set status=:status,start_time=:startTime,end_time=:endTime,current_task=:currentTask,workflow_id=:workflowId,label=:label,outputs=:outputs where id = :id ",
            sqlParameterSource
        );

        return aJob;
    }

    public void setJobTaskExecutionRepository(TaskExecutionRepository taskExecutionRepository) {
        this.taskExecutionRepository = taskExecutionRepository;
    }

    public void setJdbcOperations(NamedParameterJdbcOperations jdbcOperations) {
        this.jdbcOperations = jdbcOperations;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    protected MapSqlParameterSource createSqlParameterSource(Job aJob) {
        SimpleJob job = new SimpleJob(aJob);
        Assert.notNull(aJob, "job must not be null");
        Assert.notNull(aJob.getId(), "job status must not be null");
        Assert.notNull(aJob.getCreateTime(), "job createTime must not be null");
        Assert.notNull(aJob.getStatus(), "job status must not be null");
        MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource();
        sqlParameterSource.addValue("id", job.getId());
        sqlParameterSource.addValue("status", job.getStatus().toString());
        sqlParameterSource.addValue("currentTask", job.getCurrentTask());
        sqlParameterSource.addValue("workflowId", job.getWorkflowId());
        sqlParameterSource.addValue("label", job.getLabel());
        sqlParameterSource.addValue("createTime", job.getCreateTime());
        sqlParameterSource.addValue("startTime", job.getStartTime());
        sqlParameterSource.addValue("endTime", job.getEndTime());
        sqlParameterSource.addValue("priority", job.getPriority());
        sqlParameterSource.addValue("inputs", Json.serialize(objectMapper, job.getInputs()));
        sqlParameterSource.addValue("outputs", Json.serialize(objectMapper, job.getOutputs()));
        sqlParameterSource.addValue("webhooks", Json.serialize(objectMapper, job.getWebhooks()));
        sqlParameterSource.addValue("parentTaskExecutionId", job.getParentTaskExecutionId());
        return sqlParameterSource;
    }

    protected Job jobRowMappper(ResultSet aRs, int aIndex) throws SQLException {
        Map<String, Object> map = new HashMap<>();
        map.put("id", aRs.getString("id"));
        map.put("status", aRs.getString("status"));
        map.put("currentTask", aRs.getInt("current_task"));
        map.put("workflowId", aRs.getString("workflow_id"));
        map.put("label", aRs.getString("label"));
        map.put("createTime", aRs.getTimestamp("create_time"));
        map.put("startTime", aRs.getTimestamp("start_time"));
        map.put("endTime", aRs.getTimestamp("end_time"));
        map.put("execution", getExecution(aRs.getString("id")));
        map.put("priority", aRs.getInt("priority"));
        map.put("inputs", Json.deserialize(objectMapper, aRs.getString("inputs"), Map.class));
        map.put("outputs", Json.deserialize(objectMapper, aRs.getString("outputs"), Map.class));
        map.put("webhooks", Json.deserialize(objectMapper, aRs.getString("webhooks"), List.class));
        map.put(DSL.PARENT_TASK_EXECUTION_ID, aRs.getString("parent_task_execution_id"));
        return new SimpleJob(map);
    }

    protected JobSummary jobSummaryRowMappper(ResultSet aRs, int aIndex) throws SQLException {
        Map<String, Object> map = new HashMap<>();
        map.put("id", aRs.getString("id"));
        map.put("status", aRs.getString("status"));
        map.put("currentTask", aRs.getInt("current_task"));
        map.put("workflowId", aRs.getString("workflow_id"));
        map.put("label", aRs.getString("label"));
        map.put("createTime", aRs.getTimestamp("create_time"));
        map.put("startTime", aRs.getTimestamp("start_time"));
        map.put("endTime", aRs.getTimestamp("end_time"));
        map.put("priority", aRs.getInt("priority"));
        map.put("inputs", Json.deserialize(objectMapper, aRs.getString("inputs"), Map.class));
        map.put("outputs", Json.deserialize(objectMapper, aRs.getString("outputs"), Map.class));
        map.put("webhooks", Json.deserialize(objectMapper, aRs.getString("webhooks"), List.class));
        map.put(DSL.PARENT_TASK_EXECUTION_ID, aRs.getString("parent_task_execution_id"));
        return new JobSummary(new SimpleJob(map));
    }

    private List<TaskExecution> getExecution(String aJobId) {
        return taskExecutionRepository.getExecution(aJobId);
    }
}
