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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import com.bytechef.automation.ai.agent.service.AiAgentService;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AiAgentProjectPublishPreListenerTest {

    private static final long PROJECT_ID = 5L;

    private final AiAgentFacade aiAgentFacade = mock(AiAgentFacade.class);
    private final AiAgentService aiAgentService = mock(AiAgentService.class);
    private final AiAgentProjectPublishPreListener aiAgentProjectPublishPreListener =
        new AiAgentProjectPublishPreListener(aiAgentFacade, aiAgentService);

    @Test
    void testPublishingAProjectWithoutAgentsSkipsAgentPreparation() {
        when(aiAgentService.getProjectAgents(PROJECT_ID)).thenReturn(List.of());

        aiAgentProjectPublishPreListener.onBeforePublishProject(PROJECT_ID);

        verify(aiAgentFacade, never()).prepareProjectPublish(anyLong());
    }

    @Test
    void testPublishingAProjectWithAgentsPreparesThem() {
        when(aiAgentService.getProjectAgents(PROJECT_ID)).thenReturn(List.of(mock(AiAgent.class)));

        aiAgentProjectPublishPreListener.onBeforePublishProject(PROJECT_ID);

        verify(aiAgentFacade).prepareProjectPublish(PROJECT_ID);
    }
}
