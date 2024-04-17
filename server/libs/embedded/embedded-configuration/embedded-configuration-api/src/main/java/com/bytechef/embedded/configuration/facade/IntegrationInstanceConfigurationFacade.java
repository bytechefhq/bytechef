/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.embedded.configuration.facade;

import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.embedded.configuration.dto.IntegrationInstanceConfigurationDTO;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface IntegrationInstanceConfigurationFacade {

    IntegrationInstanceConfigurationDTO createIntegrationInstanceConfiguration(
        IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO);

    long createIntegrationInstanceConfigurationWorkflowJob(Long id, String workflowId);

    void deleteIntegrationInstanceConfiguration(long id);

    void enableIntegrationInstanceConfiguration(long id, boolean enable);

    void enableIntegrationInstanceConfigurationWorkflow(
        long integrationInstanceConfigurationId, String workflowId, boolean enable);

    IntegrationInstanceConfigurationDTO getIntegrationInstanceConfiguration(long id);

    List<Tag> getIntegrationInstanceConfigurationTags();

    List<IntegrationInstanceConfigurationDTO> getIntegrationInstanceConfigurations(
        Environment environment, Long integrationId, Long tagId);

    IntegrationInstanceConfigurationDTO updateIntegrationInstanceConfiguration(
        IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO);

    void updateIntegrationInstanceConfigurationTags(long id, List<Tag> tags);

    IntegrationInstanceConfigurationWorkflow updateIntegrationInstanceConfigurationWorkflow(
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow);
}
