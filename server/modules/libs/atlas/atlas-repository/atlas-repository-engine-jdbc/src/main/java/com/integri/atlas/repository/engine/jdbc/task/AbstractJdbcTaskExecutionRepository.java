package com.integri.atlas.repository.engine.jdbc.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integri.atlas.engine.core.json.Json;
import com.integri.atlas.engine.core.task.SimpleTaskExecution;
import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.core.task.TaskStatus;
import com.integri.atlas.engine.core.task.repository.TaskExecutionRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractJdbcTaskExecutionRepository implements TaskExecutionRepository {

    private NamedParameterJdbcOperations jdbc;
    private ObjectMapper json = new ObjectMapper();

    @Override
    public void create(TaskExecution aTaskExecution) {
        SqlParameterSource sqlParameterSource = createSqlParameterSource(aTaskExecution);

        jdbc.update(getCreateSql(), sqlParameterSource);
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
        return jdbc.query("select * From task_execution where job_id = :jobId order by create_time asc", Collections.singletonMap("jobId", aJobId), this::jobTaskRowMappper);
    }

    @Override
    @Transactional
    public TaskExecution merge(TaskExecution aTaskExecution) {
        TaskExecution current = jdbc.queryForObject("select * from task_execution where id = :id for update", Collections.singletonMap("id", aTaskExecution.getId()), this::jobTaskRowMappper);
        SimpleTaskExecution merged = SimpleTaskExecution.of(aTaskExecution);
        if (current.getStatus().isTerminated() && aTaskExecution.getStatus() == TaskStatus.STARTED) {
            merged = SimpleTaskExecution.of(current);
            merged.setStartTime(aTaskExecution.getStartTime());
        } else if (aTaskExecution.getStatus().isTerminated() && current.getStatus() == TaskStatus.STARTED) {
            merged.setStartTime(current.getStartTime());
        }
        SqlParameterSource sqlParameterSource = createSqlParameterSource(merged);

        jdbc.update(getMergeSQL(), sqlParameterSource);

        return merged;
    }

    public void setJdbcOperations(NamedParameterJdbcOperations aJdbcOperations) {
        jdbc = aJdbcOperations;
    }

    public void setObjectMapper(ObjectMapper aJson) {
        json = aJson;
    }

    protected abstract String getCreateSql();

    protected abstract String getMergeSQL();

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
        sqlParameterSource.addValue("serializedExecution", Json.serialize(json, aTaskExecution));
        sqlParameterSource.addValue("priority", aTaskExecution.getPriority());
        sqlParameterSource.addValue("taskNumber", aTaskExecution.getTaskNumber());
        return sqlParameterSource;
    }

    @SuppressWarnings("unchecked")
    private TaskExecution jobTaskRowMappper(ResultSet aRs, int aIndex) throws SQLException {
        return SimpleTaskExecution.of(Json.deserialize(json, aRs.getString("serialized_execution"), Map.class));
    }
}
