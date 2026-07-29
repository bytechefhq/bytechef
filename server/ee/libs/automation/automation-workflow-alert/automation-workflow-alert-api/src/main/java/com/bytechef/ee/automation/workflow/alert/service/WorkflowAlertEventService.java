/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.alert.service;

import com.bytechef.ee.automation.workflow.alert.domain.WorkflowAlertEvent;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WorkflowAlertEventService {

    WorkflowAlertEvent create(WorkflowAlertEvent workflowAlertEvent);

    List<WorkflowAlertEvent> getWorkflowAlertEvents(long workspaceId);
}
