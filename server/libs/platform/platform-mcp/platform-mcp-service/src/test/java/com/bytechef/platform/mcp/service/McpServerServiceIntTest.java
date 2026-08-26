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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.mcp.config.PlatformMcpIntTestConfiguration;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = PlatformMcpIntTestConfiguration.class)
public class McpServerServiceIntTest {

    @Autowired
    private DataSource dataSource;

    @MockitoBean
    private MailService mailService;

    @Autowired
    private McpServerService mcpServerService;

    @Autowired
    private McpServerRepository mcpServerRepository;

    @MockitoBean
    private WorkflowService workflowService;

    @AfterEach
    public void afterEach() {
        mcpServerRepository.deleteAll();
    }

    @Test
    public void testCreate() {
        McpServer mcpServer = getMcpServer();

        mcpServer = mcpServerService.create(mcpServer);

        assertThat(mcpServer)
            .hasFieldOrPropertyWithValue("name", "test-server")
            .hasFieldOrPropertyWithValue("type", PlatformType.AUTOMATION)
            .hasFieldOrPropertyWithValue("environment", Environment.DEVELOPMENT)
            .hasFieldOrPropertyWithValue("enabled", true);
        assertThat(mcpServer.getId()).isNotNull();
    }

    @Test
    public void testCreateWithParameters() {
        McpServer mcpServer = mcpServerService.create(
            "test-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT, false);

        assertThat(mcpServer)
            .hasFieldOrPropertyWithValue("name", "test-server")
            .hasFieldOrPropertyWithValue("type", PlatformType.AUTOMATION)
            .hasFieldOrPropertyWithValue("environment", Environment.DEVELOPMENT)
            .hasFieldOrPropertyWithValue("enabled", false);
        assertThat(mcpServer.getId()).isNotNull();
    }

    @Test
    public void testUpdate() {
        McpServer mcpServer = mcpServerRepository.save(getMcpServer());

        mcpServer.setName("updated-server");
        mcpServer.setEnabled(false);

        mcpServer = mcpServerService.update(mcpServer);

        assertThat(mcpServer)
            .hasFieldOrPropertyWithValue("name", "updated-server")
            .hasFieldOrPropertyWithValue("enabled", false);
    }

    @Test
    public void testUpdateWithParameters() {
        McpServer mcpServer = mcpServerRepository.save(getMcpServer());

        mcpServer = mcpServerService.update(mcpServer.getId(), "updated-server", false);

        assertThat(mcpServer)
            .hasFieldOrPropertyWithValue("name", "updated-server")
            .hasFieldOrPropertyWithValue("enabled", false);
    }

    @Test
    public void testDelete() {
        McpServer mcpServer = mcpServerRepository.save(getMcpServer());

        mcpServerService.delete(Validate.notNull(mcpServer.getId(), "id"));

        assertThat(mcpServerRepository.findById(mcpServer.getId()))
            .isNotPresent();
    }

    @Test
    public void testGetMcpServer() {
        McpServer mcpServer = mcpServerRepository.save(getMcpServer());

        McpServer retrievedMcpServer = mcpServerService.getMcpServer(Validate.notNull(mcpServer.getId(), "id"));

        assertThat(retrievedMcpServer).isEqualTo(mcpServer);
    }

    @Test
    public void testGetMcpServersByType() {
        McpServer automationServer = mcpServerRepository.save(getMcpServer());
        McpServer embeddedServer =
            mcpServerRepository.save(new McpServer("embedded-server", PlatformType.EMBEDDED, Environment.DEVELOPMENT));

        List<McpServer> automationServers = mcpServerService.getMcpServers(PlatformType.AUTOMATION);
        List<McpServer> embeddedServers = mcpServerService.getMcpServers(PlatformType.EMBEDDED);

        assertThat(automationServers).hasSize(1)
            .contains(automationServer);
        assertThat(embeddedServers).hasSize(1)
            .contains(embeddedServer);
    }

    @Test
    public void testGetMcpServersByTypeWithOrderBy() {
        McpServer server1 = mcpServerRepository.save(
            new McpServer("a-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT));
        McpServer server2 = mcpServerRepository.save(
            new McpServer("z-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT));

        List<McpServer> serversAsc =
            mcpServerService.getMcpServers(PlatformType.AUTOMATION, McpServerService.McpServerOrderBy.NAME_ASC);
        List<McpServer> serversDesc =
            mcpServerService.getMcpServers(PlatformType.AUTOMATION, McpServerService.McpServerOrderBy.NAME_DESC);

        assertThat(serversAsc).hasSize(2);
        assertThat(serversAsc.get(0)).isEqualTo(server1);
        assertThat(serversAsc.get(1)).isEqualTo(server2);

        assertThat(serversDesc).hasSize(2);
        assertThat(serversDesc.get(0)).isEqualTo(server2);
        assertThat(serversDesc.get(1)).isEqualTo(server1);
    }

    @Test
    void testNewMcpServerDefaultsAuthenticationRequiredTrue() {
        McpServer mcpServer = mcpServerService.create(
            "auth-default", PlatformType.AUTOMATION, Environment.PRODUCTION, true);

        McpServer loaded = mcpServerService.getMcpServer(mcpServer.getSecretKey());

        assertThat(loaded.isAuthenticationRequired()).isTrue();
    }

    @Test
    void testLegacyRowLoadsAuthenticationRequiredFalse() {
        McpServer mcpServer = mcpServerService.create(
            "auth-legacy", PlatformType.AUTOMATION, Environment.PRODUCTION, true);

        new JdbcTemplate(dataSource).update(
            "UPDATE mcp_server SET authentication_required = false WHERE id = ?", mcpServer.getId());

        McpServer loaded = mcpServerService.getMcpServer(mcpServer.getSecretKey());

        assertThat(loaded.isAuthenticationRequired()).isFalse();
    }

    @Test
    void testUpdatePersistsAuthenticationRequired() {
        McpServer mcpServer = mcpServerService.create(
            "auth-update", PlatformType.AUTOMATION, Environment.PRODUCTION, true);

        mcpServer.setAuthenticationRequired(false);

        mcpServerService.update(mcpServer);

        McpServer loaded = mcpServerService.getMcpServer(mcpServer.getSecretKey());

        assertThat(loaded.isAuthenticationRequired()).isFalse();
    }

    @Test
    void testUpdatePersistsEnforceToolAuthorization() {
        McpServer mcpServer = mcpServerService.create(
            "auth-enforce-persist", PlatformType.AUTOMATION, Environment.PRODUCTION, true);

        mcpServer.setAuthenticationRequired(true);
        mcpServer.setEnforceToolAuthorization(true);

        mcpServerService.update(mcpServer);

        McpServer loaded = mcpServerService.getMcpServer(mcpServer.getSecretKey());

        assertThat(loaded.isEnforceToolAuthorization()).isTrue();
    }

    @Test
    void testUpdateRejectsNoAuthWithToolAuthorization() {
        McpServer mcpServer = mcpServerService.create(
            "auth-invariant", PlatformType.AUTOMATION, Environment.PRODUCTION, true);

        mcpServer.setAuthenticationRequired(false);
        mcpServer.setEnforceToolAuthorization(true);

        assertThatThrownBy(() -> mcpServerService.update(mcpServer))
            .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Proves the derived queries behind {@code existsByNameAndEnvironment} actually derive, and that the constraint
     * they mirror is per-environment: the same name in another environment is not a conflict.
     */
    @Test
    void testExistsByNameAndEnvironment() {
        mcpServerRepository.save(new McpServer("taken", PlatformType.AUTOMATION, Environment.STAGING));

        assertThat(mcpServerService.existsByNameAndEnvironment("taken", Environment.STAGING, null)).isTrue();
        assertThat(mcpServerService.existsByNameAndEnvironment("taken", Environment.PRODUCTION, null)).isFalse();
        assertThat(mcpServerService.existsByNameAndEnvironment("free", Environment.STAGING, null)).isFalse();
    }

    @Test
    void testExistsByNameAndEnvironmentExcludesTheGivenLineage() {
        McpServer mcpServer = mcpServerRepository.save(
            new McpServer("taken", PlatformType.AUTOMATION, Environment.STAGING));

        assertThat(mcpServerService.existsByNameAndEnvironment("taken", Environment.STAGING, mcpServer.getUuid()))
            .isFalse();
        assertThat(mcpServerService.existsByNameAndEnvironment("taken", Environment.STAGING, UUID.randomUUID()))
            .isTrue();
    }

    /**
     * The constraint the check mirrors spans platform types, so an EMBEDDED server blocks an AUTOMATION name.
     */
    @Test
    void testExistsByNameAndEnvironmentSpansPlatformTypes() {
        mcpServerRepository.save(new McpServer("taken", PlatformType.EMBEDDED, Environment.STAGING));

        assertThat(mcpServerService.existsByNameAndEnvironment("taken", Environment.STAGING, null)).isTrue();
    }

    private McpServer getMcpServer() {
        return new McpServer("test-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT);
    }
}
