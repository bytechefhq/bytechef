/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.atlas.configuration.repository.git.operations;

import com.bytechef.atlas.configuration.workflow.mapper.WorkflowResource;
import java.util.List;

/**
 * @version ee
 *
 * @author Arik Cohen
 */
public interface GitWorkflowOperations {

    List<WorkflowResource> getHeadFiles();

    WorkflowResource getFile(String fileId);
}
