/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.ConfigurationMoveImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.ConfigurationMoveResultDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationConnectedUserDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationDTO;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface IntegrationInstanceConfigurationFacade {

    long createIntegrationInstanceConfiguration(
        IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO);

    long createIntegrationInstanceConfigurationWorkflowJob(Long id, String workflowId);

    void deleteIntegrationInstanceConfiguration(long id);

    ConfigurationMoveImpactDTO getMoveImpact(long sourceId, long targetId, List<Long> connectedUserIds);

    void enableIntegrationInstanceConfiguration(long id, boolean enable);

    void enableIntegrationInstanceConfigurationWorkflow(
        long integrationInstanceConfigurationId, String workflowId, boolean enable);

    /**
     * Makes a published integration version live in an environment: creates and enables an instance configuration when
     * the integration has none there, otherwise moves the existing one to the version and enables it.
     *
     * @return the id of the instance configuration
     */
    long enableIntegrationVersion(long integrationId, int integrationVersion, Environment environment);

    IntegrationInstanceConfigurationDTO getIntegrationInstanceConfigurationIntegration(
        long integrationId, boolean enabled, Environment environment, @Nullable ConnectedUser connectedUser);

    List<IntegrationInstanceConfigurationDTO> getIntegrationInstanceConfigurationIntegrations(
        boolean enabled, Environment environment, @Nullable ConnectedUser connectedUser);

    IntegrationInstanceConfigurationDTO getIntegrationInstanceConfiguration(long id);

    List<IntegrationInstanceConfigurationConnectedUserDTO> getIntegrationInstanceConfigurationConnectedUsers(long id);

    List<Tag> getIntegrationInstanceConfigurationTags();

    ConfigurationMoveImpactDTO getUpgradeImpact(long targetId, List<Long> connectedUserIds);

    List<IntegrationInstanceConfigurationDTO> getIntegrationInstanceConfigurations(
        Long environmentId, Long integrationId, Long tagId, boolean includeAllFields);

    ConfigurationMoveResultDTO moveConnectedUsers(long sourceId, long targetId, List<Long> connectedUserIds);

    void updateIntegrationInstanceConfiguration(
        IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO);

    void updateIntegrationInstanceConfigurationTags(long id, List<Tag> tags);

    void updateIntegrationInstanceConfigurationWorkflow(
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow);

    ConfigurationMoveResultDTO upgradeConnectedUsers(long targetId, List<Long> connectedUserIds);
}
