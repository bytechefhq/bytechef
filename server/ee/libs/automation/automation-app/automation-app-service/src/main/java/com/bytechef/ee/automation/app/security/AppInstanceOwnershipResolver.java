/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.security;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.ee.automation.app.service.AppInstanceService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.OptionalLong;
import org.springframework.stereotype.Component;

/**
 * Maps an AppInstance id to the instance's own workspace and owner user, never its app's: an instance in another
 * workspace is governed by that workspace. Fails closed: an unknown instance resolves to
 * {@link ResourceOwner#unknown()}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AppInstanceOwnershipResolver implements ResourceOwnershipResolver {

    private final AppInstanceService appInstanceService;

    @SuppressFBWarnings("EI")
    public AppInstanceOwnershipResolver(AppInstanceService appInstanceService) {
        this.appInstanceService = appInstanceService;
    }

    @Override
    public String resourceType() {
        return "AppInstance";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return appInstanceService.fetchAppInstance(id)
            .map(appInstance -> ResourceOwner.of(
                toOptionalLong(appInstance.getWorkspaceId()), toOptionalLong(appInstance.getOwnerUserId())))
            .orElseGet(ResourceOwner::unknown);
    }

    private static OptionalLong toOptionalLong(Long value) {
        return value == null ? OptionalLong.empty() : OptionalLong.of(value);
    }
}
