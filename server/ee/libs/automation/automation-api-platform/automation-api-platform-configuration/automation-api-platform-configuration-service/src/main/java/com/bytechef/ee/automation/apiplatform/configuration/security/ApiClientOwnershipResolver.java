/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.security;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiClient;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiClientService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
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
            .map(ApiClient::getCreatedBy)
            .flatMap(userService::fetchUserByLogin)
            .map(User::getId)
            .map(ResourceOwner::ofUser)
            .orElseGet(ResourceOwner::unknown);
    }
}
