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

package com.bytechef.embedded.configuration.service;

import com.bytechef.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.embedded.configuration.repository.IntegrationInstanceRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class IntegrationInstanceServiceImpl implements IntegrationInstanceService {

    private final IntegrationInstanceRepository integrationInstanceRepository;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceServiceImpl(IntegrationInstanceRepository integrationInstanceRepository) {
        this.integrationInstanceRepository = integrationInstanceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationInstance> getConnectedUserIntegrationInstances(long connectedUserId) {
        return integrationInstanceRepository.findAllByConnectedUserId(connectedUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationInstance> getConnectedUserIntegrationInstances(List<Long> connectedUserIds) {
        return integrationInstanceRepository.findAllByConnectedUserIdIn(connectedUserIds);
    }

    @Override
    @Transactional(readOnly = true)
    public IntegrationInstance getIntegrationInstance(long id) {
        return integrationInstanceRepository.findById(id)
            .orElseThrow();
    }

    @Override
    public void updateEnabled(long id, boolean enable) {
        IntegrationInstance integrationInstance = getIntegrationInstance(id);

        integrationInstance.setEnabled(enable);

        integrationInstanceRepository.save(integrationInstance);
    }

    @Override
    public IntegrationInstanceWorkflow updateWorkflowEnabled(
        long id, long integrationInstanceConfigurationWorkflowId, boolean enable) {

        IntegrationInstance integrationInstance = getIntegrationInstance(id);

        IntegrationInstanceWorkflow integrationInstanceWorkflow = integrationInstance.updateWorkflowEnabled(
            integrationInstanceConfigurationWorkflowId, enable);

        integrationInstanceRepository.save(integrationInstance);

        return integrationInstanceWorkflow;
    }
}
