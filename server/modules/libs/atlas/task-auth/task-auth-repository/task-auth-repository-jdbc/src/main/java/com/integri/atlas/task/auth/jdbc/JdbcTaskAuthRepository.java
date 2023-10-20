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

package com.integri.atlas.task.auth.jdbc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integri.atlas.encryption.Encryption;
import com.integri.atlas.engine.json.Json;
import com.integri.atlas.task.auth.SimpleTaskAuth;
import com.integri.atlas.task.auth.TaskAuth;
import com.integri.atlas.task.auth.repository.TaskAuthRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
public class JdbcTaskAuthRepository implements TaskAuthRepository {

    private Encryption encryption;
    private NamedParameterJdbcTemplate jdbcTemplate;
    private ObjectMapper objectMapper;

    @Override
    public void create(TaskAuth taskAuth) {
        MapSqlParameterSource sqlParameterSource = createSqlParameterSource(taskAuth);

        jdbcTemplate.update(
            "insert into task_auth (id,create_time,name,properties,update_time,type) values (:id,:createTime,:name,:properties,:updateTime,:type)",
            sqlParameterSource
        );
    }

    @Override
    public void delete(String id) {
        jdbcTemplate.update("delete from task_auth where id = :id", Map.of("id", id));
    }

    @Override
    public List<TaskAuth> findAll() {
        return jdbcTemplate.query("select * from task_auth", this::taskAuthRowMapper);
    }

    @Override
    public TaskAuth findById(String id) {
        List<TaskAuth> query = jdbcTemplate.query(
            "select * from task_auth where id = :id",
            Collections.singletonMap("id", id),
            this::taskAuthRowMapper
        );

        Assert.isTrue(query.size() == 1, "expected 1 result. got " + query.size());

        return query.get(0);
    }

    @Override
    public TaskAuth update(TaskAuth taskAuth) {
        MapSqlParameterSource sqlParameterSource = createSqlParameterSource(taskAuth);

        jdbcTemplate.update(
            "update task_auth set name=:name,update_time=:updateTime where id = :id ",
            sqlParameterSource
        );

        return taskAuth;
    }

    public void setEncryption(Encryption encryption) {
        this.encryption = encryption;
    }

    public void setJdbcTemplate(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private TaskAuth taskAuthRowMapper(ResultSet resultSet, int index) throws SQLException {
        Map<String, Object> map = new HashMap<>();

        map.put("createTime", resultSet.getTimestamp("create_time"));
        map.put("name", resultSet.getString("name"));
        map.put("id", resultSet.getString("id"));
        map.put(
            "properties",
            Json.deserialize(objectMapper, new String(encryption.decrypt(resultSet.getString("properties"))), Map.class)
        );
        map.put("type", resultSet.getString("type"));
        map.put("updateTime", resultSet.getTimestamp("update_time"));

        return new SimpleTaskAuth(map);
    }

    protected MapSqlParameterSource createSqlParameterSource(TaskAuth taskAuth) {
        SimpleTaskAuth simpleTaskAuth = new SimpleTaskAuth(taskAuth);

        Assert.notNull(taskAuth, "taskAuth must not be null");
        Assert.notNull(taskAuth.getCreateTime(), "taskAuth createTime must not be null");
        Assert.notNull(taskAuth.getId(), "taskAuth id must not be null");
        Assert.notNull(taskAuth.getName(), "taskAuth name must not be null");
        Assert.notNull(taskAuth.getType(), "taskAuth type must not be null");
        Assert.notNull(taskAuth.getUpdateTime(), "taskAuth updateTime must not be null");

        MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource();

        sqlParameterSource.addValue("id", simpleTaskAuth.getId());
        sqlParameterSource.addValue("createTime", simpleTaskAuth.getCreateTime());
        sqlParameterSource.addValue("name", simpleTaskAuth.getName());
        sqlParameterSource.addValue(
            "properties",
            new String(encryption.encrypt(Json.serialize(objectMapper, simpleTaskAuth.getProperties())))
        );
        sqlParameterSource.addValue("type", simpleTaskAuth.getType());
        sqlParameterSource.addValue("updateTime", simpleTaskAuth.getUpdateTime());

        return sqlParameterSource;
    }
}
