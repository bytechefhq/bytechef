/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.execution.cost.repository;

import com.bytechef.ee.automation.workflow.execution.cost.domain.WorkflowExecutionCost;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface WorkflowExecutionCostRepository extends ListCrudRepository<WorkflowExecutionCost, Long> {

    Optional<WorkflowExecutionCost> findByJobId(Long jobId);
}
