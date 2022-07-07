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

package com.bytechef.hermes.auth.repository;

import com.bytechef.atlas.util.JsonUtils;
import com.bytechef.hermes.auth.domain.Authentication;
import com.bytechef.hermes.encryption.Encryption;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
public class AuthenticationRepository {

    private final Encryption encryption;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AuthenticationRepository(
            Encryption encryption, NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.encryption = encryption;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void create(Authentication authentication) {
        MapSqlParameterSource sqlParameterSource = createSqlParameterSource(authentication);

        jdbcTemplate.update(
                "insert into authentication (id,name,type,properties,create_time,update_time) values (:id,:name,:type,:properties,:createTime,:updateTime)",
                sqlParameterSource);
    }

    public void delete(String id) {
        jdbcTemplate.update("delete from authentication where id = :id", Map.of("id", id));
    }

    public List<Authentication> findAll() {
        return jdbcTemplate.query("select * from authentication", this::authenticationRowMapper);
    }

    public Authentication findById(String id) {
        List<Authentication> query = jdbcTemplate.query(
                "select * from authentication where id = :id",
                Collections.singletonMap("id", id),
                this::authenticationRowMapper);

        Assert.isTrue(query.size() == 1, "expected 1 result. got " + query.size());

        return query.get(0);
    }

    public Authentication update(Authentication authentication) {
        MapSqlParameterSource sqlParameterSource = createSqlParameterSource(authentication);

        jdbcTemplate.update(
                "update authentication set name=:name,update_time=:updateTime where id = :id ", sqlParameterSource);

        return authentication;
    }

    private Authentication authenticationRowMapper(ResultSet resultSet, int index) throws SQLException {
        Authentication authentication = new Authentication();

        authentication.setCreateTime(resultSet.getTimestamp("create_time"));
        authentication.setName(resultSet.getString("name"));
        authentication.setId(resultSet.getString("id"));
        authentication.setProperties(JsonUtils.deserialize(
                objectMapper, new String(encryption.decrypt(resultSet.getString("properties"))), Map.class));
        authentication.setType(resultSet.getString("type"));
        authentication.setUpdateTime(resultSet.getTimestamp("update_time"));

        return authentication;
    }

    protected MapSqlParameterSource createSqlParameterSource(Authentication authentication) {
        Assert.notNull(authentication, "authentication must not be null");
        Assert.notNull(authentication.getId(), "authentication id must not be null");
        Assert.notNull(authentication.getName(), "authentication name must not be null");
        Assert.notNull(authentication.getType(), "authentication type must not be null");
        Assert.notNull(authentication.getCreateTime(), "authentication createTime must not be null");
        Assert.notNull(authentication.getUpdateTime(), "authentication updateTime must not be null");

        MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource();

        sqlParameterSource.addValue("id", authentication.getId());
        sqlParameterSource.addValue("name", authentication.getName());
        sqlParameterSource.addValue(
                "properties",
                new String(encryption.encrypt(JsonUtils.serialize(objectMapper, authentication.getProperties()))));
        sqlParameterSource.addValue("type", authentication.getType());
        sqlParameterSource.addValue("createTime", authentication.getCreateTime());
        sqlParameterSource.addValue("updateTime", authentication.getUpdateTime());

        return sqlParameterSource;
    }
}
