/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.codeworkflow.constant;

import java.util.Set;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CodeWorkflowConstants {

    public static final String CODE_WORKFLOW = "codeWorkflow";

    public static final String INPUT = "input";

    public static final String CONCURRENT_TASK_NAMES = "concurrentTaskNames";
    public static final String PERFORM = "perform";

    public static final Set<String> DISPATCH_PARAMETERS = Set.of(
        "codeWorkflowContainerUuid", "workflowName", "taskName", INPUT, CONCURRENT_TASK_NAMES, "type");
}
