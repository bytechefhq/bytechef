
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

package com.bytechef.hermes.connection.service;

import com.bytechef.hermes.connection.config.ConnectionIntTestConfiguration;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.repository.ConnectionRepository;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.repository.TagRepository;
import com.bytechef.test.annotation.EmbeddedSql;

import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Autowired
    private TagRepository tagRepository;

    @BeforeEach
    public void beforeEach() {
        connectionRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testCreate() {
        Connection connection = getConnection();

        Tag tag = tagRepository.save(new Tag("tag1"));

        connection.setTags(List.of(tag));

        connection = connectionService.create(connection);

        assertThat(connection)
            .hasFieldOrPropertyWithValue("name", "name")
            .hasFieldOrPropertyWithValue("tagIds", List.of(tag.getId()));
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testDelete() {
        Connection connection = connectionRepository.save(getConnection());

        connectionService.delete(connection.getId());

        assertThat(connectionRepository.findById(connection.getId())).isNotPresent();
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testGetConnection() {
        Connection connection = getConnection();

        Tag tag = new Tag("tag1");

        tag = tagRepository.save(tag);

        connection.setTags(List.of(tag));

        connection = connectionRepository.save(connection);

        assertThat(connectionService.getConnection(connection.getId())).isEqualTo(connection);
        assertThat(connectionService.getConnections(null, List.of(tag.getId()))).hasSize(1);
    }

    @Test
    @SuppressFBWarnings("NP")
    public void getGetConnections() {
        for (Connection connection : connectionRepository.findAll()) {
            connectionRepository.deleteById(connection.getId());
        }

        connectionRepository.save(getConnection());

        assertThat(connectionService.getConnections(null, null)).hasSize(1);
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testUpdate() {
        Connection connection = connectionRepository.save(getConnection());

        connection.setName("name2");

        Connection updatedConnection = connectionService.update(
            connection.getId(), "name2", List.of(), connection.getVersion());

        assertThat(updatedConnection.getName()).isEqualTo("name2");
    }

    private static Connection getConnection() {
        return Connection.builder()
            .componentName("componentName")
            .name("name")
            .parameters(Map.of("key1", "value1"))
            .version(1)
            .build();
    }
}
