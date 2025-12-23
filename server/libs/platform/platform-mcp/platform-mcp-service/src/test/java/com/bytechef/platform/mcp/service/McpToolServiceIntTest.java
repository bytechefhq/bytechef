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
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.mcp.config.PlatformMcpIntTestConfiguration;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.repository.McpComponentRepository;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.platform.mcp.repository.McpToolRepository;
import java.util.Map;
import java.util.Optional;
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
public class McpToolServiceIntTest {

    @MockitoBean
    private MailService mailService;

    @Autowired
    private McpToolService mcpToolService;

    @Autowired
    private McpToolRepository mcpToolRepository;

    @Autowired
    private McpComponentRepository mcpComponentRepository;

    @Autowired
    private McpServerRepository mcpServerRepository;

    @MockitoBean
    private WorkflowService workflowService;

    private McpComponent mcpComponent;

    @BeforeEach
    public void beforeEach() {
        McpServer mcpServer = mcpServerRepository.save(
            new McpServer("test-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT));

        mcpComponent = mcpComponentRepository.save(new McpComponent("test-component", 1, mcpServer.getId(), null));
    }

    @AfterEach
    public void afterEach() {
        mcpToolRepository.deleteAll();
        mcpComponentRepository.deleteAll();
        mcpServerRepository.deleteAll();
    }

    @Test
    public void testCreate() {
        McpTool mcpTool = getMcpTool();

        mcpTool = mcpToolService.create(mcpTool);

        assertThat(mcpTool)
            .hasFieldOrPropertyWithValue("name", "test-tool")
            .hasFieldOrPropertyWithValue("mcpComponentId", mcpComponent.getId());
        assertThat(mcpTool.getId()).isNotNull();
        assertThat(mcpTool.getParameters()).isEqualTo(Map.of("param1", "value1"));
    }

    @Test
    public void testUpdate() {
        McpTool mcpTool = mcpToolRepository.save(getMcpTool());

        mcpTool.setName("updated-tool");

        mcpTool = mcpToolService.update(mcpTool);

        assertThat(mcpTool)
            .hasFieldOrPropertyWithValue("name", "updated-tool")
            .hasFieldOrPropertyWithValue("mcpComponentId", mcpComponent.getId());
    }

    @Test
    public void testDelete() {
        McpTool mcpTool = mcpToolRepository.save(getMcpTool());

        mcpToolService.delete(Validate.notNull(mcpTool, "mcpTool"));

        assertThat(mcpToolRepository.findById(mcpTool.getId()))
            .isNotPresent();
    }

    @Test
    public void testFetchMcpTool() {
        McpTool mcpTool = mcpToolRepository.save(getMcpTool());

        Optional<McpTool> fetchedTool = mcpToolService.fetchMcpTool(Validate.notNull(mcpTool.getId(), "id"));

        assertThat(fetchedTool).isPresent();
        assertThat(fetchedTool.get()).isEqualTo(mcpTool);
    }

    @Test
    public void testGetMcpTools() {
        McpTool mcpTool = mcpToolRepository.save(getMcpTool());

        assertThat(mcpToolService.getMcpTools()).hasSize(1);
        assertThat(mcpToolService.getMcpTools()
            .getFirst()).isEqualTo(mcpTool);
    }

    @Test
    public void testGetMcpComponentMcpTools() {
        McpTool mcpTool = mcpToolRepository.save(getMcpTool());

        assertThat(mcpToolService.getMcpComponentMcpTools(mcpComponent.getId())).hasSize(1);
        assertThat(mcpToolService.getMcpComponentMcpTools(mcpComponent.getId())
            .getFirst()).isEqualTo(mcpTool);

        assertThat(mcpToolService.getMcpComponentMcpTools(Long.MAX_VALUE)).hasSize(0);
    }

    private McpTool getMcpTool() {
        return new McpTool("test-tool", Map.of("param1", "value1"), mcpComponent.getId());
    }
}
