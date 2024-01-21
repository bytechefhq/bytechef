/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.workflow.test.event;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.platform.workflow.test.facade.WorkflowTestConfigurationFacade;
import org.springframework.data.relational.core.mapping.event.AbstractRelationalEventListener;
import org.springframework.data.relational.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Component;

@Component
public class WorkflowAfterSaveEventListener extends AbstractRelationalEventListener<Workflow> {

    private final WorkflowTestConfigurationFacade workflowTestConfigurationFacade;

    public WorkflowAfterSaveEventListener(WorkflowTestConfigurationFacade workflowTestConfigurationFacade) {
        this.workflowTestConfigurationFacade = workflowTestConfigurationFacade;
    }

    @Override
    protected void onAfterSave(AfterSaveEvent<Workflow> afterSaveEvent) {
        Workflow workflow = afterSaveEvent.getEntity();

        workflowTestConfigurationFacade.updateWorkflowTestConfiguration(workflow);
    }
}
