/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.atlas.counter.repository.jdbc;

import com.bytechef.atlas.counter.repository.CounterRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Arik Cohen
 */
public class JdbcCounterRepository implements CounterRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcCounterRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void set(String counterName, long value) {
        try {
            jdbcTemplate.queryForObject("select value from counter where id = ? for update", Long.class, counterName);
            jdbcTemplate.update("update counter set value = ? where id = ?", value, counterName);
        } catch (EmptyResultDataAccessException e) {
            jdbcTemplate.update(
                    "insert into counter (id,value,create_time) values (?,?,current_timestamp)", counterName, value);
        }
    }

    @Override
    @Transactional
    public long decrement(String counterName) {
        Long value = jdbcTemplate.queryForObject(
                "select value from counter where id = ? for update", Long.class, counterName);

        value = value - 1;

        jdbcTemplate.update("update counter set value = ? where id = ?", value, counterName);

        return value;
    }

    @Override
    public void delete(String counterName) {
        jdbcTemplate.update("delete from counter where id = ?", counterName);
    }
}
