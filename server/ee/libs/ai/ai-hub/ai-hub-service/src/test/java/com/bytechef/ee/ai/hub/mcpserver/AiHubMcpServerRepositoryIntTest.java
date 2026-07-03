/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.mcpserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.ai.hub.mcpserver.repository.AiHubMcpServerRepository;
import com.bytechef.ee.ai.hub.mcpserver.repository.AiHubMcpServerToolRepository;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link AiHubMcpServerRepository} and {@link AiHubMcpServerToolRepository} against a real
 * Postgres (Liquibase-managed schema). Verifies the entity mapping, the per-user/workspace scoping, the per-tool unique
 * constraint, and the FK cascade from a server to its tools.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AiHubMcpServerRepositoryIntTest.IntTestConfiguration.class)
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
public class AiHubMcpServerRepositoryIntTest {

    @Autowired
    private AiHubMcpServerRepository mcpServerRepository;

    @Autowired
    private AiHubMcpServerToolRepository mcpServerToolRepository;

    @AfterEach
    public void afterEach() {
        mcpServerToolRepository.deleteAll();
        mcpServerRepository.deleteAll();
    }

    @Test
    public void testSaveAndFindServer() {
        AiHubMcpServer mcpServer = mcpServerRepository.save(
            new AiHubMcpServer(10L, 1L, 0, "My server", "https://example.com/mcp", "encrypted-token"));

        AiHubMcpServer found = mcpServerRepository.findById(mcpServer.getId())
            .orElseThrow();

        assertThat(found.getName()).isEqualTo("My server");
        assertThat(found.getUrl()).isEqualTo("https://example.com/mcp");
        // The token is stored verbatim (already encrypted by the facade) — the repository doesn't transform it.
        assertThat(found.getAuthToken()).isEqualTo("encrypted-token");
        assertThat(found.getUserId()).isEqualTo(10L);
        assertThat(found.getWorkspaceId()).isEqualTo(1L);
        assertThat(found.isEnabled()).isTrue();
    }

    @Test
    public void testFindAllByUserIdAndWorkspaceIdScopesByUserAndWorkspace() {
        mcpServerRepository.save(new AiHubMcpServer(10L, 1L, 0, "a", "https://a.example.com/mcp", null));
        mcpServerRepository.save(new AiHubMcpServer(10L, 1L, 0, "b", "https://b.example.com/mcp", null));
        mcpServerRepository.save(new AiHubMcpServer(20L, 1L, 0, "other-user", "https://c.example.com/mcp", null));
        mcpServerRepository.save(new AiHubMcpServer(10L, 2L, 0, "other-workspace", "https://d.example.com/mcp", null));

        List<AiHubMcpServer> servers = mcpServerRepository.findAllByUserIdAndWorkspaceId(10L, 1L);

        assertThat(servers)
            .extracting(AiHubMcpServer::getName)
            .containsExactlyInAnyOrder("a", "b");
    }

    @Test
    public void testToolSaveAndLookup() {
        long serverId = mcpServerRepository.save(
            new AiHubMcpServer(10L, 1L, 0, "s", "https://s.example.com/mcp", null))
            .getId();

        mcpServerToolRepository.save(new AiHubMcpServerTool(serverId, "search", false));

        Optional<AiHubMcpServerTool> found = mcpServerToolRepository.findByMcpServerIdAndName(serverId, "search");

        assertThat(found).isPresent();
        assertThat(found.get()
            .isEnabled()).isFalse();
        assertThat(mcpServerToolRepository.findAllByMcpServerId(serverId)).hasSize(1);
    }

    @Test
    public void testToolUniqueConstraintPerServerAndName() {
        long serverId = mcpServerRepository.save(
            new AiHubMcpServer(10L, 1L, 0, "s", "https://s.example.com/mcp", null))
            .getId();

        mcpServerToolRepository.save(new AiHubMcpServerTool(serverId, "duplicate", false));

        assertThatThrownBy(() -> mcpServerToolRepository.save(new AiHubMcpServerTool(serverId, "duplicate", true)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void testDeletingServerCascadesToTools() {
        long serverId = mcpServerRepository.save(
            new AiHubMcpServer(10L, 1L, 0, "s", "https://s.example.com/mcp", null))
            .getId();

        mcpServerToolRepository.save(new AiHubMcpServerTool(serverId, "t1", false));
        mcpServerToolRepository.save(new AiHubMcpServerTool(serverId, "t2", false));

        assertThat(mcpServerToolRepository.findAllByMcpServerId(serverId)).hasSize(2);

        mcpServerRepository.deleteById(serverId);

        // The FK is ON DELETE CASCADE, so removing a server removes its per-tool rows.
        assertThat(mcpServerToolRepository.findAllByMcpServerId(serverId)).isEmpty();
    }

    @EnableAutoConfiguration
    @Import({
        LiquibaseConfiguration.class,
        AiHubMcpServerRepositoryIntTest.IntTestConfiguration.IntTestJdbcConfiguration.class
    })
    @Configuration
    public static class IntTestConfiguration {

        @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
        @Configuration
        public static class IntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
        }
    }
}
