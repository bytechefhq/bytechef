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

package com.bytechef.platform.configuration.web.graphql;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import java.util.List;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * Exposes Workflow over GraphQL.
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class WorkflowGraphQLController {

    @SchemaMapping(typeName = "Workflow", field = "triggers")
    public List<WorkflowTrigger> triggers(Workflow workflow) {

        return WorkflowTrigger.of(workflow);
    }
}
