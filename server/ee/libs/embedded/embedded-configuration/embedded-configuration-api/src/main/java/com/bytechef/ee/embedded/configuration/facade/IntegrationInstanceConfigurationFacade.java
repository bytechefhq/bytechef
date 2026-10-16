/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationDTO;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;

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

    void enableIntegrationInstanceConfiguration(long id, boolean enable);

    void enableIntegrationInstanceConfigurationWorkflow(
        long integrationInstanceConfigurationId, String workflowId, boolean enable);

    /**
     * Makes a published integration version live in an environment: creates and enables an instance configuration when
     * the integration has none there, otherwise moves the existing one to the version and enables it. Workflows new to
     * the configuration are enabled too, except those needing a connection an administrator has yet to pick.
     *
     * @return the id of the instance configuration
     */
    long enableIntegrationVersion(long integrationId, int integrationVersion, Environment environment);

    IntegrationInstanceConfigurationDTO getIntegrationInstanceConfigurationIntegration(
        long integrationId, boolean enabled, Environment environment);

    List<IntegrationInstanceConfigurationDTO> getIntegrationInstanceConfigurationIntegrations(
        boolean enabled, Environment environment);

    IntegrationInstanceConfigurationDTO getIntegrationInstanceConfiguration(long id);

    List<Tag> getIntegrationInstanceConfigurationTags();

    List<IntegrationInstanceConfigurationDTO> getIntegrationInstanceConfigurations(
        Long environmentId, Long integrationId, Long tagId, boolean includeAllFields);

    void updateIntegrationInstanceConfiguration(
        IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO);

    void updateIntegrationInstanceConfigurationTags(long id, List<Tag> tags);

    void updateIntegrationInstanceConfigurationWorkflow(
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow);
}
