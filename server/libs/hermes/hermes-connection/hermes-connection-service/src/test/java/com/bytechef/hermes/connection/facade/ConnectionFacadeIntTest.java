/*
 * Copyright 2023-present ByteChef Inc.
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

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.registry.facade.ConnectionDefinitionFacade;
import com.bytechef.hermes.component.registry.service.ConnectionDefinitionService;
import com.bytechef.hermes.configuration.facade.WorkflowConnectionFacade;
import com.bytechef.hermes.connection.config.ConnectionIntTestConfiguration;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.dto.ConnectionDTO;
import com.bytechef.hermes.connection.repository.ConnectionRepository;
import com.bytechef.hermes.oauth2.service.OAuth2Service;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.repository.TagRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ConnectionIntTestConfiguration.class,
    properties = {
        "spring.application.name=server-app"
    })
@Import(PostgreSQLContainerConfiguration.class)
public class ConnectionFacadeIntTest {

    @Autowired
    private ConnectionFacade connectionFacade;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private TagRepository tagRepository;

    @AfterEach
    public void afterEach() {
        connectionRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @Test
    public void testCreate() {
        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .componentName("componentName")
            .name("name1")
            .tags(List.of(new Tag("tag1")))
            .build();

        connectionDTO = connectionFacade.create(connectionDTO, 1);

        Assertions.assertThat(connectionDTO.name())
            .isEqualTo("name1");
        Assertions.assertThat(connectionDTO.id())
            .isNotNull();
        Assertions.assertThat(connectionDTO.tags())
            .hasSize(1);
    }

    @Test
    public void testDelete() {
        ConnectionDTO connectionDTO1 = ConnectionDTO.builder()
            .componentName("componentName")
            .name("name1")
            .tags(List.of(new Tag("tag1")))
            .build();

        connectionDTO1 = connectionFacade.create(connectionDTO1, 1);

        ConnectionDTO connectionDTO2 = ConnectionDTO.builder()
            .componentName("componentName")
            .name("name2")
            .tags(List.of(new Tag("tag1")))
            .build();

        connectionDTO2 = connectionFacade.create(connectionDTO2, 1);

        Assertions.assertThat(connectionRepository.count())
            .isEqualTo(2);
        Assertions.assertThat(tagRepository.count())
            .isEqualTo(1);

        connectionFacade.delete(connectionDTO1.id());

        Assertions.assertThat(connectionRepository.count())
            .isEqualTo(1);

        connectionFacade.delete(connectionDTO2.id());

        Assertions.assertThat(connectionRepository.count())
            .isEqualTo(0);
        Assertions.assertThat(tagRepository.count())
            .isEqualTo(1);
    }

    @Test
    public void testGetConnection() {
        Connection connection = new Connection();

        connection.setComponentName("componentName");
        connection.setName("name");

        Tag tag1 = tagRepository.save(new Tag("tag1"));
        Tag tag2 = tagRepository.save(new Tag("tag2"));

        connection.setTags(List.of(tag1, tag2));

        connection = connectionRepository.save(connection);

        Assertions.assertThat(connectionFacade.getConnection(connection.getId()))
            .hasFieldOrPropertyWithValue("componentName", "componentName")
            .hasFieldOrPropertyWithValue("name", "name")
            .hasFieldOrPropertyWithValue("tags", List.of(tag1, tag2));
    }

    @Test
    public void testGetConnections() {
        Connection connection = new Connection();

        connection.setComponentName("componentName");
        connection.setName("name");

        Tag tag1 = tagRepository.save(new Tag("tag1"));
        Tag tag2 = tagRepository.save(new Tag("tag2"));

        connection.setTags(List.of(tag1, tag2));

        connection = connectionRepository.save(connection);

        List<ConnectionDTO> connectionDTOs = connectionFacade.getConnections(null, null, null, 1);

        Assertions.assertThat(
            CollectionUtils.map(connectionDTOs, ConnectionDTO::toConnection))
            .isEqualTo(List.of(connection));

        ConnectionDTO connectionDTO = connectionDTOs.get(0);

        Assertions.assertThat(connectionFacade.getConnection(connection.getId()))
            .isEqualTo(connectionDTO)
            .hasFieldOrPropertyWithValue("tags", List.of(tag1, tag2));
    }

    @Test
    public void testGetConnectionTags() {
        Connection connection = new Connection();

        Tag tag1 = tagRepository.save(new Tag("tag1"));
        Tag tag2 = tagRepository.save(new Tag("tag2"));

        connection.setComponentName("componentName");
        connection.setName("name");
        connection.setTags(List.of(tag1, tag2));

        connectionRepository.save(connection);

        Assertions.assertThat(connectionFacade.getConnectionTags(1)
            .stream()
            .map(Tag::getName)
            .collect(Collectors.toSet()))
            .contains("tag1", "tag2");

        connection = new Connection();

        connection.setComponentName("componentName");
        connection.setName("name2");

        tag1 = OptionalUtils.get(tagRepository.findById(Validate.notNull(tag1.getId(), "id")));

        connection.setTags(List.of(tag1, tagRepository.save(new Tag("tag3"))));

        connectionRepository.save(connection);

        Assertions.assertThat(connectionFacade.getConnectionTags(1)
            .stream()
            .map(Tag::getName)
            .collect(Collectors.toSet()))
            .contains("tag1", "tag2", "tag3");

        connectionRepository.deleteById(Validate.notNull(connection.getId(), "id"));

        Assertions.assertThat(connectionFacade.getConnectionTags(1)
            .stream()
            .map(Tag::getName)
            .collect(Collectors.toSet()))
            .contains("tag1", "tag2");
    }

    @Test
    public void testUpdate() {
        Tag tag1 = new Tag("tag1");

        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .componentName("componentName")
            .name("name")
            .tags(List.of(tag1, tagRepository.save(new Tag("tag2"))))
            .build();

        connectionDTO = connectionFacade.create(connectionDTO, 1);

        Assertions.assertThat(connectionDTO.tags())
            .hasSize(2);

        connectionDTO = ConnectionDTO.builder()
            .componentName("componentName")
            .id(connectionDTO.id())
            .name("name")
            .tags(List.of(tag1))
            .version(connectionDTO.version())
            .build();

        connectionDTO = connectionFacade.update(connectionDTO, 1);

        Assertions.assertThat(connectionDTO.tags())
            .hasSize(1);
    }

    @ComponentScan(basePackages = {
        "com.bytechef.hermes.configuration.instance.accessor", "com.bytechef.tag"
    })
    @TestConfiguration
    public static class ConnectionFacadeIntTestConfiguration {

        @MockBean
        ConnectionDefinitionFacade connectionDefinitionFacade;

        @MockBean
        ConnectionDefinitionService connectionDefinitionService;

        @MockBean
        private OAuth2Service oAuth2Service;

        @MockBean
        WorkflowService workflowService;

        @MockBean
        WorkflowConnectionFacade workflowConnectionFacade;
    }
}
