/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.runtime.job.task.dispatcher.registry;

import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.platform.workflow.task.dispatcher.map.MapDataSource;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
class RuntimeJobMapDataSource implements MapDataSource {

    @Override
    public @Nullable OutputResponse getLastIterateeTaskOutput(
        String workflowId, String lastTaskName, String lastTaskType, long environmentId) {

        throw new UnsupportedOperationException();
    }
}
