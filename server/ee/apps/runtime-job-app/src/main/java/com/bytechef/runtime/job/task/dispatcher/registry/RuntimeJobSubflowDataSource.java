/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.runtime.job.task.dispatcher.registry;

import com.bytechef.definition.BaseProperty;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowDataSource;
import com.bytechef.platform.workflow.task.dispatcher.subflow.domain.SubflowEntry;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
class RuntimeJobSubflowDataSource implements SubflowDataSource {

    @Override
    public BaseProperty.@Nullable BaseValueProperty<?> getSubWorkflowInputSchema(String workflowUuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BaseProperty.@Nullable BaseValueProperty<?> getSubWorkflowOutputSchema(String workflowUuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<SubflowEntry> getSubWorkflows(PlatformType platformType, String triggerName, String search) {
        throw new UnsupportedOperationException();
    }
}
