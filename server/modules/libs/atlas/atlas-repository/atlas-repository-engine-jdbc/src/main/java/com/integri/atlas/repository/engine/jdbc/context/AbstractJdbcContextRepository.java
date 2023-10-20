/*
 * Copyright 2016-2018 the original author or authors.
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

package com.integri.atlas.repository.engine.jdbc.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integri.atlas.engine.core.context.Context;
import com.integri.atlas.engine.core.context.MapContext;
import com.integri.atlas.engine.core.context.repository.ContextRepository;
import com.integri.atlas.engine.core.json.Json;
import com.integri.atlas.engine.core.uuid.UUIDGenerator;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractJdbcContextRepository implements ContextRepository {

    private JdbcTemplate jdbc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void push(String aStackId, Context aContext) {
        jdbc.update(getPushSql(), UUIDGenerator.generate(), aStackId, Json.serialize(objectMapper, aContext), new Date());
    }

    @Override
    public Context peek(String aStackId) {
        try {
            String sql =
                "select id,serialized_context from context where stack_id = ? order by create_time desc limit 1";
            return jdbc.queryForObject(sql, new Object[] { aStackId }, this::contextRowMapper);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Context contextRowMapper(ResultSet aResultSet, int aIndex) throws SQLException {
        String serialized = aResultSet.getString(2);
        return new MapContext(Json.deserialize(objectMapper, serialized, Map.class));
    }

    public void setJdbcTemplate(JdbcTemplate aJdbcTemplate) {
        jdbc = aJdbcTemplate;
    }

    public void setObjectMapper(ObjectMapper aObjectMapper) {
        objectMapper = aObjectMapper;
    }

    protected abstract String getPushSql();
}
