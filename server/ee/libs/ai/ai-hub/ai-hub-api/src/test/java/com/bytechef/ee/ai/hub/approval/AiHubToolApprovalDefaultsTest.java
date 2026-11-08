/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.approval;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubToolApprovalDefaultsTest {

    @Test
    void testTheEnvironmentPromotionToolsAreGated() {
        assertThat(AiHubToolApprovalDefaults.isGatedByDefault("promoteApiCollection")).isTrue();
        assertThat(AiHubToolApprovalDefaults.isGatedByDefault("promoteMcpServer")).isTrue();
        assertThat(AiHubToolApprovalDefaults.isGatedByDefault("promoteA2aServer")).isTrue();
        assertThat(AiHubToolApprovalDefaults.isGatedByDefault("promoteProjectDeployment")).isTrue();
    }

    @Test
    void testOnlyTheContextStoreDeleteToolsAreGated() {
        assertThat(AiHubToolApprovalDefaults.isGatedByDefault("deleteContextStore")).isTrue();
        assertThat(AiHubToolApprovalDefaults.isGatedByDefault("deleteContextStoreSource")).isTrue();
        assertThat(AiHubToolApprovalDefaults.isGatedByDefault("createContextStoreSource")).isFalse();
        assertThat(AiHubToolApprovalDefaults.isGatedByDefault("setContextStoreSourceEnabled")).isFalse();
        assertThat(AiHubToolApprovalDefaults.isGatedByDefault("searchContextStore")).isFalse();
    }

    @Test
    void testAComponentToolThatReadsIsNotGated() {
        assertThat(AiHubToolApprovalDefaults.isComponentToolGatedByDefault("getRows")).isFalse();
        assertThat(AiHubToolApprovalDefaults.isComponentToolGatedByDefault("listChannels")).isFalse();
        assertThat(AiHubToolApprovalDefaults.isComponentToolGatedByDefault("searchIssues")).isFalse();
        assertThat(AiHubToolApprovalDefaults.isComponentToolGatedByDefault("search")).isFalse();
    }

    @Test
    void testAComponentToolThatWritesIsGated() {
        assertThat(AiHubToolApprovalDefaults.isComponentToolGatedByDefault("sendMessage")).isTrue();
        assertThat(AiHubToolApprovalDefaults.isComponentToolGatedByDefault("createIssue")).isTrue();
        assertThat(AiHubToolApprovalDefaults.isComponentToolGatedByDefault("deleteRow")).isTrue();
    }

    @Test
    void testAComponentToolWhoseNameOnlyStartsWithAReadWordIsGated() {
        assertThat(AiHubToolApprovalDefaults.isComponentToolGatedByDefault("getaway")).isTrue();
        assertThat(AiHubToolApprovalDefaults.isComponentToolGatedByDefault("listen")).isTrue();
    }

    @Test
    void testWorkspaceRulesOverrideTheComponentToolDefault() {
        AiHubToolApprovalPolicy.Decision decision = new AiHubToolApprovalPolicy.Decision(
            Set.of("slack_getUser"), Set.of("slack_sendMessage"));

        assertThat(decision.isComponentToolGated("slack_sendMessage", "sendMessage")).isFalse();
        assertThat(decision.isComponentToolGated("slack_getUser", "getUser")).isTrue();
        assertThat(decision.isComponentToolGated("github_createIssue", "createIssue")).isTrue();
        assertThat(decision.isComponentToolGated("github_listIssues", "listIssues")).isFalse();
    }
}
