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
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record AutomationProjectConfigurationUpdatedEvent(
    long projectId, long environmentId, long configurationId, Map<String, Set<String>> removedProvidedInputNames) {

    public AutomationProjectConfigurationUpdatedEvent {
        removedProvidedInputNames = removedProvidedInputNames.entrySet()
            .stream()
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> Set.copyOf(entry.getValue())));
    }

    public AutomationProjectConfigurationUpdatedEvent(long projectId, long environmentId,
        long configurationId) {
        this(projectId, environmentId, configurationId, Map.of());
    }
}
