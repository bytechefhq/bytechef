/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.event;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @param removedProvidedInputNames The input names whose value the configuration stopped providing in this change,
 *                                  keyed by workflow uuid. The rollout removes them from every connected user's
 *                                  workflow of that template, so the administrator's value does not stay behind.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record AutomationWorkflowProjectConfigurationUpdatedEvent(
    long projectId, long environmentId, Map<String, Set<String>> removedProvidedInputNames) {

    public AutomationWorkflowProjectConfigurationUpdatedEvent {
        removedProvidedInputNames = removedProvidedInputNames.entrySet()
            .stream()
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> Set.copyOf(entry.getValue())));
    }

    public AutomationWorkflowProjectConfigurationUpdatedEvent(long projectId, long environmentId) {
        this(projectId, environmentId, Map.of());
    }
}
