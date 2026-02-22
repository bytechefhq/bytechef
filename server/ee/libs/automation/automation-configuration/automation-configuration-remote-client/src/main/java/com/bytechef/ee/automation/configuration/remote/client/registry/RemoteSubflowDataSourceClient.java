/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.registry;

import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
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
@ConditionalOnEEVersion
public class RemoteSubflowDataSourceClient implements SubflowDataSource {

    @Override
    public @Nullable OutputResponse getSubWorkflowInputSchema(String workflowUuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable OutputResponse getSubWorkflowOutputSchema(String workflowUuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<SubflowEntry> getSubWorkflows(PlatformType platformType, String search) {
        throw new UnsupportedOperationException();
    }
}
