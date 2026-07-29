/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.alert.repository;

import com.bytechef.ee.automation.workflow.alert.domain.WorkflowAlertRule;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface WorkflowAlertRuleRepository extends ListCrudRepository<WorkflowAlertRule, Long> {

    List<WorkflowAlertRule> findAllByRuleTypeAndEnabledTrue(int ruleType);
}
