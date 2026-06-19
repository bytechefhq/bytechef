/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.permission;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiClientService;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * Maps an API-client id to its owning user, resolved from the client's {@code created_by} login. API clients are
 * user-owned and not workspace-scoped, so authorization is owner-isolation only (enforced in EE). Fails closed when the
 * client does not exist or its creator cannot be resolved to a user.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Component
public class ApiClientOwnershipResolver implements ResourceOwnershipResolver {

    private final ApiClientService apiClientService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public ApiClientOwnershipResolver(ApiClientService apiClientService, UserService userService) {
        this.apiClientService = apiClientService;
        this.userService = userService;
    }

    @Override
    public String resourceType() {
        return "ApiClient";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return apiClientService.fetchApiClient(id)
            .map(apiClient -> apiClient.getCreatedBy())
            .flatMap(userService::fetchUserByLogin)
            .map(user -> ResourceOwner.ofUser(user.getId()))
            .orElseGet(ResourceOwner::unknown);
    }
}
