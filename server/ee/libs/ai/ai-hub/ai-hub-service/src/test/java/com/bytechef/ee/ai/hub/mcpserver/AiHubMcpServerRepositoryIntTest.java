/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.mcpserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.ai.hub.config.AiHubIntTestConfiguration;
import com.bytechef.ee.ai.hub.mcpserver.repository.AiHubMcpServerRepository;
import com.bytechef.ee.ai.hub.mcpserver.repository.AiHubMcpServerToolRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * @version ee
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = AiHubIntTestConfiguration.class,
    properties = {
        "bytechef.edition=ee", "spring.liquibase.change-log=classpath:config/liquibase/ai-hub-execution-testint.xml",
        "spring.liquibase.contexts=configuration"
    })
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

        assertThat(mcpServerToolRepository.findAllByMcpServerId(serverId)).isEmpty();
    }
}
