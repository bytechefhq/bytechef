/*
 * Copyright 2025 ByteChef
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

package com.bytechef.automation.ai.agent.event;

import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import com.bytechef.automation.ai.agent.service.AiAgentService;
import com.bytechef.automation.configuration.listener.ProjectPublishPreListener;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class AiAgentProjectPublishPreListener implements ProjectPublishPreListener {

    private final AiAgentFacade aiAgentFacade;
    private final AiAgentService aiAgentService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiAgentProjectPublishPreListener(@Lazy AiAgentFacade aiAgentFacade, AiAgentService aiAgentService) {
        this.aiAgentFacade = aiAgentFacade;
        this.aiAgentService = aiAgentService;
    }

    @Override
    public void onBeforePublishProject(long projectId) {
        List<AiAgent> projectAgents = aiAgentService.getProjectAgents(projectId);

        if (projectAgents.isEmpty()) {
            return;
        }

        aiAgentFacade.prepareProjectPublish(projectId);
    }
}
