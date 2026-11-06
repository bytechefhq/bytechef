/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.user.scim.model.ScimGroup;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditOutcome;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.service.AuthorityService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class ScimGroupAuditMapperTest {

    private static final long GROUP_ID = 7L;

    private final AuthorityService authorityService = mock(AuthorityService.class);
    private final ScimGroupAuditMapper scimGroupAuditMapper = new ScimGroupAuditMapper(authorityService);

    @Test
    void testCaptureSkipsCreatedEvent() {
        AuditInvocation auditInvocation = createAuditInvocation(
            ScimGroupAuditEvents.SCIM_GROUP_CREATED, Map.of("scimGroup", createScimGroup("ROLE_DEV")), null, null);

        assertThat(scimGroupAuditMapper.capture(auditInvocation)).isNull();

        verifyNoInteractions(authorityService);
    }

    @Test
    void testCaptureReturnsCurrentAuthorityName() {
        when(authorityService.fetchAuthority(GROUP_ID)).thenReturn(Optional.of(createAuthority("ROLE_OLD")));

        AuditInvocation auditInvocation = createAuditInvocation(
            ScimGroupAuditEvents.SCIM_GROUP_UPDATED, Map.of("id", GROUP_ID), null, null);

        assertThat(scimGroupAuditMapper.capture(auditInvocation)).isEqualTo("ROLE_OLD");
    }

    @Test
    void testCaptureReturnsNullWhenAuthorityDoesNotExist() {
        when(authorityService.fetchAuthority(GROUP_ID)).thenReturn(Optional.empty());

        AuditInvocation auditInvocation = createAuditInvocation(
            ScimGroupAuditEvents.SCIM_GROUP_DELETED, Map.of("id", GROUP_ID), null, null);

        assertThat(scimGroupAuditMapper.capture(auditInvocation)).isNull();
    }

    @Test
    void testMapCreatedEventUsesResultAuthority() {
        AuditInvocation auditInvocation = createAuditInvocation(
            ScimGroupAuditEvents.SCIM_GROUP_CREATED, Map.of("scimGroup", createScimGroup("ROLE_DEV")),
            createAuthority("ROLE_DEV"), null);

        assertThat(scimGroupAuditMapper.map(auditInvocation)).containsExactly(
            Map.entry("groupId", "7"), Map.entry("name", "ROLE_DEV"));
    }

    @Test
    void testMapFailedCreateFallsBackToScimGroupDisplayName() {
        AuditInvocation auditInvocation = new AuditInvocation(
            ScimGroupAuditEvents.SCIM_GROUP_CREATED, Map.of("scimGroup", createScimGroup("ROLE_DEV")), null, null,
            AuditOutcome.ERROR, IllegalStateException.class);

        assertThat(scimGroupAuditMapper.map(auditInvocation)).containsExactly(Map.entry("name", "ROLE_DEV"));
    }

    @Test
    void testMapUpdatedEventRecordsPreviousNameWhenRenamed() {
        AuditInvocation auditInvocation = createAuditInvocation(
            ScimGroupAuditEvents.SCIM_GROUP_UPDATED,
            Map.of("id", GROUP_ID, "scimGroup", createScimGroup("ROLE_NEW")), Optional.of(createAuthority("ROLE_NEW")),
            "ROLE_OLD");

        assertThat(scimGroupAuditMapper.map(auditInvocation)).containsExactly(
            Map.entry("groupId", "7"), Map.entry("name", "ROLE_NEW"), Map.entry("previousName", "ROLE_OLD"));
    }

    @Test
    void testMapUpdatedEventOmitsPreviousNameWhenUnchanged() {
        AuditInvocation auditInvocation = createAuditInvocation(
            ScimGroupAuditEvents.SCIM_GROUP_UPDATED, Map.of("id", GROUP_ID), Optional.of(createAuthority("ROLE_DEV")),
            "ROLE_DEV");

        assertThat(scimGroupAuditMapper.map(auditInvocation)).containsExactly(
            Map.entry("groupId", "7"), Map.entry("name", "ROLE_DEV"));
    }

    @Test
    void testMapDeletedEventUsesCapturedName() {
        AuditInvocation auditInvocation = createAuditInvocation(
            ScimGroupAuditEvents.SCIM_GROUP_DELETED, Map.of("id", GROUP_ID), Boolean.TRUE, "ROLE_DEV");

        assertThat(scimGroupAuditMapper.map(auditInvocation)).containsExactly(
            Map.entry("groupId", "7"), Map.entry("name", "ROLE_DEV"));
    }

    @Test
    void testShouldRecordSkipsSuccessfulNoOpResults() {
        assertThat(scimGroupAuditMapper.shouldRecord(createSuccessInvocation(Optional.empty()))).isFalse();
        assertThat(scimGroupAuditMapper.shouldRecord(createSuccessInvocation(Boolean.FALSE))).isFalse();
    }

    @Test
    void testShouldRecordAcceptsSuccessfulResultsAndFailures() {
        AuditInvocation errorAuditInvocation = new AuditInvocation(
            ScimGroupAuditEvents.SCIM_GROUP_DELETED, Map.of("id", GROUP_ID), null, null, AuditOutcome.ERROR,
            IllegalStateException.class);

        assertThat(scimGroupAuditMapper.shouldRecord(createSuccessInvocation(Boolean.TRUE))).isTrue();
        assertThat(scimGroupAuditMapper.shouldRecord(createSuccessInvocation(Optional.of(createAuthority("X")))))
            .isTrue();
        assertThat(scimGroupAuditMapper.shouldRecord(errorAuditInvocation)).isTrue();
    }

    private static AuditInvocation createAuditInvocation(
        String event, Map<String, Object> arguments, Object result, Object captured) {

        return new AuditInvocation(event, new HashMap<>(arguments), result, captured, AuditOutcome.SUCCESS, null);
    }

    private static AuditInvocation createSuccessInvocation(Object result) {
        return createAuditInvocation(ScimGroupAuditEvents.SCIM_GROUP_UPDATED, Map.of("id", GROUP_ID), result, null);
    }

    private static Authority createAuthority(String name) {
        Authority authority = new Authority();

        authority.setId(GROUP_ID);
        authority.setName(name);

        return authority;
    }

    private static ScimGroup createScimGroup(String displayName) {
        return new ScimGroup(List.of(ScimGroup.SCHEMA), null, displayName, List.of(), null);
    }
}
