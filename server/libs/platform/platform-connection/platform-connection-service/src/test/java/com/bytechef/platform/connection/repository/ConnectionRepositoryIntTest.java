/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.connection.repository;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.platform.connection.config.ConnectionIntTestConfiguration;
import com.bytechef.platform.connection.config.ConnectionIntTestConfigurationSharedMocks;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ConnectionIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@ConnectionIntTestConfigurationSharedMocks
public class ConnectionRepositoryIntTest {

    @Autowired
    private ConnectionRepository connectionRepository;

    @AfterEach
    public void afterEach() {
        connectionRepository.deleteAll();
    }

    @Test
    public void testCreate() {
        Connection connection = connectionRepository.save(getConnection());

        Assertions.assertEquals(connection, OptionalUtils.get(connectionRepository.findById(connection.getId())));
    }

    @Test
    public void testDelete() {
        Connection connection = connectionRepository.save(getConnection());

        Assertions.assertEquals(
            connection, OptionalUtils.get(connectionRepository.findById(Validate.notNull(connection.getId(), "id"))));

        connectionRepository.deleteById(Validate.notNull(connection.getId(), "id"));

        Assertions.assertFalse(
            OptionalUtils.isPresent(connectionRepository.findById(Validate.notNull(connection.getId(), "id"))));
    }

    @Test
    public void testFindAll() {
        connectionRepository.save(getConnection());

        Assertions.assertEquals(1, CollectionUtils.count(connectionRepository.findAll()));
    }

    @Test
    public void testFindById() {
        Connection connection = connectionRepository.save(getConnection());

        Assertions.assertEquals(
            connection, OptionalUtils.get(connectionRepository.findById(Validate.notNull(connection.getId(), "id"))));
    }

    @Test
    public void testUpdate() {
        Connection connection = connectionRepository.save(getConnection());

        connection.setName("name2");
        connection.setParameters(Map.of("key2", "value2"));

        connectionRepository.save(connection);

        Connection updatedConnection = OptionalUtils.get(
            connectionRepository.findById(Validate.notNull(connection.getId(), "id")));

        Assertions.assertEquals("name2", updatedConnection.getName());
        Assertions.assertEquals(Map.of("key2", "value2"), updatedConnection.getParameters());
    }

    private static Connection getConnection() {
        return Connection.builder()
            .authorizationType(AuthorizationType.BASIC_AUTH)
            .componentName("componentName")
            .name("name")
            .parameters(Map.of("key1", "value1"))
            .type(PlatformType.AUTOMATION)
            .build();
    }
}
