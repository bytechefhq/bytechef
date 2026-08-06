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

    /**
     * The task parameter carrying the job context snapshot — the workflow's inputs plus every prior task's output. The
     * deploy path writes it as the {@code =#root} formula so the engine evaluates it into a map before dispatch; see
     * {@code CodeWorkflowContainerFacadeImpl}.
     */
    public static final String INPUT = "input";

    /**
     * The task parameter naming the tasks that run at the same time as this one — the other members of its parallel
     * group, or the tasks in a fork/join group's other branches. Their output cannot be read, and this is what lets the
     * failure say so instead of reporting an unknown name.
     */
    public static final String CONCURRENT_TASK_NAMES = "concurrentTaskNames";
    public static final String PERFORM = "perform";

    /**
     * The parameter keys the deploy path writes onto a task node to dispatch it. Everything else on the node is the
     * task's own declared configuration, which is what {@code TaskContext.parameters()} hands back.
     */
    public static final Set<String> DISPATCH_PARAMETERS = Set.of(
        "codeWorkflowContainerUuid", "workflowName", "taskName", INPUT, CONCURRENT_TASK_NAMES, "type");
}
