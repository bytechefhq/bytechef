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

package com.bytechef.automation.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.dto.AutomationGettingStartedDTO;
import com.bytechef.automation.configuration.dto.AutomationGettingStartedRunDTO;
import com.bytechef.automation.configuration.facade.AutomationGettingStartedFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class AutomationGettingStartedGraphQlController {

    private final AutomationGettingStartedFacade automationGettingStartedFacade;

    @SuppressFBWarnings("EI")
    public AutomationGettingStartedGraphQlController(AutomationGettingStartedFacade automationGettingStartedFacade) {
        this.automationGettingStartedFacade = automationGettingStartedFacade;
    }

    @QueryMapping
    public AutomationGettingStartedDTO automationGettingStarted(@Argument long workspaceId) {
        return automationGettingStartedFacade.getAutomationGettingStarted(workspaceId);
    }

    @QueryMapping
    public boolean automationGettingStartedCompleted(@Argument long workspaceId) {
        return automationGettingStartedFacade.isAutomationGettingStartedCompleted(workspaceId);
    }

    @MutationMapping
    public boolean dismissAutomationGettingStarted(@Argument long workspaceId) {
        automationGettingStartedFacade.dismissAutomationGettingStarted(workspaceId);

        return true;
    }

    @SchemaMapping(typeName = "AutomationGettingStartedRun", field = "date")
    public String date(AutomationGettingStartedRunDTO automationGettingStartedRun) {
        Instant date = automationGettingStartedRun.date();

        return date.toString();
    }

    @SchemaMapping(typeName = "AutomationGettingStartedRun", field = "status")
    public String status(AutomationGettingStartedRunDTO automationGettingStartedRun) {
        return automationGettingStartedRun.status()
            .name();
    }
}
