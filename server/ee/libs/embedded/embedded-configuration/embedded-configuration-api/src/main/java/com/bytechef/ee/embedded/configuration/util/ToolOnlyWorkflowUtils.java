/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.util;

import com.bytechef.platform.component.constant.WorkflowConstants;
import com.bytechef.platform.configuration.dto.WorkflowDTO;
import com.bytechef.platform.configuration.dto.WorkflowTriggerDTO;
import com.bytechef.platform.definition.WorkflowNodeType;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class ToolOnlyWorkflowUtils {

    private ToolOnlyWorkflowUtils() {
    }

    public static boolean isToolOnly(@Nullable WorkflowDTO workflow) {
        if (workflow == null || workflow.getTriggers() == null) {
            return false;
        }

        boolean workflowCallTriggerFound = false;

        for (WorkflowTriggerDTO workflowTrigger : workflow.getTriggers()) {
            if (workflowTrigger.disabled()) {
                continue;
            }

            if (!isWorkflowCallTrigger(workflowTrigger)) {
                return false;
            }

            workflowCallTriggerFound = true;
        }

        return workflowCallTriggerFound;
    }

    private static boolean isWorkflowCallTrigger(WorkflowTriggerDTO workflowTrigger) {
        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.type());

        return Objects.equals(workflowNodeType.name(), WorkflowConstants.WORKFLOW) &&
            Objects.equals(workflowNodeType.operation(), WorkflowConstants.NEW_WORKFLOW_CALL);
    }
}
