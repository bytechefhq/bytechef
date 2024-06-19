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

package com.bytechef.platform.connection.facade;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Property;
import com.bytechef.platform.component.registry.domain.ConnectionDefinition;
import com.bytechef.platform.component.registry.facade.ConnectionDefinitionFacade;
import com.bytechef.platform.component.registry.service.ConnectionDefinitionService;
import com.bytechef.platform.configuration.facade.WorkflowConnectionFacade;
import com.bytechef.platform.configuration.instance.accessor.InstanceAccessor;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.config.ConnectionIntTestConfiguration;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.constant.AppType;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.repository.TagRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
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
    private ConnectionDefinitionService connectionDefinitionService;

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

    @BeforeEach
    public void beforeEach() {
        when(connectionDefinitionService.getConnectionDefinition(anyString(), anyInt()))
            .thenReturn(new ConnectionDefinition(new MockConnectionDefinition(), "componentName", null, null));
    }

    @Test
    public void testCreate() {
        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .componentName("componentName")
            .name("name1")
            .tags(List.of(new Tag("tag1")))
            .build();

        connectionDTO = connectionFacade.create(connectionDTO, AppType.AUTOMATION);

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

        connectionDTO1 = connectionFacade.create(connectionDTO1, AppType.AUTOMATION);

        ConnectionDTO connectionDTO2 = ConnectionDTO.builder()
            .componentName("componentName")
            .name("name2")
            .tags(List.of(new Tag("tag1")))
            .build();

        connectionDTO2 = connectionFacade.create(connectionDTO2, AppType.AUTOMATION);

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
        connection.setType(AppType.AUTOMATION);

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
        connection.setType(AppType.AUTOMATION);

        Tag tag1 = tagRepository.save(new Tag("tag1"));
        Tag tag2 = tagRepository.save(new Tag("tag2"));

        connection.setTags(List.of(tag1, tag2));

        connection = connectionRepository.save(connection);

        List<ConnectionDTO> connectionDTOs =
            connectionFacade.getConnections(null, null, null, null, AppType.AUTOMATION);

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
        connection.setType(AppType.AUTOMATION);

        connectionRepository.save(connection);

        Assertions.assertThat(connectionFacade.getConnectionTags(AppType.AUTOMATION)
            .stream()
            .map(Tag::getName)
            .collect(Collectors.toSet()))
            .contains("tag1", "tag2");

        connection = new Connection();

        connection.setComponentName("componentName");
        connection.setName("name2");
        connection.setType(AppType.AUTOMATION);

        tag1 = OptionalUtils.get(tagRepository.findById(Validate.notNull(tag1.getId(), "id")));

        connection.setTags(List.of(tag1, tagRepository.save(new Tag("tag3"))));

        connectionRepository.save(connection);

        Assertions.assertThat(connectionFacade.getConnectionTags(AppType.AUTOMATION)
            .stream()
            .map(Tag::getName)
            .collect(Collectors.toSet()))
            .contains("tag1", "tag2", "tag3");

        connectionRepository.deleteById(Validate.notNull(connection.getId(), "id"));

        Assertions.assertThat(connectionFacade.getConnectionTags(AppType.AUTOMATION)
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

        connectionDTO = connectionFacade.create(connectionDTO, AppType.AUTOMATION);

        Assertions.assertThat(connectionDTO.tags())
            .hasSize(2);

        connectionDTO = ConnectionDTO.builder()
            .componentName("componentName")
            .id(connectionDTO.id())
            .name("name")
            .tags(List.of(tag1))
            .version(connectionDTO.version())
            .build();

        connectionDTO = connectionFacade.update(connectionDTO);

        Assertions.assertThat(connectionDTO.tags())
            .hasSize(1);
    }

    @ComponentScan(basePackages = {
        "com.bytechef.platform.configuration.instance.accessor", "com.bytechef.platform.tag"
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

        @MockBean
        WorkflowTestConfigurationService workflowTestConfigurationService;

        @Bean
        InstanceAccessor instanceAccessor() {
            return new InstanceAccessor() {

                @Override
                public boolean isConnectionUsed(long connectionId) {
                    return false;
                }

                @Override
                public boolean isWorkflowEnabled(long instanceId, String workflowReferenceCode) {
                    return false;
                }

                @Override
                public Map<String, ?> getInputMap(long instanceId, String workflowReferenceCode) {
                    return Map.of();
                }

                @Override
                public AppType getType() {
                    return AppType.AUTOMATION;
                }

                @Override
                public String getWorkflowId(long instanceId, String workflowReferenceCode) {
                    return "";
                }
            };
        }
    }

    private static class MockConnectionDefinition implements com.bytechef.component.definition.ConnectionDefinition {
        @Override
        public boolean containsAuthorizations() {
            return false;
        }

        @Override
        public Authorization getAuthorization(String authorizationName) {
            return null;
        }

        @Override
        public Optional<Boolean> getAuthorizationRequired() {
            return Optional.empty();
        }

        @Override
        public Optional<List<? extends Authorization>> getAuthorizations() {
            return Optional.empty();
        }

        @Override
        public Optional<BaseUriFunction> getBaseUri() {
            return Optional.empty();
        }

        @Override
        public Optional<List<? extends Property>> getProperties() {
            return Optional.empty();
        }

        @Override
        public Optional<TestConsumer> getTest() {
            return Optional.empty();
        }

        @Override
        public int getVersion() {
            return 0;
        }
    }
}
