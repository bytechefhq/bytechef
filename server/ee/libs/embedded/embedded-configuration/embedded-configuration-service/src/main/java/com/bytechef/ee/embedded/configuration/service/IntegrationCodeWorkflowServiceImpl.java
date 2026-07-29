/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationCodeWorkflow;
import com.bytechef.ee.embedded.configuration.repository.IntegrationCodeWorkflowRepository;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class IntegrationCodeWorkflowServiceImpl implements IntegrationCodeWorkflowService {

    private final IntegrationCodeWorkflowRepository integrationCodeWorkflowRepository;

    public IntegrationCodeWorkflowServiceImpl(IntegrationCodeWorkflowRepository integrationCodeWorkflowRepository) {
        this.integrationCodeWorkflowRepository = integrationCodeWorkflowRepository;
    }

    @Override
    public IntegrationCodeWorkflow create(CodeWorkflowContainer codeWorkflowContainer, Integration integration) {
        IntegrationCodeWorkflow integrationCodeWorkflow = new IntegrationCodeWorkflow();

        integrationCodeWorkflow.setCodeWorkflowContainer(codeWorkflowContainer);
        integrationCodeWorkflow.setIntegration(integration);
        integrationCodeWorkflow.setIntegrationVersion(integration.getLastIntegrationVersion());

        return integrationCodeWorkflowRepository.save(integrationCodeWorkflow);
    }

    @Override
    public Optional<IntegrationCodeWorkflow> fetchIntegrationCodeWorkflow(long integrationId) {
        return integrationCodeWorkflowRepository.findFirstByIntegrationIdOrderByIdDesc(integrationId);
    }

    @Override
    public List<Long> getCodeWorkflowIntegrationIds() {
        return integrationCodeWorkflowRepository.findDistinctIntegrationIds();
    }
}
