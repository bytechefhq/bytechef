/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A released automation workflow project an administrator may provision for one connected user, with every enabled
 * configuration of the project in the connected user's environment.
 *
 * @param projectId               the project id
 * @param name                    the project name
 * @param projectVersion          the version of the preselected configuration: the assigned one, else the self-serve
 *                                pick, else the first
 * @param workflows               the provisionable workflows of the preselected configuration
 * @param configurations          every enabled configuration of the project in the connected user's environment,
 *                                whatever its permission expression says, in id order
 * @param assignedConfigurationId the configuration the connected user is assigned to, or {@code null} when unassigned
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ProvisionableAutomationWorkflowProjectDTO(
    long projectId, String name, int projectVersion, List<Workflow> workflows, List<Configuration> configurations,
    @Nullable Long assignedConfigurationId) {

    /**
     * A configuration the connected user may be provisioned onto.
     *
     * @param id             the configuration id
     * @param name           the configuration name
     * @param projectVersion the project version the configuration pins
     * @param selfServe      whether self-serve would place the connected user on this configuration
     * @param workflows      the workflows the configuration releases to the connected user
     *
     * @version ee
     *
     * @author Ivica Cardic
     */
    @SuppressFBWarnings("EI")
    public record Configuration(
        long id, String name, int projectVersion, boolean selfServe, List<Workflow> workflows) {
    }

    /**
     * @version ee
     */
    @SuppressFBWarnings("EI")
    public record Workflow(
        String workflowUuid, String label, List<ConnectedUserWorkflowTemplateDTO.Input> inputs, boolean provisioned,
        boolean dangling) {
    }
}
