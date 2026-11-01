/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ai.mcp.server.configuration.audit.ManagementMcpServerAuditEvents;
import com.bytechef.ai.mcp.server.configuration.service.ManagementMcpServerService;
import com.bytechef.ai.mcp.server.configuration.service.ManagementMcpServerServiceImpl;
import com.bytechef.ee.platform.audit.config.AuditIntTestConfiguration;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AuditIntTestConfiguration.class)
@ActiveProfiles("testint")
@Import({
    ManagementMcpServerServiceImpl.class, PostgreSQLContainerConfiguration.class
})
@MockitoBean(types = PropertyService.class)
class ManagementMcpServerAuditIntTest {

    @Autowired
    private ManagementMcpServerService managementMcpServerService;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @BeforeEach
    void beforeEach() {
        persistenceAuditEventRepository.deleteAll();

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "admin@localhost.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testSecretKeyRotationIsRecordedWithoutTheNewUrl() {
        String managementMcpServerUrl = managementMcpServerService.updateManagementMcpServerUrl();

        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> persistentAuditEvents = page.getContent();

        assertThat(persistentAuditEvents).hasSize(1);

        PersistentAuditEvent persistentAuditEvent = persistentAuditEvents.getFirst();

        assertThat(persistentAuditEvent.getEventType())
            .isEqualTo(ManagementMcpServerAuditEvents.MANAGEMENT_MCP_SERVER_SECRET_KEY_ROTATED);
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("admin@localhost.com");
        String secretKey = StringUtils.substringBetween(managementMcpServerUrl, "/api/management/", "/mcp");

        assertThat(secretKey).isNotBlank();
        assertThat(persistentAuditEvent.getData()).containsEntry("result", "SUCCESS");
        assertThat(persistentAuditEvent.getData()
            .values()).noneMatch(value -> value.contains(secretKey));
    }
}
