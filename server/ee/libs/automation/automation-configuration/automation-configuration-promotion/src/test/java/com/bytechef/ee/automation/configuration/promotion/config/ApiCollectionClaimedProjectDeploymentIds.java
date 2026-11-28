/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.promotion.config;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class ApiCollectionClaimedProjectDeploymentIds {

    private final Set<Long> projectDeploymentIds = ConcurrentHashMap.newKeySet();

    public void claim(long projectDeploymentId) {
        projectDeploymentIds.add(projectDeploymentId);
    }

    public void clear() {
        projectDeploymentIds.clear();
    }

    public String getConditionTemplate() {
        if (projectDeploymentIds.isEmpty()) {
            return "%1$s = 0";
        }

        return "%1$s IN (" + projectDeploymentIds.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(", ")) + ")";
    }
}
