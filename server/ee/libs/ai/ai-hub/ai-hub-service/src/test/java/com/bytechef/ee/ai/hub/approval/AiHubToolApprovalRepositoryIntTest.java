/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.ai.hub.approval.repository.AiHubToolApprovalRepository;
import com.bytechef.ee.ai.hub.approval.repository.AiHubToolApprovalRuleRepository;
import com.bytechef.ee.ai.hub.config.AiHubIntTestConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;

/**
 * Integration test for {@link AiHubToolApprovalRepository} and {@link AiHubToolApprovalRuleRepository}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = AiHubIntTestConfiguration.class,
    properties = {
        "bytechef.edition=ee", "spring.liquibase.change-log=classpath:config/liquibase/ai-hub-testint.xml",
        "spring.liquibase.contexts=configuration"
    })
@Import(PostgreSQLContainerConfiguration.class)
public class AiHubToolApprovalRepositoryIntTest {

    @Autowired
    private AiHubToolApprovalRepository approvalRepository;

    @Autowired
    private AiHubToolApprovalRuleRepository ruleRepository;

    @AfterEach
    public void afterEach() {
        approvalRepository.deleteAll();
        ruleRepository.deleteAll();
    }

    @Test
    public void testFindFirstByChatIdAndStatusFindsAPendingApproval() {
        approvalRepository.save(buildApproval(5L, "thread-5"));

        Optional<AiHubToolApproval> found = approvalRepository.findFirstByChatIdAndStatus(
            5L, AiHubToolApproval.Status.PENDING.ordinal());

        assertThat(found).isPresent();
        assertThat(found.get()
            .getStatus()).isEqualTo(AiHubToolApproval.Status.PENDING);
        assertThat(found.get()
            .getToolName()).isEqualTo("createProject");

        assertThat(approvalRepository.findFirstByChatIdAndStatus(
            5L, AiHubToolApproval.Status.APPROVED.ordinal())).isEmpty();
        assertThat(approvalRepository.findFirstByChatIdAndStatus(
            6L, AiHubToolApproval.Status.PENDING.ordinal())).isEmpty();
    }

    @Test
    public void testFindAllByChatIdOrderByCreatedDateDesc() {
        approvalRepository.save(buildApproval(7L, "thread-7"));
        approvalRepository.save(buildApproval(7L, "thread-7"));
        approvalRepository.save(buildApproval(8L, "thread-8"));

        assertThat(approvalRepository.findAllByChatIdOrderByCreatedDateDesc(7L)).hasSize(2);
    }

    @Test
    public void testDeleteAllByChatIdRemovesOnlyThatChatsApprovals() {
        approvalRepository.save(buildApproval(9L, "thread-9"));
        approvalRepository.save(buildApproval(10L, "thread-10"));

        approvalRepository.deleteAllByChatId(9L);

        assertThat(approvalRepository.findAllByChatIdOrderByCreatedDateDesc(9L)).isEmpty();
        assertThat(approvalRepository.findAllByChatIdOrderByCreatedDateDesc(10L)).hasSize(1);
    }

    @Test
    public void testRuleUniqueIndexRejectsADuplicate() {
        ruleRepository.save(buildRule(1L, "COMPONENT_A", "tool"));

        assertThatThrownBy(() -> ruleRepository.save(buildRule(1L, "COMPONENT_A", "tool")))
            .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    public void testRuleUniqueIndexCollapsesANullComponentName() {
        ruleRepository.save(buildRule(1L, null, "tool"));

        assertThatThrownBy(() -> ruleRepository.save(buildRule(1L, null, "tool")))
            .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    public void testRuleUniqueIndexAllowsDistinctTuples() {
        ruleRepository.save(buildRule(1L, "COMPONENT_A", "tool"));
        ruleRepository.save(buildRule(1L, "COMPONENT_B", "tool"));
        ruleRepository.save(buildRule(2L, "COMPONENT_A", "tool"));

        assertThat(ruleRepository.findAllByWorkspaceId(1L)).hasSize(2);
        assertThat(ruleRepository.findAllByWorkspaceId(2L)).hasSize(1);
    }

    private static AiHubToolApproval buildApproval(long chatId, String threadId) {
        AiHubToolApproval approval = new AiHubToolApproval();

        approval.setChatId(chatId);
        approval.setThreadId(threadId);
        approval.setRequestedByUserId(1L);
        approval.setToolKind(AiHubToolApproval.ToolKind.CATALOG);
        approval.setToolName("createProject");
        approval.setArguments("{}");
        approval.setStatus(AiHubToolApproval.Status.PENDING);
        approval.setMode("BUILD");
        approval.setEnvironment(0);
        approval.setExpiresAt(Instant.now()
            .plusSeconds(600));

        return approval;
    }

    private static AiHubToolApprovalRule buildRule(long workspaceId, String componentName, String toolName) {
        AiHubToolApprovalRule rule = new AiHubToolApprovalRule();

        rule.setWorkspaceId(workspaceId);
        rule.setToolKind(AiHubToolApproval.ToolKind.COMPONENT);
        rule.setComponentName(componentName);
        rule.setToolName(toolName);
        rule.setMode(AiHubToolApprovalRule.Mode.REQUIRE);

        return rule;
    }
}
