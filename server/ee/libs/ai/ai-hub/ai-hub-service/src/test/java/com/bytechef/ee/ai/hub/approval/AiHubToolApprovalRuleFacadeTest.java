/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agui.core.exception.AGUIException;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.ai.hub.agent.AiHubSpringAIAgent;
import com.bytechef.ee.ai.hub.config.AiHubAgentFixtures;
import com.bytechef.ee.ai.hub.util.Mode;
import com.bytechef.platform.security.constant.AuthorityConstants;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubToolApprovalRuleFacadeTest {

    private static final long WORKSPACE_ID = 7L;

    private final AiHubToolApprovalRuleService ruleService = mock(AiHubToolApprovalRuleService.class);
    private final PermissionService permissionService = mock(PermissionService.class);

    private AiHubSpringAIAgent askAgent;
    private AiHubSpringAIAgent buildAgent;
    private AiHubToolApprovalRuleFacadeImpl facade;

    @BeforeEach
    void setUp() throws AGUIException {
        AiHubAgentFixtures fixtures = AiHubAgentFixtures.withBuildWorkflow();

        askAgent = fixtures.askAgent();
        buildAgent = fixtures.buildAgent();

        facade = new AiHubToolApprovalRuleFacadeImpl(
            mode -> mode == Mode.BUILD ? buildAgent.pinnedToolCallbacks() : askAgent.pinnedToolCallbacks(),
            ruleService, permissionService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetRulesRequiresTheWorkspaceViewerRole() {
        when(permissionService.hasWorkspaceRole(WORKSPACE_ID, "VIEWER")).thenReturn(false);

        assertThatThrownBy(() -> facade.getRules(WORKSPACE_ID)).isInstanceOf(AccessDeniedException.class);

        verify(ruleService, never()).getRules(anyLong());
    }

    @Test
    void testGetRulesReturnsTheWorkspaceRulesForAViewer() {
        AiHubToolApprovalRule rule = new AiHubToolApprovalRule();

        when(permissionService.hasWorkspaceRole(WORKSPACE_ID, "VIEWER")).thenReturn(true);
        when(ruleService.getRules(WORKSPACE_ID)).thenReturn(List.of(rule));

        assertThat(facade.getRules(WORKSPACE_ID)).containsExactly(rule);
    }

    @Test
    void testCreateRuleRequiresTheAdminAuthority() {
        authenticate("user");

        assertThatThrownBy(
            () -> facade.createRule(
                WORKSPACE_ID, AiHubToolApproval.ToolKind.CATALOG, "deleteProject",
                AiHubToolApprovalRule.Mode.EXEMPT))
                    .isInstanceOf(AccessDeniedException.class);

        verify(ruleService, never()).save(any());
    }

    @Test
    void testCreateRuleRejectsBlankToolName() {
        authenticate(AuthorityConstants.ADMIN);

        assertThatThrownBy(
            () -> facade.createRule(
                WORKSPACE_ID, AiHubToolApproval.ToolKind.CATALOG, "   ", AiHubToolApprovalRule.Mode.REQUIRE))
                    .isInstanceOf(IllegalArgumentException.class);

        verify(ruleService, never()).save(any());
    }

    @Test
    void testCreateRuleRejectsAComponentRule() {
        authenticate(AuthorityConstants.ADMIN);

        assertThatThrownBy(
            () -> facade.createRule(
                WORKSPACE_ID, AiHubToolApproval.ToolKind.COMPONENT, "sendMessage",
                AiHubToolApprovalRule.Mode.EXEMPT))
                    .isInstanceOf(IllegalArgumentException.class);

        verify(ruleService, never()).save(any());
    }

    @Test
    void testCreateRuleSavesACatalogRuleWithTheWorkspaceIdFromTheArgument() {
        authenticate(AuthorityConstants.ADMIN);

        when(ruleService.save(any(AiHubToolApprovalRule.class))).thenAnswer(invocation -> {
            AiHubToolApprovalRule savedRule = invocation.getArgument(0);

            savedRule.setId(42L);

            return savedRule;
        });

        AiHubToolApprovalRule savedRule = facade.createRule(
            WORKSPACE_ID, AiHubToolApproval.ToolKind.CATALOG, " deleteProject ", AiHubToolApprovalRule.Mode.REQUIRE);

        assertThat(savedRule.getId()).isEqualTo(42L);

        ArgumentCaptor<AiHubToolApprovalRule> ruleCaptor = ArgumentCaptor.forClass(AiHubToolApprovalRule.class);

        verify(ruleService).save(ruleCaptor.capture());

        AiHubToolApprovalRule capturedRule = ruleCaptor.getValue();

        assertThat(capturedRule.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(capturedRule.getToolKind()).isEqualTo(AiHubToolApproval.ToolKind.CATALOG);
        assertThat(capturedRule.getComponentName()).isNull();
        assertThat(capturedRule.getToolName()).isEqualTo("deleteProject");
        assertThat(capturedRule.getMode()).isEqualTo(AiHubToolApprovalRule.Mode.REQUIRE);
    }

    @Test
    void testDeleteRuleRequiresTheAdminAuthority() {
        authenticate("user");

        assertThatThrownBy(() -> facade.deleteRule(WORKSPACE_ID, 42L)).isInstanceOf(AccessDeniedException.class);

        verify(ruleService, never()).delete(anyLong(), anyLong());
    }

    @Test
    void testDeleteRuleDelegatesToRuleService() {
        authenticate(AuthorityConstants.ADMIN);

        facade.deleteRule(WORKSPACE_ID, 42L);

        verify(ruleService).delete(WORKSPACE_ID, 42L);
    }

    @Test
    void testGetDefaultToolNamesListsOnlyTheRegisteredGatedTools() {
        assertThat(facade.getDefaultToolNames()).containsExactly(
            "createProjectDeployment", "deleteAiAgentChannel", "deleteAiAgentElement", "deleteProject",
            "deleteProjectDeployment", "deleteWorkflow", "promoteWorkflow", "publishProject",
            "rollbackProjectDeployment",
            "toggleProjectDeployment", "updateProjectDeployment");
    }

    @Test
    void testEveryRegisteredToolTheFormerBuiltInSetGatedIsStillGated() {
        List<String> formerBuiltInToolNames = List.of(
            "deleteProject", "deleteWorkflow", "deleteProjectDeployment", "dropDataTable", "deleteDataTableRow",
            "deleteKnowledgeBase", "deleteKnowledgeBaseDocument", "aiAgentUtils_deleteAiSkill", "deleteContextStore",
            "deleteContextStoreSource", "deleteAiAgentChannel", "deleteAiAgentElement", "rollbackProjectDeployment",
            "promoteWorkflow");

        List<String> registeredToolNames = registeredToolNames();

        assertThat(registeredToolNames).isNotEmpty();
        assertThat(registeredToolNames)
            .filteredOn(formerBuiltInToolNames::contains)
            .allMatch(AiHubToolApprovalDefaults::isGatedByDefault)
            .allMatch(facade.getDefaultToolNames()::contains);
    }

    private List<String> registeredToolNames() {
        return Stream.concat(
            askAgent.pinnedToolCallbacks()
                .stream(),
            buildAgent.pinnedToolCallbacks()
                .stream())
            .map(ToolCallback::getToolDefinition)
            .map(ToolDefinition::name)
            .toList();
    }

    private static void authenticate(String authority) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "login", "n/a", List.of(new SimpleGrantedAuthority(authority))));
    }
}
