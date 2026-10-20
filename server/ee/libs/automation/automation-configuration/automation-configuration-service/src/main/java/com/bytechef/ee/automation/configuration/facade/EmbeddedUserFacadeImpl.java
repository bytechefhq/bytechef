/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import com.bytechef.ee.automation.configuration.audit.EmbeddedUserAuditEvents;
import com.bytechef.ee.automation.configuration.audit.EmbeddedUserAuditMapper;
import com.bytechef.ee.automation.configuration.domain.EmbeddedUser;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.ee.automation.configuration.service.EmbeddedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Embedded-wide membership changes need {@code EMBEDDED_MEMBER_MANAGE} in every environment, because they grant or
 * revoke a role everywhere at once; the per-environment ones need it only in the environment they change.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class EmbeddedUserFacadeImpl implements EmbeddedUserFacade {

    private final EmbeddedUserService embeddedUserService;

    @SuppressFBWarnings("EI")
    public EmbeddedUserFacadeImpl(EmbeddedUserService embeddedUserService) {
        this.embeddedUserService = embeddedUserService;
    }

    @Override
    @Audited(event = EmbeddedUserAuditEvents.EMBEDDED_USER_ADDED, mapper = EmbeddedUserAuditMapper.class)
    @PreAuthorize("hasEmbeddedScopeInEveryEnvironment('EMBEDDED_MEMBER_MANAGE')")
    public EmbeddedUser addEmbeddedUser(long userId, WorkspaceRole embeddedRole) {
        return embeddedUserService.addEmbeddedUser(userId, embeddedRole);
    }

    @Override
    @PreAuthorize("hasEmbeddedScope('EMBEDDED_VIEW')")
    @Transactional(readOnly = true)
    public List<EmbeddedUser> getEmbeddedUsers() {
        return embeddedUserService.getEmbeddedUsers();
    }

    @Override
    @Audited(
        event = EmbeddedUserAuditEvents.EMBEDDED_USER_ENVIRONMENT_ROLE_REMOVED, mapper = EmbeddedUserAuditMapper.class)
    @PreAuthorize("hasEmbeddedScopeInEnvironment('EMBEDDED_MEMBER_MANAGE', #environment)")
    public void removeEnvironmentRole(long userId, Environment environment) {
        embeddedUserService.removeEnvironmentRole(userId, environment);
    }

    @Override
    @Audited(event = EmbeddedUserAuditEvents.EMBEDDED_USER_REMOVED, mapper = EmbeddedUserAuditMapper.class)
    @PreAuthorize("hasEmbeddedScopeInEveryEnvironment('EMBEDDED_MEMBER_MANAGE')")
    public boolean removeEmbeddedUser(long userId) {
        return embeddedUserService.removeEmbeddedUser(userId);
    }

    @Override
    @Audited(
        event = EmbeddedUserAuditEvents.EMBEDDED_USER_ENVIRONMENT_ROLE_UPDATED, mapper = EmbeddedUserAuditMapper.class)
    @PreAuthorize("hasEmbeddedScopeInEnvironment('EMBEDDED_MEMBER_MANAGE', #environment)")
    public EmbeddedUser setEnvironmentRole(long userId, Environment environment, WorkspaceRole embeddedRole) {
        return embeddedUserService.setEnvironmentRole(userId, environment, embeddedRole);
    }

    @Override
    @Audited(event = EmbeddedUserAuditEvents.EMBEDDED_USER_ROLE_UPDATED, mapper = EmbeddedUserAuditMapper.class)
    @PreAuthorize("hasEmbeddedScopeInEveryEnvironment('EMBEDDED_MEMBER_MANAGE')")
    public EmbeddedUser updateEmbeddedUserRole(long userId, WorkspaceRole embeddedRole) {
        return embeddedUserService.updateEmbeddedUserRole(userId, embeddedRole);
    }
}
