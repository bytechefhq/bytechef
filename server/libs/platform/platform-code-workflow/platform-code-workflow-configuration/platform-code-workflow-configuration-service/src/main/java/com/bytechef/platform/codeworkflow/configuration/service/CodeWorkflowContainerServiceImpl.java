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

package com.bytechef.platform.codeworkflow.configuration.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.platform.codeworkflow.configuration.repository.CodeWorkflowContainerRepository;
import org.apache.commons.lang3.Validate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
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
