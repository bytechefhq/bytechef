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

package com.bytechef.platform.mcp.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.audit.aspect.AuditAspect;
import com.bytechef.ee.platform.audit.aspect.AuditCaptureAspect;
import com.bytechef.ee.platform.audit.aspect.AuditMapperResolver;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.mcp.config.PlatformMcpIntTestConfiguration;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.facade.McpServerFacade;
import com.bytechef.platform.mcp.facade.McpServerFacadeImpl;
import com.bytechef.platform.mcp.repository.McpComponentRepository;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.platform.mcp.service.McpServerService;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        PlatformMcpIntTestConfiguration.class, McpServerAuditIntTest.AuditConfiguration.class
    },
    properties = "bytechef.edition=ee")
@MockitoBean(types = {
    MailService.class, WorkflowService.class
})
class McpServerAuditIntTest {

    private static final String ADMIN_LOGIN = "admin";

    @Autowired
    private McpComponentRepository mcpComponentRepository;

    @Autowired
    private McpServerFacade mcpServerFacade;

    @Autowired
    private McpServerRepository mcpServerRepository;

    @Autowired
    private McpServerService mcpServerService;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @BeforeEach
    void beforeEach() {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(
                ADMIN_LOGIN, "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        persistenceAuditEventRepository.deleteAll();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        mcpComponentRepository.deleteAll();
        mcpServerRepository.deleteAll();
        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    void testCreateRecordsTheServerAndThePlatformOfTheResult() {
        McpServer mcpServer = mcpServerService.create(
            new McpServer(uniqueName("automation"), PlatformType.AUTOMATION, Environment.DEVELOPMENT));

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(McpServerAuditEvents.MCP_SERVER_CREATED);

        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo(ADMIN_LOGIN);
        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("mcpServerId", String.valueOf(mcpServer.getId()))
            .containsEntry("name", mcpServer.getName())
            .containsEntry("type", PlatformType.AUTOMATION.name())
            .containsEntry("environment", Environment.DEVELOPMENT.name())
            .doesNotContainKey("secretKey")
            .doesNotContainValue(mcpServer.getSecretKey());
    }

    @Test
    void testCreateFromAttributesRecordsOneEventWithThePlatformOfTheResult() {
        McpServer mcpServer = mcpServerService.create(
            uniqueName("embedded"), PlatformType.EMBEDDED, Environment.PRODUCTION, false);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(McpServerAuditEvents.MCP_SERVER_CREATED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.EMBEDDED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("mcpServerId", String.valueOf(mcpServer.getId()))
            .containsEntry("name", mcpServer.getName())
            .containsEntry("type", PlatformType.EMBEDDED.name())
            .containsEntry("environment", Environment.PRODUCTION.name())
            .doesNotContainKey("secretKey")
            .doesNotContainValue(mcpServer.getSecretKey());
    }

    @Test
    void testCreateThatIsRejectedRecordsAnError() {
        McpServer mcpServer = new McpServer(uniqueName("rejected"), PlatformType.AUTOMATION, Environment.DEVELOPMENT);

        mcpServer.setAuthenticationRequired(false);
        mcpServer.setEnforceToolAuthorization(true);

        assertThatThrownBy(() -> mcpServerService.create(mcpServer))
            .isInstanceOf(IllegalArgumentException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(McpServerAuditEvents.MCP_SERVER_CREATED);

        assertThat(persistentAuditEvent.getPlatform()).isNull();
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", IllegalArgumentException.class.getName())
            .doesNotContainKey("mcpServerId")
            .doesNotContainKey("name")
            .doesNotContainValue(mcpServer.getSecretKey());
    }

    @Test
    void testUpdateOfTheServerRecordsTheNewNameAndEnablement() {
        McpServer mcpServer = saveMcpServer(PlatformType.AUTOMATION);

        mcpServer.setName(uniqueName("renamed"));
        mcpServer.setEnabled(false);

        McpServer updatedMcpServer = mcpServerService.update(mcpServer);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(McpServerAuditEvents.MCP_SERVER_UPDATED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("mcpServerId", String.valueOf(mcpServer.getId()))
            .containsEntry("name", updatedMcpServer.getName())
            .containsEntry("enabled", "false")
            .doesNotContainKey("authenticationRequired")
            .doesNotContainKey("enforceToolAuthorization")
            .doesNotContainKey("secretKey")
            .doesNotContainValue(mcpServer.getSecretKey());
    }

    @Test
    void testUpdateByIdRecordsOneEventWithoutTheAuthorizationFlags() {
        McpServer mcpServer = saveMcpServer(PlatformType.EMBEDDED);

        String name = uniqueName("renamed");

        mcpServerService.update(mcpServer.getId(), name, false);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(McpServerAuditEvents.MCP_SERVER_UPDATED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.EMBEDDED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("mcpServerId", String.valueOf(mcpServer.getId()))
            .containsEntry("name", name)
            .containsEntry("enabled", "false")
            .doesNotContainKey("authenticationRequired")
            .doesNotContainKey("enforceToolAuthorization")
            .doesNotContainKey("secretKey")
            .doesNotContainValue(mcpServer.getSecretKey());
    }

    @Test
    void testUpdateByIdWithAuthorizationFlagsRecordsTheFlags() {
        McpServer mcpServer = saveMcpServer(PlatformType.AUTOMATION);

        String name = uniqueName("renamed");

        mcpServerService.update(mcpServer.getId(), name, false, true, true);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(McpServerAuditEvents.MCP_SERVER_UPDATED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("mcpServerId", String.valueOf(mcpServer.getId()))
            .containsEntry("name", name)
            .containsEntry("enabled", "false")
            .containsEntry("authenticationRequired", "true")
            .containsEntry("enforceToolAuthorization", "true")
            .doesNotContainKey("secretKey")
            .doesNotContainValue(mcpServer.getSecretKey());
    }

    @Test
    void testRotateSecretKeyRecordsTheServerButNeitherSecretKey() {
        McpServer mcpServer = saveMcpServer(PlatformType.AUTOMATION);

        String oldSecretKey = mcpServer.getSecretKey();

        McpServer rotatedMcpServer = mcpServerService.rotateSecretKey(mcpServer.getId());

        assertThat(rotatedMcpServer.getSecretKey()).isNotEqualTo(oldSecretKey);

        PersistentAuditEvent persistentAuditEvent =
            findSingleEvent(McpServerAuditEvents.MCP_SERVER_SECRET_KEY_ROTATED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("mcpServerId", String.valueOf(mcpServer.getId()))
            .containsEntry("name", mcpServer.getName())
            .containsEntry("type", PlatformType.AUTOMATION.name())
            .doesNotContainKey("secretKey")
            .doesNotContainValue(oldSecretKey)
            .doesNotContainValue(rotatedMcpServer.getSecretKey())
            .doesNotContainKey("dataError");
    }

    @Test
    void testRotateSecretKeyOfAnUnknownServerRecordsAnErrorWithoutAPlatform() {
        assertThatThrownBy(() -> mcpServerService.rotateSecretKey(Long.MAX_VALUE))
            .isInstanceOf(NoSuchElementException.class);

        PersistentAuditEvent persistentAuditEvent =
            findSingleEvent(McpServerAuditEvents.MCP_SERVER_SECRET_KEY_ROTATED);

        assertThat(persistentAuditEvent.getPlatform()).isNull();
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", NoSuchElementException.class.getName())
            .containsEntry("mcpServerId", String.valueOf(Long.MAX_VALUE))
            .doesNotContainKey("name")
            .doesNotContainKey("type");
    }

    @Test
    void testDeleteRecordsThePlatformCapturedBeforeTheDelete() {
        McpServer mcpServer = saveMcpServer(PlatformType.EMBEDDED);

        mcpServerFacade.deleteMcpServer(mcpServer.getId());

        assertThat(mcpServerRepository.findById(mcpServer.getId())).isEmpty();

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(McpServerAuditEvents.MCP_SERVER_DELETED);

        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo(ADMIN_LOGIN);
        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.EMBEDDED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("mcpServerId", String.valueOf(mcpServer.getId()))
            .doesNotContainKey("secretKey")
            .doesNotContainValue(mcpServer.getSecretKey());
    }

    @Test
    void testDeleteOfAnUnknownServerRecordsAnErrorWithoutAPlatform() {
        assertThatThrownBy(() -> mcpServerFacade.deleteMcpServer(Long.MAX_VALUE))
            .isInstanceOf(NoSuchElementException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(McpServerAuditEvents.MCP_SERVER_DELETED);

        assertThat(persistentAuditEvent.getPlatform()).isNull();
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", NoSuchElementException.class.getName())
            .containsEntry("mcpServerId", String.valueOf(Long.MAX_VALUE));
    }

    @Test
    void testCreateComponentRecordsTheComponentAndThePlatformOfItsServer() {
        McpServer mcpServer = saveMcpServer(PlatformType.EMBEDDED);

        McpComponent mcpComponent = mcpServerFacade.create(
            new McpComponent("slack", 1, mcpServer.getId(), null), List.of());

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(McpComponentAuditEvents.MCP_COMPONENT_CREATED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.EMBEDDED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("mcpComponentId", String.valueOf(mcpComponent.getId()))
            .containsEntry("mcpServerId", String.valueOf(mcpServer.getId()))
            .containsEntry("componentName", "slack")
            .containsEntry("componentVersion", "1")
            .doesNotContainKeys("dataError", "mapperError", "captureError");
    }

    @Test
    void testUpdateComponentRecordsTheComponentAndThePlatformOfItsServer() {
        McpServer mcpServer = saveMcpServer(PlatformType.AUTOMATION);

        McpComponent mcpComponent = mcpComponentRepository.save(
            new McpComponent("slack", 1, mcpServer.getId(), null));

        persistenceAuditEventRepository.deleteAll();

        McpComponent changedMcpComponent = new McpComponent();

        changedMcpComponent.setId(mcpComponent.getId());
        changedMcpComponent.setVersion(mcpComponent.getVersion());

        mcpServerFacade.update(changedMcpComponent, null, List.of());

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(McpComponentAuditEvents.MCP_COMPONENT_UPDATED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("mcpComponentId", String.valueOf(mcpComponent.getId()))
            .containsEntry("mcpServerId", String.valueOf(mcpServer.getId()))
            .doesNotContainKeys("dataError", "mapperError", "captureError");
    }

    @Test
    void testDeleteComponentRecordsThePlatformCapturedBeforeTheDelete() {
        McpServer mcpServer = saveMcpServer(PlatformType.EMBEDDED);

        McpComponent mcpComponent = mcpComponentRepository.save(
            new McpComponent("slack", 1, mcpServer.getId(), null));

        persistenceAuditEventRepository.deleteAll();

        mcpServerFacade.deleteMcpComponent(mcpComponent.getId());

        assertThat(mcpComponentRepository.findById(mcpComponent.getId())).isEmpty();

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(McpComponentAuditEvents.MCP_COMPONENT_DELETED);

        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.EMBEDDED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("mcpComponentId", String.valueOf(mcpComponent.getId()));
    }

    @Test
    void testDeleteOfAnUnknownComponentRecordsAnErrorWithoutAPlatform() {
        assertThatThrownBy(() -> mcpServerFacade.deleteMcpComponent(Long.MAX_VALUE))
            .isInstanceOf(NoSuchElementException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(McpComponentAuditEvents.MCP_COMPONENT_DELETED);

        assertThat(persistentAuditEvent.getPlatform()).isNull();
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", NoSuchElementException.class.getName())
            .containsEntry("mcpComponentId", String.valueOf(Long.MAX_VALUE));
    }

    private PersistentAuditEvent findSingleEvent(String eventType) {
        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> persistentAuditEvents = page.getContent()
            .stream()
            .filter(persistentAuditEvent -> eventType.equals(persistentAuditEvent.getEventType()))
            .toList();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }

    private McpServer saveMcpServer(PlatformType platformType) {
        McpServer mcpServer = mcpServerRepository.save(
            new McpServer(uniqueName("saved"), platformType, Environment.DEVELOPMENT));

        persistenceAuditEventRepository.deleteAll();

        return mcpServer;
    }

    private static String uniqueName(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    @Configuration
    @Import({
        AuditAspect.class, AuditCaptureAspect.class, AuditEventService.class, AuditMapperResolver.class,
        McpServerFacadeImpl.class
    })
    static class AuditConfiguration {

        @Bean
        ApplicationProperties applicationProperties() {
            return new ApplicationProperties();
        }
    }
}
