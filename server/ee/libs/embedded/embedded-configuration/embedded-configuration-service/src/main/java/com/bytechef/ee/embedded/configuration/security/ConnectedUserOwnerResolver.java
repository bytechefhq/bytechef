/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.owner.Owner;
import com.bytechef.platform.owner.OwnerResolver;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.security.web.authentication.PrincipalEnvironment;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Maps a job principal onto the connected user behind it. The two platform types reach the same answer by different
 * routes: an embedded job's principal is an integration instance, which names its connected user directly, while the
 * automation bridge's principal is a project deployment, whose connected user is found through
 * {@code connected_user_project}.
 *
 * <p>
 * A project deployment with no {@code ConnectedUserProject} is the vendor's own automation workflow. That is the common
 * case, and it resolves to no owner rather than to a denial.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ConnectedUserOwnerResolver implements OwnerResolver {

    private final ConnectedUserProjectService connectedUserProjectService;
    private final ConnectedUserService connectedUserService;
    private final IntegrationInstanceService integrationInstanceService;

    public ConnectedUserOwnerResolver(
        ConnectedUserProjectService connectedUserProjectService, ConnectedUserService connectedUserService,
        IntegrationInstanceService integrationInstanceService) {

        this.connectedUserProjectService = connectedUserProjectService;
        this.connectedUserService = connectedUserService;
        this.integrationInstanceService = integrationInstanceService;
    }

    @Override
    public Optional<Owner> resolveJobPrincipal(long jobPrincipalId, PlatformType platformType) {
        if (platformType == PlatformType.EMBEDDED) {
            IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
                jobPrincipalId);

            Long connectedUserId = integrationInstance.getConnectedUserId();

            return connectedUserId == null ? Optional.empty() : Optional.of(Owner.connectedUser(connectedUserId));
        }

        return connectedUserProjectService.fetchConnectedUserId(jobPrincipalId)
            .map(Owner::connectedUser);
    }

    /**
     * Reads the same pair {@code ConnectedUserResourceMembershipResolver} reads -- the login and the environment off
     * the current security context -- so the owner an editor run is scoped to and the principal it is authorised as
     * cannot drift apart. Both are context reads; the only query is the connected-user lookup itself.
     */
    @Override
    public Optional<Owner> resolveCurrentPrincipal() {
        Optional<String> externalUserIdOptional = SecurityUtils.fetchCurrentUserLogin();

        if (externalUserIdOptional.isEmpty()) {
            return Optional.empty();
        }

        Optional<Long> environmentIdOptional = PrincipalEnvironment.fetchCurrentPrincipalEnvironmentId();

        if (environmentIdOptional.isEmpty()) {
            return Optional.empty();
        }

        return connectedUserService.fetchConnectedUser(externalUserIdOptional.get(), environmentIdOptional.get())
            .map(connectedUser -> Owner.connectedUser(connectedUser.getId()));
    }
}
