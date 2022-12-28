
/*
 * Copyright 2021 <your company/name>.
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
 */

package com.bytechef.atlas.repository.resource;

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.repository.workflow.mapper.WorkflowMapper;

/**
 * @author Ivica Cardic
 */
public class FilesystemResourceWorkflowRepository extends AbstractResourceWorkflowRepository {

    public FilesystemResourceWorkflowRepository(String locationPattern, WorkflowMapper workflowMapper) {
        super(String.format("file:%s", locationPattern), workflowMapper);
    }

    @Override
    public Workflow.SourceType getSourceType() {
        return Workflow.SourceType.FILESYSTEM;
    }
}
