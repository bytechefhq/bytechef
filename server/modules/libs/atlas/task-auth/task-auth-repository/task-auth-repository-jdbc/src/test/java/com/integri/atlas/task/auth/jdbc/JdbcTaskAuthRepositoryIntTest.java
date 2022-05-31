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
import com.integri.atlas.encryption.EncryptionKey;
import com.integri.atlas.engine.uuid.UUIDGenerator;
import com.integri.atlas.task.auth.SimpleTaskAuth;
import com.integri.atlas.task.auth.TaskAuth;
import com.integri.atlas.task.auth.repository.TaskAuthRepository;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest
public class JdbcTaskAuthRepositoryIntTest {

    @Autowired
    private JdbcTaskAuthRepository jdbcTaskAuthRepository;

    @BeforeEach
    public void beforeEach() {
        List<TaskAuth> taskAuths = jdbcTaskAuthRepository.findAll();

        for (TaskAuth taskAuth : taskAuths) {
            jdbcTaskAuthRepository.delete(taskAuth.getId());
        }
    }

    @Test
    public void testCreate() {
        SimpleTaskAuth taskAuth = getSimpleTaskAuth();

        jdbcTaskAuthRepository.create(taskAuth);

        Assertions.assertEquals(taskAuth, jdbcTaskAuthRepository.findById(taskAuth.getId()));
    }

    @Test
    public void testDelete() {
        SimpleTaskAuth taskAuth = getSimpleTaskAuth();

        jdbcTaskAuthRepository.create(taskAuth);

        Assertions.assertEquals(1, jdbcTaskAuthRepository.findAll().size());

        jdbcTaskAuthRepository.delete(taskAuth.getId());

        Assertions.assertEquals(0, jdbcTaskAuthRepository.findAll().size());
    }

    @Test
    public void testFindAll() {
        SimpleTaskAuth taskAuth = getSimpleTaskAuth();

        jdbcTaskAuthRepository.create(taskAuth);

        Assertions.assertEquals(1, jdbcTaskAuthRepository.findAll().size());
    }

    @Test
    public void testFindById() {
        SimpleTaskAuth taskAuth = getSimpleTaskAuth();

        jdbcTaskAuthRepository.create(taskAuth);

        Assertions.assertEquals(taskAuth, jdbcTaskAuthRepository.findById(taskAuth.getId()));
    }

    @Test
    public void testMerge() {
        SimpleTaskAuth taskAuth = getSimpleTaskAuth();

        jdbcTaskAuthRepository.create(taskAuth);

        taskAuth.setName("name2");
        taskAuth.setProperties(Map.of("key2", "value2"));
        taskAuth.setUpdateTime(new Date());
        taskAuth.setType("type2");

        jdbcTaskAuthRepository.update(taskAuth);

        TaskAuth updatedTaskAuth = jdbcTaskAuthRepository.findById(taskAuth.getId());

        Assertions.assertEquals(taskAuth.getName(), updatedTaskAuth.getName());
        Assertions.assertEquals(Map.of("key1", "value1"), updatedTaskAuth.getProperties());
        Assertions.assertEquals(taskAuth.getUpdateTime(), updatedTaskAuth.getUpdateTime());
        Assertions.assertEquals("type", updatedTaskAuth.getType());
    }

    private static SimpleTaskAuth getSimpleTaskAuth() {
        SimpleTaskAuth taskAuth = new SimpleTaskAuth();

        taskAuth.setName("name");
        taskAuth.setId(UUIDGenerator.generate());
        taskAuth.setCreateTime(new Date());
        taskAuth.setProperties(Map.of("key1", "value1"));
        taskAuth.setUpdateTime(new Date());
        taskAuth.setType("type");

        return taskAuth;
    }

    @TestConfiguration
    public static class JdbcTaskAuthRepositoryIntTestConfiguration {

        @Bean
        EncryptionKey encryptionKey() {
            return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
        }

        @Bean
        TaskAuthRepository taskAuthRepository(
            Encryption encryption,
            NamedParameterJdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper
        ) {
            JdbcTaskAuthRepository jdbcTaskAuthRepository = new JdbcTaskAuthRepository();

            jdbcTaskAuthRepository.setEncryption(encryption);
            jdbcTaskAuthRepository.setJdbcTemplate(jdbcTemplate);
            jdbcTaskAuthRepository.setObjectMapper(objectMapper);

            return jdbcTaskAuthRepository;
        }
    }
}
