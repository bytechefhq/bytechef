/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationDTO;
import com.bytechef.platform.constant.Environment;
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

    IntegrationInstanceConfigurationDTO getIntegrationInstanceConfigurationIntegration(
        long integrationId, boolean enabled, Environment environment);

    List<IntegrationInstanceConfigurationDTO> getIntegrationInstanceConfigurationIntegrations(
        boolean enabled, Environment environment);

    IntegrationInstanceConfigurationDTO getIntegrationInstanceConfiguration(long id);

    List<Tag> getIntegrationInstanceConfigurationTags();

    List<IntegrationInstanceConfigurationDTO> getIntegrationInstanceConfigurations(
        Environment environment, Long integrationId, Long tagId, boolean includeAllFields);

    void updateIntegrationInstanceConfiguration(
        IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO);

    void updateIntegrationInstanceConfigurationTags(long id, List<Tag> tags);

    void updateIntegrationInstanceConfigurationWorkflow(
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow);
}
