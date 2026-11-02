/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.ee.automation.configuration.repository.CustomRoleRepository;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditOutcome;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CustomRoleAuditMapperTest {

    private CustomRoleAuditMapper customRoleAuditMapper;
    private CustomRoleRepository customRoleRepository;

    @BeforeEach
    void beforeEach() {
        customRoleRepository = mock(CustomRoleRepository.class);
        customRoleAuditMapper = new CustomRoleAuditMapper(customRoleRepository);
    }

    @Test
    void testCreatedRecordsIdNameAndSortedScopesButNotTheDescription() {
        CustomRole customRole = mock(CustomRole.class);

        when(customRole.getId()).thenReturn(5L);

        Map<String, String> data = customRoleAuditMapper.map(invocation(
            CustomRoleAuditEvents.CUSTOM_ROLE_CREATED,
            Map.of("name", "Auditor", "description", "Reads workflows", "scopeNames",
                Set.of("WORKFLOW_VIEW", "CONNECTION_VIEW")),
            customRole, null));

        assertThat(data).containsExactlyInAnyOrderEntriesOf(
            Map.of(
                "customRoleId", "5", "name", "Auditor", "scopes", "CONNECTION_VIEW,WORKFLOW_VIEW", "scopeCount",
                "2"));
    }

    @Test
    void testUpdatedRecordsPreviousAndNewValues() {
        when(customRoleRepository.findById(5L))
            .thenReturn(Optional.of(new CustomRole("Auditor", Set.of("WORKFLOW_VIEW"))));

        Map<String, Object> arguments = Map.of(
            "roleId", 5L, "name", "Reviewer", "description", "x", "scopeNames",
            Set.of("WORKFLOW_VIEW", "WORKFLOW_EDIT"));

        Object captured = customRoleAuditMapper.capture(
            invocation(CustomRoleAuditEvents.CUSTOM_ROLE_UPDATED, arguments, null, null));

        assertThat(customRoleAuditMapper.map(
            invocation(CustomRoleAuditEvents.CUSTOM_ROLE_UPDATED, arguments, null, captured)))
                .containsExactlyInAnyOrderEntriesOf(
                    Map.of(
                        "customRoleId", "5", "name", "Reviewer", "scopes", "WORKFLOW_EDIT,WORKFLOW_VIEW",
                        "scopeCount", "2", "previousName", "Auditor", "previousScopes", "WORKFLOW_VIEW",
                        "previousScopeCount", "1"));
    }

    @Test
    void testUpdatedOverTheLimitKeepsWholeScopesAndRecordsTheFullCount() {
        Set<String> manyScopes = new TreeSet<>();

        for (int index = 0; index < 60; index++) {
            manyScopes.add(String.format("SCOPE_%03d", index));
        }

        when(customRoleRepository.findById(5L))
            .thenReturn(Optional.of(new CustomRole("Auditor", manyScopes)));

        Map<String, Object> arguments = Map.of(
            "roleId", 5L, "name", "Reviewer", "description", "x", "scopeNames", manyScopes);

        Object captured = customRoleAuditMapper.capture(
            invocation(CustomRoleAuditEvents.CUSTOM_ROLE_UPDATED, arguments, null, null));

        Map<String, String> data = customRoleAuditMapper.map(
            invocation(CustomRoleAuditEvents.CUSTOM_ROLE_UPDATED, arguments, null, captured));

        assertThat(data.get("scopes")).hasSizeLessThanOrEqualTo(256);
        assertThat(data.get("previousScopes")).hasSizeLessThanOrEqualTo(256);
        assertThat(data).containsEntry("scopeCount", "60")
            .containsEntry("previousScopeCount", "60");
    }

    @Test
    void testDeletedRecordsTheCapturedName() {
        when(customRoleRepository.findById(5L))
            .thenReturn(Optional.of(new CustomRole("Auditor", Set.of("WORKFLOW_VIEW"))));

        Map<String, Object> arguments = Map.of("roleId", 5L);

        Object captured = customRoleAuditMapper.capture(
            invocation(CustomRoleAuditEvents.CUSTOM_ROLE_DELETED, arguments, null, null));

        assertThat(customRoleAuditMapper.map(
            invocation(CustomRoleAuditEvents.CUSTOM_ROLE_DELETED, arguments, null, captured)))
                .containsExactlyInAnyOrderEntriesOf(Map.of("customRoleId", "5", "name", "Auditor"));
    }

    @Test
    void testDeletingAMissingRoleRecordsOnlyTheId() {
        when(customRoleRepository.findById(5L)).thenReturn(Optional.empty());

        Map<String, Object> arguments = Map.of("roleId", 5L);

        Object captured = customRoleAuditMapper.capture(
            invocation(CustomRoleAuditEvents.CUSTOM_ROLE_DELETED, arguments, null, null));

        assertThat(customRoleAuditMapper.map(
            invocation(CustomRoleAuditEvents.CUSTOM_ROLE_DELETED, arguments, null, captured)))
                .containsExactlyInAnyOrderEntriesOf(Map.of("customRoleId", "5"));
    }

    private static AuditInvocation invocation(
        String event, Map<String, Object> arguments, Object result, Object captured) {

        return new AuditInvocation(event, arguments, result, captured, AuditOutcome.SUCCESS, null);
    }
}
