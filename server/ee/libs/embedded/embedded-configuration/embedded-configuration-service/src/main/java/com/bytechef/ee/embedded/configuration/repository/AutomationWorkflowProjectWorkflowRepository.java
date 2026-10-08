/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.repository;

import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProjectWorkflow;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface AutomationWorkflowProjectWorkflowRepository
    extends ListCrudRepository<AutomationWorkflowProjectWorkflow, Long> {

    void deleteByProjectId(long projectId);

    List<AutomationWorkflowProjectWorkflow> findAllByProjectId(long projectId);

    Optional<AutomationWorkflowProjectWorkflow> findByWorkflowUuid(UUID workflowUuid);
}
