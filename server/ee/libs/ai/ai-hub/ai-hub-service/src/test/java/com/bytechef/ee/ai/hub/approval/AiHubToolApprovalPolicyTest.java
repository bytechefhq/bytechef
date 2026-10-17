/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.approval.repository.AiHubToolApprovalRuleRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubToolApprovalPolicyTest {

    private static final long WORKSPACE_ID = 7L;

    private final AiHubToolApprovalRuleRepository ruleRepository = mock(AiHubToolApprovalRuleRepository.class);

    private final AiHubToolApprovalPolicy policy = new AiHubToolApprovalPolicyImpl(ruleRepository);

    @Test
    void testBuiltInDefaultsAreGatedWithNoRules() {
        AiHubToolApprovalPolicy.Decision decision = policy.decide(WORKSPACE_ID);

        assertThat(decision.isGated("deleteProject")).isTrue();
        assertThat(decision.isGated("dropDataTable")).isTrue();
        assertThat(decision.isGated("deleteKnowledgeBase")).isTrue();
        assertThat(decision.isGated("listProjects")).isFalse();
    }

    @Test
    void testDefaultsGateTheInScopeDestructiveTools() {
        assertThat(List.of(
            "deleteProject", "deleteWorkflow", "deleteProjectDeployment", "rollbackProjectDeployment",
            "promoteWorkflow"))
                .allMatch(AiHubToolApprovalDefaults::isGatedByDefault);
        assertThat(AiHubToolApprovalDefaults.isGatedByDefault("listProjects")).isFalse();
    }

    @Test
    void testPrefixMatchGatesAnUnlistedDestructiveName() {
        assertThat(AiHubToolApprovalDefaults.isGatedByDefault("deleteSomethingNew")).isTrue();
        assertThat(AiHubToolApprovalDefaults.isGatedByDefault("promoteSomethingNew")).isTrue();
        assertThat(AiHubToolApprovalDefaults.isGatedByDefault("deleted")).isFalse();
        assertThat(AiHubToolApprovalDefaults.isGatedByDefault("getProject")).isFalse();
    }

    @Test
    void testWorkspaceExemptRemovesADefault() {
        when(ruleRepository.findAllByWorkspaceId(WORKSPACE_ID)).thenReturn(List.of(
            rule(AiHubToolApproval.ToolKind.CATALOG, "deleteProject", AiHubToolApprovalRule.Mode.EXEMPT)));

        AiHubToolApprovalPolicy.Decision decision = policy.decide(WORKSPACE_ID);

        assertThat(decision.isGated("deleteProject")).isFalse();
    }

    @Test
    void testWorkspaceRequireGatesACatalogTool() {
        when(ruleRepository.findAllByWorkspaceId(WORKSPACE_ID)).thenReturn(List.of(
            rule(AiHubToolApproval.ToolKind.CATALOG, "createProject", AiHubToolApprovalRule.Mode.REQUIRE)));

        AiHubToolApprovalPolicy.Decision decision = policy.decide(WORKSPACE_ID);

        assertThat(decision.isGated("createProject")).isTrue();
    }

    @Test
    void testComponentRulesAreIgnored() {
        when(ruleRepository.findAllByWorkspaceId(WORKSPACE_ID)).thenReturn(List.of(
            rule(AiHubToolApproval.ToolKind.COMPONENT, "createProject", AiHubToolApprovalRule.Mode.REQUIRE),
            rule(AiHubToolApproval.ToolKind.COMPONENT, "deleteProject", AiHubToolApprovalRule.Mode.EXEMPT)));

        AiHubToolApprovalPolicy.Decision decision = policy.decide(WORKSPACE_ID);

        assertThat(decision.isGated("createProject")).isFalse();
        assertThat(decision.isGated("deleteProject")).isTrue();
    }

    @Test
    void testRepositoryFailureFallsBackToDefaultsOnly() {
        when(ruleRepository.findAllByWorkspaceId(WORKSPACE_ID)).thenThrow(new IllegalStateException("db down"));

        AiHubToolApprovalPolicy.Decision decision = policy.decide(WORKSPACE_ID);

        for (String toolName : AiHubToolApprovalDefaults.DEFAULT_TOOL_NAMES) {
            assertThat(decision.isGated(toolName)).isTrue();
        }

        assertThat(decision.exempt()).isEmpty();
    }

    private static AiHubToolApprovalRule rule(
        AiHubToolApproval.ToolKind toolKind, String toolName, AiHubToolApprovalRule.Mode mode) {

        AiHubToolApprovalRule rule = new AiHubToolApprovalRule();

        rule.setWorkspaceId(WORKSPACE_ID);
        rule.setToolKind(toolKind);
        rule.setToolName(toolName);
        rule.setMode(mode);

        return rule;
    }
}
