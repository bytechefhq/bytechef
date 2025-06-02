/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.platform.constant.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Set;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record IntegrationInstanceDTO(
    long connectedUserId, long connectionId, String createdBy, Instant createdDate, boolean enabled,
    Environment environment, Long id, long integrationInstanceConfigurationId,
    IntegrationInstanceConfiguration integrationInstanceConfiguration,
    Set<IntegrationInstanceWorkflowDTO> integrationInstanceWorkflows, Instant lastExecutionDate, String lastModifiedBy,
    Instant lastModifiedDate, int version) {

    public IntegrationInstanceDTO(
        IntegrationInstance integrationInstance, IntegrationInstanceConfiguration integrationInstanceConfiguration,
        Set<IntegrationInstanceWorkflowDTO> integrationInstanceWorkflows, Instant lastExecutionDate) {

        this(
            integrationInstance.getConnectedUserId(), integrationInstance.getConnectionId(),
            integrationInstance.getCreatedBy(), integrationInstance.getCreatedDate(), integrationInstance.isEnabled(),
            integrationInstanceConfiguration.getEnvironment(), integrationInstance.getId(),
            integrationInstance.getIntegrationInstanceConfigurationId(), integrationInstanceConfiguration,
            integrationInstanceWorkflows, lastExecutionDate, integrationInstance.getLastModifiedBy(),
            integrationInstance.getLastModifiedDate(), integrationInstance.getVersion());
    }
}
