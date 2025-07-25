/*
 * Copyright 2016-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2025 ByteChef
 */

package com.bytechef.atlas.configuration.repository.git.operations;

import com.bytechef.atlas.configuration.workflow.mapper.WorkflowResource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * @author Arik Cohen
 */
public interface GitWorkflowOperations {

    HeadFiles getHeadFiles();

    WorkflowResource getFile(String fileId);

    List<String> getRemoteBranches();

    String write(List<WorkflowResource> workflowResources, String commitMessage);

    @SuppressFBWarnings("EI")
    record HeadFiles(List<WorkflowResource> workflowResources, GitInfo gitInfo) {
    }

    @SuppressFBWarnings("EI")
    record GitInfo(String commitHash, String message) {
    }
}
