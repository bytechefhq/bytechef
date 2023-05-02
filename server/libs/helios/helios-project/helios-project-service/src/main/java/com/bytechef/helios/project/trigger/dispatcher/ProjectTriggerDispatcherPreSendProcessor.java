
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

package com.bytechef.helios.project.trigger.dispatcher;

import com.bytechef.helios.project.dispatcher.AbstractDispatcherPreSendProcessor;
import com.bytechef.helios.project.service.ProjectInstanceWorkflowService;
import com.bytechef.hermes.connection.WorkflowConnection;
import com.bytechef.hermes.constant.MetadataConstants;
import com.bytechef.hermes.trigger.TriggerDispatcherPreSendProcessor;
import com.bytechef.hermes.domain.TriggerExecution;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ProjectTriggerDispatcherPreSendProcessor extends AbstractDispatcherPreSendProcessor
    implements TriggerDispatcherPreSendProcessor {

    @SuppressFBWarnings("EI")
    public ProjectTriggerDispatcherPreSendProcessor(ProjectInstanceWorkflowService projectInstanceWorkflowService) {
        super(projectInstanceWorkflowService);
    }

    @Override
    public TriggerExecution process(TriggerExecution triggerExecution) {
        triggerExecution.putMetadata(
            MetadataConstants.CONNECTION_IDS,
            getConnectionIdMap(WorkflowConnection.of(triggerExecution.getWorkflowTrigger())));

        return triggerExecution;
    }
}
