
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

package com.bytechef.hermes.connection.repository;

import com.bytechef.hermes.connection.config.ConnectionIntTestConfiguration;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.test.annotation.EmbeddedSql;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Ivica Cardic
 */
@EmbeddedSql
@SpringBootTest(classes = ConnectionIntTestConfiguration.class)
public class ConnectionRepositoryIntTest {

    @Autowired
    private ConnectionRepository connectionRepository;

    @BeforeEach
    public void beforeEach() {
        connectionRepository.deleteAll();
    }

    @Test
    public void testCreate() {
        Connection connection = connectionRepository.save(getConnection());

        Assertions.assertEquals(
            connection, connectionRepository.findById(connection.getId())
                .get());
    }

    @Test
    public void testDelete() {
        Connection connection = connectionRepository.save(getConnection());

        Assertions.assertEquals(
            connection, connectionRepository.findById(connection.getId())
                .orElseThrow());

        connectionRepository.deleteById(connection.getId());

        Assertions.assertFalse(connectionRepository.findById(connection.getId())
            .isPresent());
    }

    @Test
    public void testFindAll() {
        connectionRepository.save(getConnection());

        Assertions.assertEquals(
            1,
            StreamSupport.stream(connectionRepository.findAll()
                .spliterator(), false)
                .count());
    }

    @Test
    public void testFindById() {
        Connection connection = connectionRepository.save(getConnection());

        Assertions.assertEquals(
            connection, connectionRepository.findById(connection.getId())
                .get());
    }

    @Test
    public void testUpdate() {
        Connection connection = connectionRepository.save(getConnection());

        connection.setName("name2");
        connection.setParameters(Map.of("key2", "value2"));

        connectionRepository.save(connection);

        Connection updatedConnection = connectionRepository.findById(connection.getId())
            .get();

        Assertions.assertEquals("name2", updatedConnection.getName());
        Assertions.assertEquals(Map.of("key2", "value2"), updatedConnection.getParameters());
    }

    private static Connection getConnection() {
        Connection connection = new Connection();

        connection.setComponentName("componentName");
        connection.setKey("key");
        connection.setName("name");
        connection.setParameters(Map.of("key1", "value1"));
        connection.setVersion(1);

        return connection;
    }
}
