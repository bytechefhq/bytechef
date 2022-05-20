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

package com.integri.atlas.engine.workflow.repository.jdbc;

import com.integri.atlas.engine.uuid.UUIDGenerator;
import com.integri.atlas.engine.workflow.Workflow;
import com.integri.atlas.engine.workflow.WorkflowFormat;
import com.integri.atlas.engine.workflow.WorkflowResource;
import com.integri.atlas.engine.workflow.repository.WorkflowRepository;
import com.integri.atlas.engine.workflow.repository.mapper.WorkflowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.Assert;

/**
 * @author Matija Petanjek
 */
public class JdbcWorkflowRepository implements WorkflowRepository {

    private NamedParameterJdbcTemplate jdbcTemplate;
    private WorkflowMapper workflowMapper;

    @Override
    public Workflow findOne(String aId) {
        List<Workflow> workflows = jdbcTemplate.query(
            "select * from workflow w where w.id = :id",
            Collections.singletonMap("id", aId),
            this::workflowRowMapper
        );

        Assert.isTrue(workflows.size() == 1, "Expected 1 results, got " + workflows.size());

        return workflows.get(0);
    }

    private Workflow workflowRowMapper(ResultSet resultSet, int i) throws SQLException {
        return workflowMapper.readValue(
            new WorkflowResource(
                resultSet.getString("id"),
                new ByteArrayResource(resultSet.getBytes("content")),
                WorkflowFormat.valueOf(resultSet.getString("format").toUpperCase())
            )
        );
    }

    @Override
    public List<Workflow> findAll() {
        return jdbcTemplate.query("select * from workflow w", this::workflowRowMapper);
    }

    @Override
    public Workflow create(String content, String format) {
        String id = UUIDGenerator.generate();
        MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource();

        sqlParameterSource.addValue("id", id);
        sqlParameterSource.addValue("content", content);
        sqlParameterSource.addValue("format", format);

        jdbcTemplate.update(
            "insert into workflow (id, content, format) values (:id, :content, :format)",
            sqlParameterSource
        );

        return findOne(id);
    }

    @Override
    public Workflow update(String id, String content, String format) {
        MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource();

        sqlParameterSource.addValue("id", id);
        sqlParameterSource.addValue("content", content);
        sqlParameterSource.addValue("format", format);

        jdbcTemplate.update(
            "update workflow set content = :content, format = :format where id = :id",
            sqlParameterSource
        );

        return findOne(id);
    }

    public void setJdbcTemplate(NamedParameterJdbcTemplate aJdbcTemplate) {
        jdbcTemplate = aJdbcTemplate;
    }

    public void setWorkflowMapper(WorkflowMapper aWorkflowMapper) {
        workflowMapper = aWorkflowMapper;
    }
}
