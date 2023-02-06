
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

package com.bytechef.hermes.connection.service.impl;

import com.bytechef.hermes.connection.config.ConnectionIntTestConfiguration;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.repository.ConnectionRepository;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.test.annotation.EmbeddedSql;
import java.util.Map;
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
public class ConnectionServiceIntTest {

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private ConnectionRepository connectionRepository;

    @BeforeEach
    public void beforeEach() {
        connectionRepository.deleteAll();
    }

    @Test
    public void testCreate() {
        Connection connection = getConnection();

        connection = connectionService.create(connection);

        Assertions.assertEquals("name", connection.getName());
        Assertions.assertEquals(Map.of("key1", "value1"), connection.getParameters());
    }

    @Test
    public void testDelete() {
        Connection connection = connectionRepository.save(getConnection());

        connectionService.delete(connection.getId());

        Assertions.assertFalse(connectionRepository.findById(connection.getId())
            .isPresent());
    }

    @Test
    public void testGetConnection() {
        Connection connection = connectionRepository.save(getConnection());

        Assertions.assertEquals(connection, connectionService.getConnection(connection.getId()));
    }

    @Test
    public void getGetConnections() {
        for (Connection connection : connectionRepository.findAll()) {
            connectionRepository.deleteById(connection.getId());
        }

        connectionRepository.save(getConnection());

        Assertions.assertEquals(1, connectionService.getConnections()
            .size());
    }

    @Test
    public void testUpdate() {
        Connection connection = connectionRepository.save(getConnection());

        connection.setName("name2");

        Connection updatedConnection = connectionService.update(connection);

        Assertions.assertEquals("name2", updatedConnection.getName());
    }

    private static Connection getConnection() {
        Connection connection = new Connection();

        connection.setComponentName("componentName");
        connection.setComponentVersion(1);
        connection.setName("name");
        connection.setParameters(Map.of("key1", "value1"));

        return connection;
    }
}
