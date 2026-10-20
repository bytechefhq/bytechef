/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.config;

import static org.mockito.Mockito.mock;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import org.mockito.Answers;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class TenantAdminOnlyPermissionService {

    private TenantAdminOnlyPermissionService() {
    }

    public static PermissionService create() {
        return mock(PermissionService.class, invocation -> {
            Class<?> returnType = invocation.getMethod()
                .getReturnType();

            if (returnType != boolean.class) {
                return Answers.RETURNS_DEFAULTS.answer(invocation);
            }

            String methodName = invocation.getMethod()
                .getName();

            if (methodName.equals("isAuthorizationSkipped")) {
                return false;
            }

            return SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN);
        });
    }
}
