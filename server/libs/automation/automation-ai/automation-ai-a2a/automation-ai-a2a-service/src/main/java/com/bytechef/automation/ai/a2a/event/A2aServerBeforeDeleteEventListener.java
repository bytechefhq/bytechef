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

package com.bytechef.automation.ai.a2a.event;

import com.bytechef.automation.ai.a2a.domain.A2aProject;
import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.facade.A2aProjectFacade;
import com.bytechef.automation.ai.a2a.service.A2aProjectService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.data.relational.core.mapping.event.AbstractRelationalEventListener;
import org.springframework.data.relational.core.mapping.event.BeforeDeleteEvent;
import org.springframework.data.relational.core.mapping.event.Identifier;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class A2aServerBeforeDeleteEventListener extends AbstractRelationalEventListener<A2aServer> {

    private final A2aProjectFacade a2aProjectFacade;
    private final A2aProjectService a2aProjectService;

    @SuppressFBWarnings("EI")
    public A2aServerBeforeDeleteEventListener(A2aProjectFacade a2aProjectFacade, A2aProjectService a2aProjectService) {
        this.a2aProjectFacade = a2aProjectFacade;
        this.a2aProjectService = a2aProjectService;
    }

    @Override
    protected void onBeforeDelete(BeforeDeleteEvent<A2aServer> beforeDeleteEvent) {
        Identifier identifier = beforeDeleteEvent.getId();

        for (A2aProject a2aProject : a2aProjectService.getA2aServerA2aProjects((Long) identifier.getValue())) {
            a2aProjectFacade.deleteA2aProject(a2aProject.getId());
        }
    }
}
