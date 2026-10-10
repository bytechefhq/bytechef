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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.mcp.config.PlatformMcpIntTestConfiguration;
import com.bytechef.platform.mcp.config.PlatformMcpMethodSecurityTestConfiguration;
import com.bytechef.platform.mcp.config.PlatformMcpMethodSecurityTestConfiguration.TenantAdminCheck;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import javax.sql.DataSource;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = PlatformMcpIntTestConfiguration.class)
class McpServerServiceIntTest {

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
    void afterEach() {
        mcpServerRepository.deleteAll();
    }

    @Test
    void testCreate() {
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
    void testCreateWithParameters() {
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
    void testUpdate() {
        McpServer mcpServer = mcpServerRepository.save(getMcpServer());

        mcpServer.setName("updated-server");
        mcpServer.setEnabled(false);

        mcpServer = mcpServerService.update(mcpServer);

        assertThat(mcpServer)
            .hasFieldOrPropertyWithValue("name", "updated-server")
            .hasFieldOrPropertyWithValue("enabled", false);
    }

    @Test
    void testUpdateWithParameters() {
        McpServer mcpServer = mcpServerRepository.save(getMcpServer());

        mcpServer = mcpServerService.update(mcpServer.getId(), "updated-server", false);

        assertThat(mcpServer)
            .hasFieldOrPropertyWithValue("name", "updated-server")
            .hasFieldOrPropertyWithValue("enabled", false);
    }

    @Test
    void testDelete() {
        McpServer mcpServer = mcpServerRepository.save(getMcpServer());

        mcpServerService.delete(Validate.notNull(mcpServer.getId(), "id"));

        assertThat(mcpServerRepository.findById(mcpServer.getId()))
            .isNotPresent();
    }

    @Test
    void testGetMcpServer() {
        McpServer mcpServer = mcpServerRepository.save(getMcpServer());

        McpServer retrievedMcpServer = mcpServerService.getMcpServer(Validate.notNull(mcpServer.getId(), "id"));

        assertThat(retrievedMcpServer).isEqualTo(mcpServer);
    }

    @Test
    void testGetMcpServersByType() {
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
    void testNewMcpServerDefaultsAuthenticationRequiredTrue() {
        McpServer mcpServer = mcpServerService.create(
            "auth-default", PlatformType.AUTOMATION, Environment.PRODUCTION, true);

        McpServer loaded = mcpServerService.getMcpServer(mcpServer.getSecretKey());

        assertThat(loaded.isAuthenticationRequired()).isTrue();
    }

    @Test
    void testLegacyRowLoadsAuthenticationRequiredFalse() {
        Timestamp now = Timestamp.from(Instant.now());

        new JdbcTemplate(dataSource).update(
            "INSERT INTO mcp_server (name, type, environment, enabled, secret_key, created_date, created_by, " +
                "last_modified_date, last_modified_by, version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            "auth-legacy", PlatformType.AUTOMATION.ordinal(), Environment.PRODUCTION.ordinal(), true,
            "legacy-secret-key", now, "system", now, "system", 0);

        McpServer loaded = mcpServerService.getMcpServer("legacy-secret-key");

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

    @Test
    void testUpdateWithFlagsAppliesAllFieldsTogether() {
        McpServer mcpServer = mcpServerService.create(
            "flags-update", PlatformType.AUTOMATION, Environment.PRODUCTION, true);

        mcpServerService.update(mcpServer.getId(), "flags-renamed", false, true, true);

        McpServer loaded = mcpServerService.getMcpServer(mcpServer.getSecretKey());

        assertThat(loaded.getName()).isEqualTo("flags-renamed");
        assertThat(loaded.isEnabled()).isFalse();
        assertThat(loaded.isEnforceToolAuthorization()).isTrue();
        assertThat(loaded.isAuthenticationRequired()).isTrue();
    }

    @Test
    void testUpdateWithFlagsRejectsInvalidCombinationWithoutPersistingAnyField() {
        McpServer mcpServer = mcpServerService.create(
            "flags-invariant", PlatformType.AUTOMATION, Environment.PRODUCTION, true);

        assertThatThrownBy(() -> mcpServerService.update(mcpServer.getId(), "flags-renamed", false, true, false))
            .isInstanceOf(IllegalArgumentException.class);

        McpServer loaded = mcpServerService.getMcpServer(mcpServer.getSecretKey());

        assertThat(loaded.getName()).isEqualTo("flags-invariant");
        assertThat(loaded.isEnabled()).isTrue();
        assertThat(loaded.isEnforceToolAuthorization()).isFalse();
        assertThat(loaded.isAuthenticationRequired()).isTrue();
    }

    @Test
    void testRotateSecretKeyInvalidatesPreviousSecretKey() {
        McpServer mcpServer = mcpServerService.create(
            "rotate", PlatformType.AUTOMATION, Environment.PRODUCTION, true);

        String previousSecretKey = mcpServer.getSecretKey();

        McpServer rotatedMcpServer = mcpServerService.rotateSecretKey(Validate.notNull(mcpServer.getId(), "id"));

        String rotatedSecretKey = rotatedMcpServer.getSecretKey();

        assertThat(rotatedSecretKey).isNotEqualTo(previousSecretKey);
        assertThat(mcpServerService.getMcpServer(rotatedSecretKey)
            .getId()).isEqualTo(mcpServer.getId());
        assertThatThrownBy(() -> mcpServerService.getMcpServer(previousSecretKey))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private McpServer getMcpServer() {
        return new McpServer("test-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT);
    }

    @Nested
    @Import(PlatformMcpMethodSecurityTestConfiguration.class)
    class MethodSecurity {

        @Autowired
        private PermissionEvaluator permissionEvaluator;

        @Autowired
        private TenantAdminCheck tenantAdminCheck;

        @BeforeEach
        void setAuthentication() {
            SecurityContextHolder.getContext()
                .setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                        "viewer", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        }

        @AfterEach
        void clearAuthentication() {
            SecurityContextHolder.clearContext();

            reset(permissionEvaluator, tenantAdminCheck);
        }

        @Test
        void testGetMcpServerRequiresViewer() {
            assertThatThrownBy(() -> mcpServerService.getMcpServer(7L))
                .isInstanceOf(AccessDeniedException.class);

            verify(permissionEvaluator).hasPermission(any(), eq(7L), eq("McpServer"), eq("MCP_VIEW"));
        }

        @Test
        void testGetSecretKeyRequiresTenantAdmin() {
            assertThatThrownBy(() -> mcpServerService.getMcpServerSecretKey(7L))
                .isInstanceOf(AccessDeniedException.class);

            verify(tenantAdminCheck).isTenantAdmin();
        }

        @Test
        void testRotateSecretKeyRequiresTenantAdmin() {
            assertThatThrownBy(() -> mcpServerService.rotateSecretKey(7L))
                .isInstanceOf(AccessDeniedException.class);

            verify(tenantAdminCheck).isTenantAdmin();
        }

        @Test
        void testUpdateRequiresEditor() {
            assertThatThrownBy(() -> mcpServerService.update(7L, "name", true))
                .isInstanceOf(AccessDeniedException.class);

            verify(permissionEvaluator).hasPermission(any(), eq(7L), eq("McpServer"), eq("MCP_EDIT"));
        }
    }
}
