/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.dto.AutomationProjectConfigurationConnectedUserDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectConfigurationDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectConfigurationDeleteImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectConfigurationVersionImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectConfigurationWorkflowProvidableValuesDTO;
import com.bytechef.ee.embedded.configuration.dto.ConfigurationMoveImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.ConfigurationMoveResultDTO;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AutomationProjectConfigurationFacade {

    ConfigurationMoveResultDTO changeConnectedUserVersion(
        long sourceId, long connectedUserId, int projectVersion, @Nullable Long targetId);

    long createConfiguration(
        long projectId, long environmentId, String name, @Nullable String permissionExpression, int projectVersion,
        boolean enabled, boolean automationHubVisible);

    void deleteConfiguration(long id);

    void enableConfiguration(long id, boolean enable);

    void enableConfigurationWorkflow(long id, String workflowUuid, boolean enable);

    void enableConnectedUserConfiguration(long connectedUserId, long projectId, boolean enable);

    AutomationProjectConfigurationDTO getConfiguration(long id);

    List<AutomationProjectConfigurationConnectedUserDTO> getConfigurationConnectedUsers(long id);

    List<AutomationProjectConfigurationDTO> getConfigurations(long projectId);

    List<AutomationProjectConfigurationDTO> getConfigurations(long projectId, long environmentId);

    AutomationProjectConfigurationWorkflowProvidableValuesDTO getConfigurationWorkflowProvidableValues(
        long configurationId, String workflowUuid, @Nullable Integer projectVersion);

    List<AutomationProjectConfigurationWorkflowProvidableValuesDTO.InputOption>
        getConfigurationWorkflowInputOptions(
            long configurationId, String workflowUuid, @Nullable Integer projectVersion, String inputName,
            String propertyName, long connectionId, Map<String, ?> lookupDependsOnValues, @Nullable String searchText);

    AutomationProjectConfigurationDeleteImpactDTO getDeleteImpact(long id);

    ConfigurationMoveImpactDTO getMoveImpact(
        long sourceId, long targetId, List<Long> connectedUserIds);

    ConfigurationMoveImpactDTO getUpgradeImpact(long targetId, List<Long> connectedUserIds);

    AutomationProjectConfigurationVersionImpactDTO getVersionImpact(long configurationId, int projectVersion);

    ConfigurationMoveResultDTO moveConnectedUsers(long sourceId, long targetId, List<Long> connectedUserIds);

    void updateConfiguration(
        long id, String name, @Nullable String permissionExpression, int projectVersion, boolean enabled,
        boolean automationHubVisible);

    void updateConfigurationWorkflow(
        long id, String workflowUuid, Map<String, Long> providedConnections, Map<String, Object> providedInputs);

    ConfigurationMoveResultDTO upgradeConnectedUsers(long targetId, List<Long> connectedUserIds);
}
