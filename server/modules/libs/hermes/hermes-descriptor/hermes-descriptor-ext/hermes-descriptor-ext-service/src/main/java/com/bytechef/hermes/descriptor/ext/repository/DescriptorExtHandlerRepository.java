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

package com.bytechef.hermes.descriptor.ext.repository;

import com.bytechef.atlas.util.JsonUtils;
import com.bytechef.hermes.descriptor.ext.domain.DescriptorExtHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
public class DescriptorExtHandlerRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public DescriptorExtHandlerRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void create(DescriptorExtHandler descriptorExtHandler) {
        MapSqlParameterSource sqlParameterSource = createSqlParameterSource(descriptorExtHandler);

        jdbcTemplate.update(
                "insert into descriptor_ext_handler (name,versions,authentication_exists,type,properties,create_time,update_time) values (:name,:versions,:authenticationExists,:type,:properties,:createTime,:updateTime)",
                sqlParameterSource);
    }

    public void delete(String name) {
        jdbcTemplate.update("delete from descriptor_ext_handler where name = :name", Map.of("name", name));
    }

    public List<DescriptorExtHandler> findAll() {
        return jdbcTemplate.query("select * from descriptor_ext_handler", this::extDescriptorHandlerRowMapper);
    }

    public List<DescriptorExtHandler> findAllByType(String type) {
        return jdbcTemplate.query(
                "select * from descriptor_ext_handler where type = :type",
                Collections.singletonMap("type", type),
                this::extDescriptorHandlerRowMapper);
    }

    public DescriptorExtHandler findByName(String name) {
        List<DescriptorExtHandler> query = jdbcTemplate.query(
                "select * from descriptor_ext_handler where name = :name",
                Collections.singletonMap("name", name),
                this::extDescriptorHandlerRowMapper);

        if (query.size() == 1) {
            return query.get(0);
        }

        return null;
    }

    public void merge(DescriptorExtHandler descriptorExtHandler) {
        MapSqlParameterSource sqlParameterSource = createSqlParameterSource(descriptorExtHandler);
        jdbcTemplate.update(
                "update descriptor_ext_handler set versions=:versions,authentication_exists=:authenticationExists,type=:type,properties=:properties,create_time=:createTime,update_time=:updateTime where name = :name ",
                sqlParameterSource);
    }

    private DescriptorExtHandler extDescriptorHandlerRowMapper(ResultSet resultSet, int index) throws SQLException {
        DescriptorExtHandler descriptorExtHandler = new DescriptorExtHandler();

        descriptorExtHandler.setName(resultSet.getString("name"));
        descriptorExtHandler.setVersions(
                JsonUtils.deserialize(objectMapper, resultSet.getString("versions"), Set.class));
        descriptorExtHandler.setAuthenticationExists(resultSet.getBoolean("authentication_exists"));
        descriptorExtHandler.setType(resultSet.getString("type"));
        descriptorExtHandler.setProperties(
                JsonUtils.deserialize(objectMapper, resultSet.getString("properties"), Map.class));
        descriptorExtHandler.setCreateTime(resultSet.getTimestamp("create_time"));
        descriptorExtHandler.setUpdateTime(resultSet.getTimestamp("update_time"));

        return descriptorExtHandler;
    }

    protected MapSqlParameterSource createSqlParameterSource(DescriptorExtHandler descriptorExtHandler) {
        Assert.notNull(descriptorExtHandler, "extDescriptorHandler must not be null");
        Assert.notNull(descriptorExtHandler.getName(), "extDescriptorHandler name must not be null");
        Assert.notNull(descriptorExtHandler.getVersions(), "extDescriptorHandler versions must not be null");
        Assert.notNull(
                descriptorExtHandler.isAuthenticationExists(),
                "extDescriptorHandler authenticationExists must not be null");
        Assert.notNull(descriptorExtHandler.getType(), "extDescriptorHandler type must not be null");
        Assert.notNull(descriptorExtHandler.getProperties(), "extDescriptorHandler properties must not be null");
        Assert.notNull(descriptorExtHandler.getCreateTime(), "extDescriptorHandler createTime must not be null");
        Assert.notNull(descriptorExtHandler.getUpdateTime(), "extDescriptorHandler updateTime must not be null");

        MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource();

        sqlParameterSource.addValue("name", descriptorExtHandler.getName());
        sqlParameterSource.addValue("versions", JsonUtils.serialize(objectMapper, descriptorExtHandler.getVersions()));
        sqlParameterSource.addValue("authenticationExists", descriptorExtHandler.isAuthenticationExists());
        sqlParameterSource.addValue("type", descriptorExtHandler.getType());
        sqlParameterSource.addValue(
                "properties", JsonUtils.serialize(objectMapper, descriptorExtHandler.getProperties()));
        sqlParameterSource.addValue("createTime", descriptorExtHandler.getCreateTime());
        sqlParameterSource.addValue("updateTime", descriptorExtHandler.getUpdateTime());

        return sqlParameterSource;
    }
}
