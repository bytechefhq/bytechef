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

package com.bytechef.hermes.auth.jdbc;

import com.bytechef.atlas.util.JSONUtils;
import com.bytechef.hermes.auth.domain.Authentication;
import com.bytechef.hermes.auth.domain.SimpleAuthentication;
import com.bytechef.hermes.auth.repository.AuthenticationRepository;
import com.bytechef.hermes.encryption.Encryption;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class JdbcAuthenticationRepository implements AuthenticationRepository {

    private Encryption encryption;
    private NamedParameterJdbcTemplate jdbcTemplate;
    private ObjectMapper objectMapper;

    @Override
    public void create(Authentication authentication) {
        MapSqlParameterSource sqlParameterSource = createSqlParameterSource(authentication);

        jdbcTemplate.update(
                "insert into authentication (id,create_time,name,properties,update_time,type) values (:id,:createTime,:name,:properties,:updateTime,:type)",
                sqlParameterSource);
    }

    @Override
    public void delete(String id) {
        jdbcTemplate.update("delete from authentication where id = :id", Map.of("id", id));
    }

    @Override
    public List<Authentication> findAll() {
        return jdbcTemplate.query("select * from authentication", this::authenticationRowMapper);
    }

    @Override
    public Authentication findById(String id) {
        List<Authentication> query = jdbcTemplate.query(
                "select * from authentication where id = :id",
                Collections.singletonMap("id", id),
                this::authenticationRowMapper);

        Assert.isTrue(query.size() == 1, "expected 1 result. got " + query.size());

        return query.get(0);
    }

    @Override
    public Authentication update(Authentication authentication) {
        MapSqlParameterSource sqlParameterSource = createSqlParameterSource(authentication);

        jdbcTemplate.update(
                "update authentication set name=:name,update_time=:updateTime where id = :id ", sqlParameterSource);

        return authentication;
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

    private Authentication authenticationRowMapper(ResultSet resultSet, int index) throws SQLException {
        Map<String, Object> map = new HashMap<>();

        map.put("createTime", resultSet.getTimestamp("create_time"));
        map.put("name", resultSet.getString("name"));
        map.put("id", resultSet.getString("id"));
        map.put(
                "properties",
                JSONUtils.deserialize(
                        objectMapper, new String(encryption.decrypt(resultSet.getString("properties"))), Map.class));
        map.put("type", resultSet.getString("type"));
        map.put("updateTime", resultSet.getTimestamp("update_time"));

        return new SimpleAuthentication(map);
    }

    protected MapSqlParameterSource createSqlParameterSource(Authentication authentication) {
        SimpleAuthentication simpleAuthentication = new SimpleAuthentication(authentication);

        Assert.notNull(authentication, "authentication must not be null");
        Assert.notNull(authentication.getCreateTime(), "authentication createTime must not be null");
        Assert.notNull(authentication.getId(), "authentication id must not be null");
        Assert.notNull(authentication.getName(), "authentication name must not be null");
        Assert.notNull(authentication.getType(), "authentication type must not be null");
        Assert.notNull(authentication.getUpdateTime(), "authentication updateTime must not be null");

        MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource();

        sqlParameterSource.addValue("id", simpleAuthentication.getId());
        sqlParameterSource.addValue("createTime", simpleAuthentication.getCreateTime());
        sqlParameterSource.addValue("name", simpleAuthentication.getName());
        sqlParameterSource.addValue(
                "properties",
                new String(
                        encryption.encrypt(JSONUtils.serialize(objectMapper, simpleAuthentication.getProperties()))));
        sqlParameterSource.addValue("type", simpleAuthentication.getType());
        sqlParameterSource.addValue("updateTime", simpleAuthentication.getUpdateTime());

        return sqlParameterSource;
    }
}
