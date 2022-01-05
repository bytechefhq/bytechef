/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.integri.atlas.engine.repository.jdbc.task;

import com.integri.atlas.engine.core.json.JSONHelper;
import com.integri.atlas.engine.core.task.SimpleTaskExecution;
import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.core.task.TaskStatus;
import com.integri.atlas.engine.core.task.repository.TaskExecutionRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Arik Cohe
 * @author Ivica Cardic
 */
public class JdbcTaskExecutionRepository implements TaskExecutionRepository {

    private NamedParameterJdbcOperations jdbc;
    private JSONHelper jsonMapper;

    @Override
    public void create(TaskExecution aTaskExecution) {
        SqlParameterSource sqlParameterSource = createSqlParameterSource(aTaskExecution);

        jdbc.update(
            "insert into task_execution " +
            "  (id,parent_id,job_id,serialized_execution,status,progress,create_time,priority,task_number) " +
            "values " +
            "  (:id,:parentId,:jobId,:serializedExecution,:status,:progress,:createTime,:priority,:taskNumber)",
            sqlParameterSource
        );
    }

    @Override
    public TaskExecution findOne(String aTaskExecutionId) {
        List<TaskExecution> query = jdbc.query(
            "select * from task_execution where id = :id",
            Collections.singletonMap("id", aTaskExecutionId),
            this::jobTaskRowMappper
        );
        if (query.size() == 1) {
            return query.get(0);
        }
        return null;
    }

    @Override
    public List<TaskExecution> findByParentId(String aParentId) {
        return jdbc.query(
            "select * from task_execution where parent_id = :parentId order by task_number",
            Collections.singletonMap("parentId", aParentId),
            this::jobTaskRowMappper
        );
    }

    @Override
    public List<TaskExecution> getExecution(String aJobId) {
        return jdbc.query(
            "select * From task_execution where job_id = :jobId order by create_time asc",
            Collections.singletonMap("jobId", aJobId),
            this::jobTaskRowMappper
        );
    }

    @Override
    @Transactional
    public TaskExecution merge(TaskExecution aTaskExecution) {
        TaskExecution current = jdbc.queryForObject(
            "select * from task_execution where id = :id for update",
            Collections.singletonMap("id", aTaskExecution.getId()),
            this::jobTaskRowMappper
        );
        SimpleTaskExecution merged = SimpleTaskExecution.of(aTaskExecution);
        if (current.getStatus().isTerminated() && aTaskExecution.getStatus() == TaskStatus.STARTED) {
            merged = SimpleTaskExecution.of(current);
            merged.setStartTime(aTaskExecution.getStartTime());
        } else if (aTaskExecution.getStatus().isTerminated() && current.getStatus() == TaskStatus.STARTED) {
            merged.setStartTime(current.getStartTime());
        }
        SqlParameterSource sqlParameterSource = createSqlParameterSource(merged);

        jdbc.update(
            "update task_execution set " +
            "  serialized_execution=:serializedExecution,status=:status,progress=:progress,start_time=:startTime,end_time=:endTime where id = :id ",
            sqlParameterSource
        );

        return merged;
    }

    public void setJdbcOperations(NamedParameterJdbcOperations aJdbcOperations) {
        jdbc = aJdbcOperations;
    }

    public void setJsonMapper(JSONHelper jsonMapper) {
        this.jsonMapper = jsonMapper;
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
        sqlParameterSource.addValue("serializedExecution", jsonMapper.serialize(aTaskExecution));
        sqlParameterSource.addValue("priority", aTaskExecution.getPriority());
        sqlParameterSource.addValue("taskNumber", aTaskExecution.getTaskNumber());
        return sqlParameterSource;
    }

    @SuppressWarnings("unchecked")
    private TaskExecution jobTaskRowMappper(ResultSet aRs, int aIndex) throws SQLException {
        return SimpleTaskExecution.of(jsonMapper.deserialize(aRs.getString("serialized_execution"), Map.class));
    }
}
