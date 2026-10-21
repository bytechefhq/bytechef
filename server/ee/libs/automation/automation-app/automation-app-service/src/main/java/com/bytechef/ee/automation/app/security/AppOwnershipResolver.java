/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.security;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.OptionalLong;
import org.springframework.stereotype.Component;

/**
 * Maps an App id to its owning workspace and owner user, both read from the app row. Fails closed: an unknown app
 * resolves to {@link ResourceOwner#unknown()}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AppOwnershipResolver implements ResourceOwnershipResolver {

    private final AppService appService;

    @SuppressFBWarnings("EI")
    public AppOwnershipResolver(AppService appService) {
        this.appService = appService;
    }

    @Override
    public String resourceType() {
        return "App";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return appService.fetchApp(id)
            .map(app -> ResourceOwner.of(toOptionalLong(app.getWorkspaceId()), toOptionalLong(app.getOwnerUserId())))
            .orElseGet(ResourceOwner::unknown);
    }

    private static OptionalLong toOptionalLong(Long value) {
        return value == null ? OptionalLong.empty() : OptionalLong.of(value);
    }
}
