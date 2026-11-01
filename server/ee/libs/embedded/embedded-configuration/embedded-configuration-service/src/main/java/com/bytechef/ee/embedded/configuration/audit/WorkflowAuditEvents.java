/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.audit;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class WorkflowAuditEvents {

    public static final String AUTOMATION_WORKFLOW_PERMISSION_EXPRESSION_UPDATED =
        "AUTOMATION_WORKFLOW_PERMISSION_EXPRESSION_UPDATED";
    public static final String INTEGRATION_WORKFLOW_PERMISSION_EXPRESSION_UPDATED =
        "INTEGRATION_WORKFLOW_PERMISSION_EXPRESSION_UPDATED";

    private WorkflowAuditEvents() {
    }
}
