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

package com.integri.atlas.task.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integri.atlas.encryption.Encryption;
import com.integri.atlas.encryption.EncryptionKey;
import com.integri.atlas.engine.uuid.UUIDGenerator;
import com.integri.atlas.task.auth.jdbc.JdbcTaskAuthRepository;
import com.integri.atlas.task.auth.repository.TaskAuthRepository;
import com.integri.atlas.task.auth.service.TaskAuthService;
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
public class TaskAuthServiceIntTest {

    @Autowired
    private TaskAuthService taskAuthService;

    @Autowired
    private TaskAuthRepository taskAuthRepository;

    @BeforeEach
    public void beforeEach() {
        List<TaskAuth> taskAuths = taskAuthRepository.findAll();

        for (TaskAuth taskAuth : taskAuths) {
            taskAuthRepository.delete(taskAuth.getId());
        }
    }

    @Test
    public void testCreate() {
        TaskAuth taskAuth = taskAuthService.create("name", "type", Map.of("key1", "value1"));

        Assertions.assertEquals("name", taskAuth.getName());
        Assertions.assertEquals(Map.of("key1", "value1"), taskAuth.getProperties());
        Assertions.assertEquals("type", taskAuth.getType());
    }

    @Test
    public void testDelete() {
        TaskAuth taskAuth = getSimpleTaskAuth();

        taskAuthRepository.create(taskAuth);

        Assertions.assertEquals(1, taskAuthRepository.findAll().size());

        taskAuthService.delete(taskAuth.getId());

        Assertions.assertEquals(0, taskAuthRepository.findAll().size());
    }

    @Test
    public void testGetTaskAuth() {
        TaskAuth taskAuth = getSimpleTaskAuth();

        taskAuthRepository.create(taskAuth);

        Assertions.assertEquals(taskAuth, taskAuthService.getTaskAuth(taskAuth.getId()));
    }

    @Test
    public void getGetTaskAuths() {
        TaskAuth taskAuth = getSimpleTaskAuth();

        taskAuthRepository.create(taskAuth);

        Assertions.assertEquals(1, taskAuthService.getTaskAuths().size());
    }

    @Test
    public void testUpdate() {
        TaskAuth taskAuth = getSimpleTaskAuth();

        taskAuthRepository.create(taskAuth);

        TaskAuth updatedTaskAuth = taskAuthService.update(taskAuth.getId(), "name2");

        Assertions.assertEquals("name2", updatedTaskAuth.getName());
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
    public static class TaskAuthServiceIntTestConfiguration {

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
