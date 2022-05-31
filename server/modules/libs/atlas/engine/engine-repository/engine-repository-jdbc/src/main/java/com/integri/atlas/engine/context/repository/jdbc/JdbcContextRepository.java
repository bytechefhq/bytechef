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

package com.integri.atlas.engine.context.repository.jdbc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integri.atlas.engine.context.Context;
import com.integri.atlas.engine.context.MapContext;
import com.integri.atlas.engine.context.repository.ContextRepository;
import com.integri.atlas.engine.json.Json;
import com.integri.atlas.engine.uuid.UUIDGenerator;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Arik Cohe
 * @author Ivica Cardic
 */
public class JdbcContextRepository implements ContextRepository {

    private JdbcTemplate jdbcTemplate;
    private ObjectMapper objectMapper;

    @Override
    public void delete(String stackId) {
        jdbcTemplate.update("delete from context where stack_id = ?", stackId);
    }

    @Override
    public void push(String stackId, Context context) {
        jdbcTemplate.update(
            "insert into context (id,stack_id,serialized_context,create_time) values (?,?,?,?)",
            UUIDGenerator.generate(),
            stackId,
            Json.serialize(objectMapper, context),
            new Date()
        );
    }

    @Override
    public Context peek(String stackId) {
        try {
            String sql =
                "select id,serialized_context from context where stack_id = ? order by create_time desc limit 1";
            return jdbcTemplate.queryForObject(sql, new Object[] { stackId }, this::contextRowMapper);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Context contextRowMapper(ResultSet aResultSet, int aIndex) throws SQLException {
        String serialized = aResultSet.getString(2);

        return new MapContext(Json.deserialize(objectMapper, serialized, Map.class));
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
}
