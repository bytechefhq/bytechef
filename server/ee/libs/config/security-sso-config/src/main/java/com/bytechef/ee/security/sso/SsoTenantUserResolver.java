/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.security.sso;

import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.security.web.authentication.TenantUser;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class SsoTenantUserResolver {

    private static final Logger log = LoggerFactory.getLogger(SsoTenantUserResolver.class);

    private final TenantService tenantService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public SsoTenantUserResolver(TenantService tenantService, UserService userService) {
        this.tenantService = tenantService;
        this.userService = userService;
    }

    public Optional<TenantUser> resolveTenantUser(
        String identifier, Collection<? extends GrantedAuthority> authorities) {

        boolean email = EmailValidator.getInstance()
            .isValid(identifier);

        if (!tenantService.isMultiTenantEnabled()) {
            return fetchUser(identifier, email)
                .map(user -> createTenantUser(user, authorities, TenantContext.getCurrentTenantId()));
        }

        List<String> tenantIds = email
            ? tenantService.getTenantIdsByUserEmail(identifier) : tenantService.getTenantIdsByUserLogin(identifier);

        if (tenantIds.size() != 1) {
            log.warn("Refusing the SSO sign-in of {}: it matches users in {} tenants", identifier, tenantIds.size());

            return Optional.empty();
        }

        String tenantId = tenantIds.getFirst();

        return TenantContext.callWithTenantId(tenantId, () -> fetchUser(identifier, email))
            .map(user -> createTenantUser(user, authorities, tenantId));
    }

    private static TenantUser createTenantUser(
        User user, Collection<? extends GrantedAuthority> authorities, String tenantId) {

        return new TenantUser(user.getLogin(), "", authorities, tenantId);
    }

    private Optional<User> fetchUser(String identifier, boolean email) {
        if (email) {
            return userService.fetchUserByEmail(identifier);
        }

        return userService.fetchUserByLogin(identifier.toLowerCase(Locale.ENGLISH));
    }
}
