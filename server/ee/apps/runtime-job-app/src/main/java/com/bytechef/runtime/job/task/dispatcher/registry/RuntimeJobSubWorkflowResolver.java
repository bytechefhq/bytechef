/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.runtime.job.task.dispatcher.registry;

import com.bytechef.platform.workflow.task.dispatcher.registry.SubWorkflowResolver;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
class RuntimeJobSubWorkflowResolver implements SubWorkflowResolver {

    @Override
    public String resolveWorkflowId(String workflowUuid) {
        return "";
    }
}
