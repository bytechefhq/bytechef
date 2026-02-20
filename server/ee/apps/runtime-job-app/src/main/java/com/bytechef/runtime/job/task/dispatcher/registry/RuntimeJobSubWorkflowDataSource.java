/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.runtime.job.task.dispatcher.registry;

import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.task.dispatcher.registry.SubWorkflowDataSource;
import com.bytechef.platform.workflow.task.dispatcher.registry.domain.SubWorkflowEntry;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
class RuntimeJobSubWorkflowDataSource implements SubWorkflowDataSource {

    @Override
    public @Nullable OutputResponse getSubWorkflowInputSchema(String workflowUuid) {
        return null;
    }

    @Override
    public @Nullable OutputResponse getSubWorkflowOutputSchema(String workflowUuid) {
        return null;
    }

    @Override
    public List<SubWorkflowEntry> getSubWorkflows(PlatformType platformType, String search) {
        return List.of();
    }
}
