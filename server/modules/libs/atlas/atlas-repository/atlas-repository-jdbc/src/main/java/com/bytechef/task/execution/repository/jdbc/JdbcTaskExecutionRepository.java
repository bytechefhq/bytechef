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

package com.bytechef.task.execution.repository.jdbc;

import com.bytechef.atlas.task.execution.TaskStatus;
import com.bytechef.atlas.task.execution.domain.SimpleTaskExecution;
import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.atlas.util.JSONUtils;
import com.bytechef.task.execution.repository.TaskExecutionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Arik Cohe
 * @author Ivica Cardic
 */
public class JdbcTaskExecutionRepository implements TaskExecutionRepository {

    private NamedParameterJdbcTemplate jdbcTemplate;
    private ObjectMapper objectMapper;

    @Override
    public void create(TaskExecution taskExecution) {
        SqlParameterSource sqlParameterSource = createSqlParameterSource(taskExecution);

        jdbcTemplate.update(
                "insert into task_execution "
                        + "  (id,parent_id,job_id,serialized_execution,status,progress,create_time,priority,task_number) "
                        + "values "
                        + "  (:id,:parentId,:jobId,:serializedExecution,:status,:progress,:createTime,:priority,:taskNumber)",
                sqlParameterSource);
    }

    @Override
    public TaskExecution findOne(String id) {
        List<TaskExecution> taskExecutions = jdbcTemplate.query(
                "select * from task_execution where id = :id",
                Collections.singletonMap("id", id),
                this::jobTaskRowMapper);
        if (taskExecutions.size() == 1) {
            return taskExecutions.get(0);
        }
        return null;
    }

    @Override
    public List<TaskExecution> findAllByJobIdOrderByTaskNumber(String jobId) {
        return jdbcTemplate.query(
                "select * from task_execution where job_id = :jobId order by task_number",
                Collections.singletonMap("jobId", jobId),
                this::jobTaskRowMapper);
    }

    @Override
    public List<TaskExecution> findAllByParentId(String parentId) {
        return jdbcTemplate.query(
                "select * from task_execution where parent_id = :parentId order by task_number",
                Collections.singletonMap("parentId", parentId),
                this::jobTaskRowMapper);
    }

    @Override
    public List<TaskExecution> findAllByJobIdOrderByCreateTime(String jobId) {
        return jdbcTemplate.query(
                "select * from task_execution where job_id = :jobId order by create_time asc",
                Collections.singletonMap("jobId", jobId),
                this::jobTaskRowMapper);
    }

    @Override
    public List<TaskExecution> findAllByJobIdsOrderByCreateTime(List<String> jobIds) {
        return jdbcTemplate.query(
                "select * from task_execution where job_id in(:jobId) order by create_time asc",
                Collections.singletonMap("jobIds", jobIds),
                this::jobTaskRowMapper);
    }

    @Override
    @Transactional
    public TaskExecution merge(TaskExecution taskExecution) {
        TaskExecution currentTaskExecution = jdbcTemplate.queryForObject(
                "select * from task_execution where id = :id for update",
                Collections.singletonMap("id", taskExecution.getId()),
                this::jobTaskRowMapper);
        SimpleTaskExecution merged = SimpleTaskExecution.of(taskExecution);

        if (currentTaskExecution.getStatus().isTerminated() && taskExecution.getStatus() == TaskStatus.STARTED) {
            merged = SimpleTaskExecution.of(currentTaskExecution);

            merged.setStartTime(taskExecution.getStartTime());
        } else if (taskExecution.getStatus().isTerminated() && currentTaskExecution.getStatus() == TaskStatus.STARTED) {
            merged.setStartTime(currentTaskExecution.getStartTime());
        }

        SqlParameterSource sqlParameterSource = createSqlParameterSource(merged);

        jdbcTemplate.update(
                "update task_execution set "
                        + "  serialized_execution=:serializedExecution,status=:status,progress=:progress,start_time=:startTime,end_time=:endTime where id = :id ",
                sqlParameterSource);

        return merged;
    }

    public void setJdbcTemplate(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private SqlParameterSource createSqlParameterSource(TaskExecution aTaskExecution) {
        MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource();

        sqlParameterSource.addValue("id", aTaskExecution.getId());
        sqlParameterSource.addValue("parentId", aTaskExecution.getParentId());
        sqlParameterSource.addValue("jobId", aTaskExecution.getJobId());
        sqlParameterSource.addValue("status", aTaskExecution.getStatus().toString());
        sqlParameterSource.addValue("progress", aTaskExecution.getProgress());
        sqlParameterSource.addValue("createTime", aTaskExecution.getCreateTime());
        sqlParameterSource.addValue("startTime", aTaskExecution.getStartTime());
        sqlParameterSource.addValue("endTime", aTaskExecution.getEndTime());
        sqlParameterSource.addValue("serializedExecution", JSONUtils.serialize(objectMapper, aTaskExecution));
        sqlParameterSource.addValue("priority", aTaskExecution.getPriority());
        sqlParameterSource.addValue("taskNumber", aTaskExecution.getTaskNumber());

        return sqlParameterSource;
    }

    @SuppressWarnings("unchecked")
    private TaskExecution jobTaskRowMapper(ResultSet aRs, int aIndex) throws SQLException {
        return SimpleTaskExecution.of(
                JSONUtils.deserialize(objectMapper, aRs.getString("serialized_execution"), Map.class));
    }
}
