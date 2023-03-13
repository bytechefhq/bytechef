
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

package com.bytechef.hermes.connection.facade;

import com.bytechef.hermes.connection.config.ConnectionIntTestConfiguration;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.repository.ConnectionRepository;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.connection.service.ConnectionServiceImpl;
import com.bytechef.hermes.definition.registry.config.WorkerDefinitionRegistryConfiguration;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.repository.TagRepository;
import com.bytechef.tag.service.TagService;
import com.bytechef.test.annotation.EmbeddedSql;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = {
        "com.bytechef.tag"
    })
@EmbeddedSql
@SpringBootTest(classes = ConnectionIntTestConfiguration.class, properties = {
    "spring.application.name=server-app"
})
@Import({
    WorkerDefinitionRegistryConfiguration.class, ConnectionFacadeIntTest.ConnectionFacadeIntTestConfiguration.class
})
public class ConnectionFacadeIntTest {

    @Autowired
    private ConnectionFacade connectionFacade;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private TagRepository tagRepository;

    @BeforeEach
    @SuppressFBWarnings("NP")
    public void beforeEach() {
        connectionRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @Test
    public void testCreate() {
        Connection connection = new Connection();

        connection.setComponentName("componentName");
        connection.setKey("key");
        connection.setName("name1");
        connection.setTags(List.of(new Tag("tag1")));

        connection = connectionFacade.create(connection);

        assertThat(connection.getName()).isEqualTo("name1");
        assertThat(connection.getId()).isNotNull();
        assertThat(connection.getTagIds()).hasSize(1);
    }

    @Test
    public void testDelete() {
        Connection connection1 = new Connection();

        connection1.setComponentName("componentName");
        connection1.setKey("key");
        connection1.setName("name1");
        connection1.setTags(List.of(new Tag("tag1")));

        connection1 = connectionFacade.create(connection1);

        Connection connection2 = new Connection();

        connection2.setComponentName("componentName");
        connection2.setKey("key");
        connection2.setName("name2");
        connection2.setTags(List.of(new Tag("tag1")));

        connection2 = connectionFacade.create(connection2);

        assertThat(connectionRepository.count()).isEqualTo(2);
        assertThat(tagRepository.count()).isEqualTo(1);

        connectionFacade.delete(connection1.getId());

        assertThat(connectionRepository.count()).isEqualTo(1);

        connectionFacade.delete(connection2.getId());

        assertThat(connectionRepository.count()).isEqualTo(0);
        assertThat(tagRepository.count()).isEqualTo(1);
    }

    @Test
    public void testGetConnection() {
        Connection connection = new Connection();

        connection.setComponentName("componentName");
        connection.setKey("key");
        connection.setName("name");

        Tag tag1 = tagRepository.save(new Tag("tag1"));
        Tag tag2 = tagRepository.save(new Tag("tag2"));

        connection.setTags(List.of(tag1, tag2));

        connection = connectionRepository.save(connection);

        assertThat(connectionFacade.getConnection(connection.getId()))
            .isEqualTo(connection)
            .hasFieldOrPropertyWithValue("tags", List.of(tag1, tag2));
    }

    @Test
    public void testGetConnections() {
        Connection connection = new Connection();

        connection.setComponentName("componentName");
        connection.setKey("key");
        connection.setName("name");

        Tag tag1 = tagRepository.save(new Tag("tag1"));
        Tag tag2 = tagRepository.save(new Tag("tag2"));

        connection.setTags(List.of(tag1, tag2));

        connection = connectionRepository.save(connection);

        List<Connection> connections = connectionFacade.getConnections(null, null);

        assertThat(connections).isEqualTo(List.of(connection));

        connection = connections.get(0);

        assertThat(connectionFacade.getConnection(connection.getId()))
            .isEqualTo(connection)
            .hasFieldOrPropertyWithValue("tags", List.of(tag1, tag2));
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testGetConnectionTags() {
        Connection connection = new Connection();

        Tag tag1 = tagRepository.save(new Tag("tag1"));

        connection.setComponentName("componentName");
        connection.setKey("key");
        connection.setName("name");
        connection.setTags(List.of(tag1, tagRepository.save(new Tag("tag2"))));

        connectionRepository.save(connection);

        assertThat(connectionFacade.getConnectionTags()
            .stream()
            .map(Tag::getName)
            .collect(Collectors.toSet())).contains("tag1", "tag2");

        connection = new Connection();

        connection.setComponentName("componentName");
        connection.setKey("key");
        connection.setName("name2");

        tag1 = tagRepository.findById(tag1.getId())
            .orElseThrow();

        connection.setTags(List.of(tag1, tagRepository.save(new Tag("tag3"))));

        connectionRepository.save(connection);

        assertThat(connectionFacade.getConnectionTags()
            .stream()
            .map(Tag::getName)
            .collect(Collectors.toSet())).contains("tag1", "tag2", "tag3");

        connectionRepository.deleteById(connection.getId());

        assertThat(connectionFacade.getConnectionTags()
            .stream()
            .map(Tag::getName)
            .collect(Collectors.toSet())).contains("tag1", "tag2");
    }

    @Test
    public void testUpdate() {
        Connection connection = new Connection();

        connection.setComponentName("componentName");
        connection.setKey("key");
        connection.setName("name");

        Tag tag1 = new Tag("tag1");

        connection.setTags(List.of(tag1, tagRepository.save(new Tag("tag2"))));

        connection = connectionFacade.create(connection);

        assertThat(connection.getTagIds()).hasSize(2);

        connection.setTags(List.of(tag1));

        connectionRepository.save(connection);

        connection = connectionFacade.update(connection);

        assertThat(connection.getTagIds()).hasSize(1);
    }

    @TestConfiguration
    public static class ConnectionFacadeIntTestConfiguration {

        @Bean
        ConnectionFacade connectionFacade(
            ConnectionDefinitionService connectionDefinitionService, ConnectionService connectionService,
            TagService tagService) {

            return new ConnectionFacadeImpl(connectionDefinitionService, connectionService, tagService);
        }

        @Bean
        ConnectionService connectionService(ConnectionRepository connectionRepository) {
            return new ConnectionServiceImpl(connectionRepository);
        }
    }
}
