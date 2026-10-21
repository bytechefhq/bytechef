/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.user.facade;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.user.constant.UserConstants;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.exception.InvalidPasswordException;
import com.bytechef.platform.user.exception.OrganizationDeletionConfirmationException;
import com.bytechef.platform.user.exception.OrganizationDeletionNotAvailableException;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.domain.TenantDeletion;
import com.bytechef.tenant.service.TenantDeletionService;
import com.bytechef.tenant.service.TenantService;
import com.bytechef.tenant.teardown.TenantTeardownContributor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@ConditionalOnCoordinator
class OrganizationDeletionFacadeImpl implements OrganizationDeletionFacade {

    private static final Logger log = LoggerFactory.getLogger(OrganizationDeletionFacadeImpl.class);

    private final ApplicationProperties applicationProperties;
    private final ObjectProvider<Clock> clockProvider;
    private final List<TenantTeardownContributor> tenantTeardownContributors;
    private final ObjectProvider<TenantDeletionService> tenantDeletionServiceProvider;
    private final PasswordEncoder passwordEncoder;
    private final TenantService tenantService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    OrganizationDeletionFacadeImpl(
        ApplicationProperties applicationProperties, PasswordEncoder passwordEncoder,
        ObjectProvider<TenantDeletionService> tenantDeletionServiceProvider,
        List<TenantTeardownContributor> tenantTeardownContributors, TenantService tenantService,
        UserService userService, ObjectProvider<Clock> clockProvider) {

        this.applicationProperties = applicationProperties;
        this.clockProvider = clockProvider;
        this.passwordEncoder = passwordEncoder;
        this.tenantDeletionServiceProvider = tenantDeletionServiceProvider;
        this.tenantTeardownContributors = tenantTeardownContributors;
        this.tenantService = tenantService;
        this.userService = userService;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public Instant requestDeletion(String confirmation, String password) {
        String tenantId = TenantContext.getCurrentTenantId();

        TenantDeletionService tenantDeletionService = tenantDeletionServiceProvider.getIfAvailable();

        if (tenantDeletionService == null || !tenantService.isMultiTenantEnabled() ||
            Objects.equals(tenantId, TenantContext.DEFAULT_TENANT_ID)) {

            throw new OrganizationDeletionNotAvailableException();
        }

        User user = userService.getCurrentUser();

        validateConfirmation(user, confirmation);
        validatePassword(user, password);

        Instant purgeAfter = Instant.now(clockProvider.getIfAvailable(Clock::systemUTC));

        purgeAfter = purgeAfter.plus(applicationProperties.getTenant()
            .getDeletion()
            .getGracePeriod());

        if (!tenantDeletionService.markPendingDeletion(tenantId, user.getLogin(), purgeAfter)) {
            return tenantDeletionService.fetchTenantDeletion(tenantId)
                .map(TenantDeletion::purgeAfter)
                .orElse(purgeAfter);
        }

        List<String> failures = new ArrayList<>();

        for (TenantTeardownContributor tenantTeardownContributor : tenantTeardownContributors) {
            failures.addAll(runContributor(tenantTeardownContributor, tenantId));
        }

        if (!failures.isEmpty()) {
            tenantDeletionService.recordTeardownFailures(tenantId, failures);
        }

        return purgeAfter;
    }

    private static List<String> runContributor(TenantTeardownContributor tenantTeardownContributor, String tenantId) {
        try {
            List<String> contributorFailures = TenantContext.callWithTenantId(
                tenantId, () -> SecurityUtils.runAsSystem(
                    () -> tenantTeardownContributor.onDeletionRequested(tenantId)));

            return contributorFailures == null ? List.of() : contributorFailures;
        } catch (RuntimeException runtimeException) {
            Throwable cause = runtimeException.getCause() == null ? runtimeException : runtimeException.getCause();

            log.error("Teardown contributor {} failed for tenant {}", tenantTeardownContributor, tenantId, cause);

            return List.of(
                tenantTeardownContributor.getClass()
                    .getSimpleName() + ": " + cause.getMessage());
        }
    }

    private static void validateConfirmation(User user, String confirmation) {
        String expected = Objects.requireNonNullElse(user.getEmail(), user.getLogin());

        if (!StringUtils.equalsIgnoreCase(StringUtils.trim(confirmation), expected)) {
            throw new OrganizationDeletionConfirmationException();
        }
    }

    private void validatePassword(User user, String password) {
        String authProvider = user.getAuthProvider();

        if (authProvider != null && !authProvider.equals(UserConstants.AUTH_PROVIDER_LOCAL)) {
            return;
        }

        if (password == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidPasswordException();
        }
    }
}
