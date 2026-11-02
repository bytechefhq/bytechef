/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record AutomationWorkflowProjectConfigurationWorkflowProvidableValuesDTO(
    List<ConnectionSlot> connectionSlots, List<Input> inputs, Map<String, Long> providedConnections,
    Map<String, Object> providedInputs) {

    public record ConnectionSlot(
        String componentName, String title, boolean required, List<ProvidableConnection> providableConnections) {
    }

    public record ProvidableConnection(long id, String name) {
    }

    /**
     * An input an administrator can provide.
     *
     * @param componentName     the referenced component when the input is a component input group reference
     * @param componentVersion  the referenced component version
     * @param groupName         the referenced component input group
     * @param optionConnections the administrator's connections of the referenced component, used to load the input's
     *                          options
     */
    public record Input(
        String name, @Nullable String label, @Nullable String type, boolean required, @Nullable String componentName,
        @Nullable Integer componentVersion, @Nullable String groupName, List<ProvidableConnection> optionConnections) {

        public Input(String name, @Nullable String label, @Nullable String type, boolean required) {
            this(name, label, type, required, null, null, null, List.of());
        }
    }

    public record InputOption(@Nullable String label, String value, @Nullable String description) {
    }
}
