/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.security;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicy;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Declares the rungs an App supports. {@code ORGANIZATION} is deliberately absent: an App belongs to one workspace and
 * promoting it organization-wide would surface it in other workspaces' lists.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AppVisibilityPolicy implements ResourceVisibilityPolicy {

    @Override
    public String resourceType() {
        return "App";
    }

    @Override
    public ResourceVisibility defaultVisibility() {
        return ResourceVisibility.WORKSPACE;
    }

    @Override
    public Set<ResourceVisibility> supportedVisibilities() {
        return Set.of(ResourceVisibility.PRIVATE, ResourceVisibility.WORKSPACE);
    }
}
