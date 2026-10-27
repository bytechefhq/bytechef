/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.remote.grpc;

import com.bytechef.atlas.configuration.workflow.contributor.WorkflowReservedWordContributor;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import java.util.List;

/**
 * Lets apps that rebuild workflows read remotely accept the platform's workflow definition properties, which are
 * otherwise contributed only where the platform configuration service runs.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class RemoteWorkflowReservedWordContributor implements WorkflowReservedWordContributor {

    @Override
    public List<String> getReservedWords() {
        return WorkflowExtConstants.WORKFLOW_RESERVED_WORDS;
    }
}
