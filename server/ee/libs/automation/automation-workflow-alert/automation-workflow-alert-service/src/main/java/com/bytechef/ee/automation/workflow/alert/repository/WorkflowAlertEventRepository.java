/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.alert.repository;

import com.bytechef.ee.automation.workflow.alert.domain.WorkflowAlertEvent;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface WorkflowAlertEventRepository extends ListCrudRepository<WorkflowAlertEvent, Long> {

    List<WorkflowAlertEvent> findTop100ByWorkflowAlertRuleIdInOrderByCreatedDateDesc(List<Long> workflowAlertRuleIds);
}
