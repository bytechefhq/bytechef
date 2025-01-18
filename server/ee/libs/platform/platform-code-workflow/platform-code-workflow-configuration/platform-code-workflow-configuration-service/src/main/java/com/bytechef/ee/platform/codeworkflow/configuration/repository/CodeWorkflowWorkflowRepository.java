/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.codeworkflow.configuration.repository;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.atlas.configuration.domain.Workflow.SourceType;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
@ConditionalOnEEVersion
public class CodeWorkflowWorkflowRepository implements WorkflowCrudRepository {

    private final CodeWorkflowContainerRepository codeWorkflowContainerRepository;
    private final CodeWorkflowFileStorage codeWorkflowFileStorage;

    @SuppressFBWarnings("EI")
    public CodeWorkflowWorkflowRepository(
        CodeWorkflowContainerRepository codeWorkflowContainerRepository,
        CodeWorkflowFileStorage codeWorkflowFileStorage) {

        this.codeWorkflowContainerRepository = codeWorkflowContainerRepository;
        this.codeWorkflowFileStorage = codeWorkflowFileStorage;
    }

    @Override
    public List<Workflow> findAll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Workflow> findById(String id) {
        return codeWorkflowContainerRepository.findByWorkflowId(id)
            .map(codeWorkflowContainer -> toWorkflow(codeWorkflowContainer, id));
    }

    @Override
    public SourceType getSourceType() {
        return SourceType.JDBC;
    }

    @Override
    public void deleteById(String id) {
    }

    @Override
    public Workflow save(Workflow workflow) {
        throw new UnsupportedOperationException();
    }

    private Workflow toWorkflow(CodeWorkflowContainer codeWorkflowContainer, String workflowId) {
        return new Workflow(
            workflowId,
            codeWorkflowFileStorage.readCodeWorkflowDefinition(
                codeWorkflowContainer.getDefinition(workflowId)),
            Format.JSON, codeWorkflowContainer.getLastModifiedDate(), Map.of());
    }
}
