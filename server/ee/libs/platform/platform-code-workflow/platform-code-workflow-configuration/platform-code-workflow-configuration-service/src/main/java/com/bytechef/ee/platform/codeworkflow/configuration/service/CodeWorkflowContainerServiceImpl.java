/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.codeworkflow.configuration.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.repository.CodeWorkflowContainerRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.apache.commons.lang3.Validate;
import org.springframework.lang.NonNull;
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
public class CodeWorkflowContainerServiceImpl implements CodeWorkflowContainerService {

    private final CodeWorkflowContainerRepository codeWorkflowContainerRepository;

    public CodeWorkflowContainerServiceImpl(CodeWorkflowContainerRepository codeWorkflowContainerRepository) {
        this.codeWorkflowContainerRepository = codeWorkflowContainerRepository;
    }

    @Override
    public CodeWorkflowContainer create(@NonNull CodeWorkflowContainer codeWorkflowContainer) {
        Validate.notNull(codeWorkflowContainer, "'codeWorkflow' must not be null");
        Validate.notNull(codeWorkflowContainer.getWorkflowsFile(), "'workflowsFile' must not be null");
        Validate.isTrue(codeWorkflowContainer.getId() == null, "'id' must be null");
        Validate.notNull(codeWorkflowContainer.getName(), "'name' must not be null");

        return codeWorkflowContainerRepository.save(codeWorkflowContainer);
    }

    @Override
    public CodeWorkflowContainer getCodeWorkflowContainer(String codeWorkflowContainerReference) {
        return OptionalUtils.get(
            codeWorkflowContainerRepository.findByCodeWorkflowContainerReference(codeWorkflowContainerReference));
    }

}
