/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.approval;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.ai.hub.approval.repository.AiHubToolApprovalRuleRepository;
import com.bytechef.ee.ai.hub.audit.AiHubAuditEvents;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.config.AiHubAuditIntTestConfiguration;
import com.bytechef.ee.ai.hub.toolsearch.AiHubGlobalToolCatalog;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
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
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = AiHubAuditIntTestConfiguration.class,
    properties = {
        "bytechef.audit.enabled=true", "bytechef.edition=ee",
        "spring.liquibase.change-log=classpath:config/liquibase/ai-hub-audit-testint.xml"
    })
@Import({
    AiHubToolApprovalRuleFacadeIntTest.AiHubToolApprovalRuleFacadeIntTestConfiguration.class,
    PostgreSQLContainerConfiguration.class
})
@MockitoBean(types = {
    AiHubChatService.class, AiHubPinnedToolCallbacks.class, PermissionService.class
})
public class AiHubToolApprovalRuleFacadeIntTest {

    private static final long WORKSPACE_ID = 7L;

    @Autowired
    private AiHubToolApprovalRuleFacade ruleFacade;

    @Autowired
    private AiHubToolApprovalRuleRepository ruleRepository;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @BeforeEach
    public void beforeEach() {
        persistenceAuditEventRepository.deleteAll();

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "alice", "n/a", List.of(new SimpleGrantedAuthority(AuthorityConstants.ADMIN))));
    }

    @AfterEach
    public void afterEach() {
        SecurityContextHolder.clearContext();

        ruleRepository.deleteAll();
        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    public void testCreateRuleRecordsTheRule() {
        AiHubToolApprovalRule rule = ruleFacade.createRule(
            WORKSPACE_ID, AiHubToolApproval.ToolKind.CATALOG, " createProject ", AiHubToolApprovalRule.Mode.REQUIRE);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType())
            .isEqualTo(AiHubAuditEvents.AI_HUB_TOOL_APPROVAL_RULE_CHANGED);
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("alice");
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("action", "created")
            .containsEntry("workspaceId", String.valueOf(WORKSPACE_ID))
            .containsEntry("ruleId", String.valueOf(rule.getId()))
            .containsEntry("toolName", "createProject")
            .containsEntry("mode", "REQUIRE")
            .doesNotContainKeys("dataError", "mapperError");
    }

    @Test
    public void testDeleteRuleRecordsTheDeletedRule() {
        AiHubToolApprovalRule rule = new AiHubToolApprovalRule();

        rule.setWorkspaceId(WORKSPACE_ID);
        rule.setToolKind(AiHubToolApproval.ToolKind.CATALOG);
        rule.setToolName("deleteProject");
        rule.setMode(AiHubToolApprovalRule.Mode.EXEMPT);

        AiHubToolApprovalRule savedRule = ruleRepository.save(rule);

        ruleFacade.deleteRule(WORKSPACE_ID, savedRule.getId());

        PersistentAuditEvent persistentAuditEvent = findSingleEvent();

        assertThat(persistentAuditEvent.getEventType())
            .isEqualTo(AiHubAuditEvents.AI_HUB_TOOL_APPROVAL_RULE_CHANGED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("action", "deleted")
            .containsEntry("workspaceId", String.valueOf(WORKSPACE_ID))
            .containsEntry("ruleId", String.valueOf(savedRule.getId()))
            .containsEntry("toolName", "deleteProject")
            .containsEntry("mode", "EXEMPT")
            .doesNotContainKeys("captureError", "dataError", "mapperError");
    }

    private PersistentAuditEvent findSingleEvent() {
        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> persistentAuditEvents = page.getContent();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }

    @Configuration
    static class AiHubToolApprovalRuleFacadeIntTestConfiguration {

        @Bean
        AiHubToolApprovalRuleService aiHubToolApprovalRuleService(AiHubToolApprovalRuleRepository ruleRepository) {
            return new AiHubToolApprovalRuleServiceImpl(ruleRepository);
        }

        @Bean
        AiHubToolApprovalRuleFacade aiHubToolApprovalRuleFacade(
            AiHubPinnedToolCallbacks pinnedToolCallbacks,
            ObjectProvider<AiHubGlobalToolCatalog> globalToolCatalogProvider, AiHubToolApprovalRuleService ruleService,
            PermissionService permissionService) {

            return new AiHubToolApprovalRuleFacadeImpl(
                pinnedToolCallbacks, globalToolCatalogProvider, ruleService, permissionService);
        }
    }
}
