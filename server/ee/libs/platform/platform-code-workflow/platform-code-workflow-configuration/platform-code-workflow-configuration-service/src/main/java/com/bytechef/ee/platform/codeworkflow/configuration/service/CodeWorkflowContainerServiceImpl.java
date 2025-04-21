/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.codeworkflow.configuration.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.repository.CodeWorkflowContainerRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class CodeWorkflowContainerServiceImpl implements CodeWorkflowContainerService {

    private final CodeWorkflowContainerRepository codeWorkflowContainerRepository;

    public CodeWorkflowContainerServiceImpl(CodeWorkflowContainerRepository codeWorkflowContainerRepository) {
        this.codeWorkflowContainerRepository = codeWorkflowContainerRepository;
    }

    @Override
    public CodeWorkflowContainer create(CodeWorkflowContainer codeWorkflowContainer) {
        Assert.notNull(codeWorkflowContainer, "'codeWorkflow' must not be null");
        Assert.notNull(codeWorkflowContainer.getWorkflowsFile(), "'workflowsFile' must not be null");
        Assert.isTrue(codeWorkflowContainer.getId() == null, "'id' must be null");
        Assert.notNull(codeWorkflowContainer.getName(), "'name' must not be null");

        return codeWorkflowContainerRepository.save(codeWorkflowContainer);
    }

    @Override
    public CodeWorkflowContainer getCodeWorkflowContainer(String codeWorkflowContainerReference) {
        return OptionalUtils.get(
            codeWorkflowContainerRepository.findByCodeWorkflowContainerReference(codeWorkflowContainerReference));
    }

}
