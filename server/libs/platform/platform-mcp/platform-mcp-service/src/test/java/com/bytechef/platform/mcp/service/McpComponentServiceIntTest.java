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

package com.bytechef.platform.mcp.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.mcp.config.PlatformMcpIntTestConfiguration;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpComponentRepository;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = PlatformMcpIntTestConfiguration.class)
public class McpComponentServiceIntTest {

    @Autowired
    private ConnectionRepository connectionRepository;

    @MockitoBean
    private MailService mailService;

    @Autowired
    private McpComponentService mcpComponentService;

    @Autowired
    private McpComponentRepository mcpComponentRepository;

    @Autowired
    private McpServerRepository mcpServerRepository;

    @MockitoBean
    private WorkflowService workflowService;

    private McpServer mcpServer;

    @BeforeEach
    public void beforeEach() {
        mcpServer = mcpServerRepository.save(
            new McpServer("test-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT));
    }

    @AfterEach
    public void afterEach() {
        mcpComponentRepository.deleteAll();
        mcpServerRepository.deleteAll();
        connectionRepository.deleteAll();
    }

    @Test
    public void testCreate() {
        McpComponent mcpComponent = getMcpComponent();

        mcpComponent = mcpComponentService.create(mcpComponent);

        assertThat(mcpComponent)
            .hasFieldOrPropertyWithValue("componentName", "test-component")
            .hasFieldOrPropertyWithValue("componentVersion", 1)
            .hasFieldOrPropertyWithValue("mcpServerId", mcpServer.getId())
            .hasFieldOrPropertyWithValue("connectionId", null);
        assertThat(mcpComponent.getId()).isNotNull();
    }

    @Test
    public void testUpdate() {
        McpComponent mcpComponent = mcpComponentRepository.save(getMcpComponent());

        Connection connection = new Connection();

        connection.setComponentName("test-connection-component");
        connection.setConnectionVersion(1);
        connection.setId(2L);
        connection.setName("test-connection");
        connection.setParameters(Map.of("param1", "value1", "param2", "value2"));

        connectionRepository.save(connection);

        mcpComponent.setConnectionId(2L);

        mcpComponent = mcpComponentService.update(mcpComponent);

        assertThat(mcpComponent)
            .hasFieldOrPropertyWithValue("connectionId", 2L)
            .hasFieldOrPropertyWithValue("mcpServerId", mcpServer.getId());
    }

    @Test
    public void testDelete() {
        McpComponent mcpComponent = mcpComponentRepository.save(getMcpComponent());

        mcpComponentService.delete(Validate.notNull(mcpComponent.getId(), "id"));

        assertThat(mcpComponentRepository.findById(mcpComponent.getId()))
            .isNotPresent();
    }

    @Test
    public void testGetMcpComponent() {
        McpComponent mcpComponent = mcpComponentRepository.save(getMcpComponent());

        McpComponent retrievedComponent = mcpComponentService.getMcpComponent(
            Validate.notNull(mcpComponent.getId(), "id"));

        assertThat(retrievedComponent).isEqualTo(mcpComponent);
    }

    @Test
    public void testGetMcpComponents() {
        McpComponent mcpComponent = mcpComponentRepository.save(getMcpComponent());

        List<McpComponent> components = mcpComponentService.getMcpComponents();

        assertThat(components).hasSize(1);
        assertThat(components.getFirst()).isEqualTo(mcpComponent);
    }

    @Test
    public void testGetMcpServerMcpComponents() {
        McpComponent component1 = mcpComponentRepository.save(getMcpComponent());
        McpComponent component2 = new McpComponent("test-component-2", 1, mcpServer.getId(), null);

        component2 = mcpComponentRepository.save(component2);

        McpServer anotherServer = mcpServerRepository.save(
            new McpServer("another-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT));

        McpComponent anotherComponent = new McpComponent("another-component", 1, anotherServer.getId(), null);

        mcpComponentRepository.save(anotherComponent);

        List<McpComponent> serverComponents = mcpComponentService.getMcpServerMcpComponents(mcpServer.getId());
        List<McpComponent> anotherServerComponents =
            mcpComponentService.getMcpServerMcpComponents(anotherServer.getId());

        assertThat(serverComponents).hasSize(2);
        assertThat(serverComponents).containsExactlyInAnyOrder(component1, component2);

        assertThat(anotherServerComponents).hasSize(1);

        McpComponent first = anotherServerComponents.getFirst();

        assertThat(first.getComponentName()).isEqualTo("another-component");

        assertThat(mcpComponentService.getMcpServerMcpComponents(Long.MAX_VALUE)).hasSize(0);
    }

    private McpComponent getMcpComponent() {
        return new McpComponent("test-component", 1, mcpServer.getId(), null);
    }
}
