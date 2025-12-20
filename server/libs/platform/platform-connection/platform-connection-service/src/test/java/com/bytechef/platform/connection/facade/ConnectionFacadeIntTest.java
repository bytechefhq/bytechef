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

package com.bytechef.platform.connection.facade;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.Property;
import com.bytechef.platform.component.domain.ConnectionDefinition;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessor;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.config.ConnectionIntTestConfiguration;
import com.bytechef.platform.connection.config.ConnectionIntTestConfigurationSharedMocks;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.constant.ModeType;
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
@ConnectionIntTestConfigurationSharedMocks
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
        when(connectionDefinitionService.getConnectionConnectionDefinition(eq("componentName"), eq(1)))
            .thenReturn(new ConnectionDefinition(new MockConnectionDefinition(), "componentName", null, null));
    }

    @Test
    public void testCreate() {
        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .authorizationType(AuthorizationType.BASIC_AUTH)
            .componentName("componentName")
            .connectionVersion(1)
            .environmentId(Environment.STAGING.ordinal())
            .name("name1")
            .tags(List.of(new Tag("tag1")))
            .build();

        long connectionId = connectionFacade.create(connectionDTO, ModeType.AUTOMATION);

        Assertions.assertThat(connectionId)
            .isEqualTo(1055L);
    }

    @Test
    public void testDelete() {
        ConnectionDTO connectionDTO1 = ConnectionDTO.builder()
            .authorizationType(AuthorizationType.BASIC_AUTH)
            .componentName("componentName")
            .connectionVersion(1)
            .environmentId(Environment.STAGING.ordinal())
            .name("name1")
            .tags(List.of(new Tag("tag1")))
            .build();

        long connectionId1 = connectionFacade.create(connectionDTO1, ModeType.AUTOMATION);

        ConnectionDTO connectionDTO2 = ConnectionDTO.builder()
            .authorizationType(AuthorizationType.BASIC_AUTH)
            .componentName("componentName")
            .connectionVersion(1)
            .environmentId(Environment.STAGING.ordinal())
            .name("name2")
            .tags(List.of(new Tag("tag1")))
            .build();

        long connectionId2 = connectionFacade.create(connectionDTO2, ModeType.AUTOMATION);

        Assertions.assertThat(connectionRepository.count())
            .isEqualTo(2);
        Assertions.assertThat(tagRepository.count())
            .isEqualTo(1);

        connectionFacade.delete(connectionId1);

        Assertions.assertThat(connectionRepository.count())
            .isEqualTo(1);

        connectionFacade.delete(connectionId2);

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
        connection.setType(ModeType.AUTOMATION);

        Tag tag1 = tagRepository.save(new Tag("tag1"));
        Tag tag2 = tagRepository.save(new Tag("tag2"));

        connection.setTags(List.of(tag1, tag2));

        connection = connectionRepository.save(connection);

        when(connectionDefinitionService.executeBaseUri(eq("componentName"), any()))
            .thenReturn(Optional.of("baseUri"));

        Assertions.assertThat(connectionFacade.getConnection(connection.getId()))
            .hasFieldOrPropertyWithValue("baseUri", "baseUri")
            .hasFieldOrPropertyWithValue("componentName", "componentName")
            .hasFieldOrPropertyWithValue("name", "name")
            .hasFieldOrPropertyWithValue("tags", List.of(tag1, tag2));
    }

    @Test
    public void testGetConnectionWhenExecuteBaseUriThrowsException() {
        Connection connection = new Connection();

        connection.setComponentName("componentName");
        connection.setName("name");
        connection.setType(ModeType.AUTOMATION);

        Tag tag1 = tagRepository.save(new Tag("tag1"));
        Tag tag2 = tagRepository.save(new Tag("tag2"));

        connection.setTags(List.of(tag1, tag2));

        connection = connectionRepository.save(connection);

        when(connectionDefinitionService.executeBaseUri(eq("componentName"), any()))
            .thenThrow(new IllegalStateException("Connection failed"));

        ConnectionDTO result = connectionFacade.getConnection(connection.getId());

        Assertions.assertThat(result)
            .isNotNull()
            .hasFieldOrPropertyWithValue("baseUri", null)
            .hasFieldOrPropertyWithValue("componentName", "componentName")
            .hasFieldOrPropertyWithValue("name", "name")
            .hasFieldOrPropertyWithValue("tags", List.of(tag1, tag2));
    }

    @Test
    public void testGetConnections() {
        Connection connection = new Connection();

        connection.setComponentName("componentName");
        connection.setName("name");
        connection.setType(ModeType.AUTOMATION);

        Tag tag1 = tagRepository.save(new Tag("tag1"));
        Tag tag2 = tagRepository.save(new Tag("tag2"));

        connection.setTags(List.of(tag1, tag2));

        connection = connectionRepository.save(connection);

        List<ConnectionDTO> connectionDTOs = connectionFacade.getConnections(
            null, null, List.of(), null, null, ModeType.AUTOMATION);

        Assertions.assertThat(CollectionUtils.map(connectionDTOs, ConnectionDTO::toConnection))
            .isEqualTo(List.of(connection));

        ConnectionDTO connectionDTO = connectionDTOs.getFirst();

        Assertions.assertThat(connectionFacade.getConnection(connection.getId()))
            .isEqualTo(connectionDTO)
            .hasFieldOrPropertyWithValue("tags", List.of(tag1, tag2));

        when(connectionDefinitionService.getConnectionConnectionDefinition(eq("componentName2"), eq(1)))
            .thenThrow(new IllegalArgumentException("componentName2 not found"));

        Connection connection2 = new Connection();

        connection2.setComponentName("componentName2");
        connection2.setName("name");
        connection2.setType(ModeType.AUTOMATION);

        connectionRepository.save(connection2);

        connectionDTOs = connectionFacade.getConnections(null, null, List.of(), null, null, ModeType.AUTOMATION);

        Assertions.assertThat(CollectionUtils.map(connectionDTOs, ConnectionDTO::toConnection))
            .isEqualTo(List.of(connection));
    }

    @Test
    public void testGetConnectionTags() {
        Connection connection = new Connection();

        Tag tag1 = tagRepository.save(new Tag("tag1"));
        Tag tag2 = tagRepository.save(new Tag("tag2"));

        connection.setComponentName("componentName");
        connection.setName("name");
        connection.setTags(List.of(tag1, tag2));
        connection.setType(ModeType.AUTOMATION);

        connectionRepository.save(connection);

        Assertions.assertThat(connectionFacade.getConnectionTags(ModeType.AUTOMATION)
            .stream()
            .map(Tag::getName)
            .collect(Collectors.toSet()))
            .contains("tag1", "tag2");

        connection = new Connection();

        connection.setComponentName("componentName");
        connection.setName("name2");
        connection.setType(ModeType.AUTOMATION);

        tag1 = OptionalUtils.get(tagRepository.findById(Validate.notNull(tag1.getId(), "id")));

        connection.setTags(List.of(tag1, tagRepository.save(new Tag("tag3"))));

        connectionRepository.save(connection);

        Assertions.assertThat(connectionFacade.getConnectionTags(ModeType.AUTOMATION)
            .stream()
            .map(Tag::getName)
            .collect(Collectors.toSet()))
            .contains("tag1", "tag2", "tag3");

        connectionRepository.deleteById(Validate.notNull(connection.getId(), "id"));

        Assertions.assertThat(connectionFacade.getConnectionTags(ModeType.AUTOMATION)
            .stream()
            .map(Tag::getName)
            .collect(Collectors.toSet()))
            .contains("tag1", "tag2");
    }

    @Test
    public void testUpdate() {
        Tag tag1 = new Tag("tag1");

        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .authorizationType(AuthorizationType.BASIC_AUTH)
            .componentName("componentName")
            .connectionVersion(1)
            .environmentId(Environment.STAGING.ordinal())
            .name("name")
            .tags(List.of(tag1, tagRepository.save(new Tag("tag2"))))
            .build();

        connectionDTO = connectionFacade.getConnection(connectionFacade.create(connectionDTO, ModeType.AUTOMATION));

        Assertions.assertThat(connectionDTO.tags())
            .hasSize(2);

        connectionFacade.update(connectionDTO.id(), "name", List.of(tag1), connectionDTO.version());

        connectionDTO = connectionFacade.getConnection(connectionDTO.id());

        Assertions.assertThat(connectionDTO.tags())
            .hasSize(1);
    }

    @ComponentScan(basePackages = {
        "com.bytechef.platform.configuration.accessor", "com.bytechef.platform.tag"
    })
    @TestConfiguration
    public static class ConnectionFacadeIntTestConfiguration {

        @Bean
        JobPrincipalAccessor jobPrincipalAccessor() {
            return new JobPrincipalAccessor() {

                @Override
                public boolean isConnectionUsed(long connectionId) {
                    return false;
                }

                @Override
                public boolean isWorkflowEnabled(long jobPrincipalId, String workflowUuid) {
                    return false;
                }

                @Override
                public long getEnvironmentId(long jobPrincipalId) {
                    return 0;
                }

                @Override
                public Map<String, ?> getInputMap(long jobPrincipalId, String workflowUuid) {
                    return Map.of();
                }

                @Override
                public Map<String, ?> getMetadataMap(long jobPrincipalId) {
                    return Map.of();
                }

                @Override
                public ModeType getType() {
                    return ModeType.AUTOMATION;
                }

                @Override
                public String getWorkflowId(long jobPrincipalId, String workflowUuid) {
                    return "";
                }

                @Override
                public String getLastWorkflowId(String workflowUuid) {
                    return "";
                }

                @Override
                public String getWorkflowUuid(String workflowId) {
                    return "";
                }
            };
        }
    }

    private static class MockConnectionDefinition implements com.bytechef.component.definition.ConnectionDefinition {

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
