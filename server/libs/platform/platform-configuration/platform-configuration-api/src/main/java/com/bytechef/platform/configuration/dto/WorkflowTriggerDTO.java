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

package com.bytechef.platform.configuration.dto;

import com.bytechef.platform.configuration.domain.WorkflowConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record WorkflowTriggerDTO(
    List<WorkflowConnection> connections, String description, Map<String, ?> metadata, String name, String label,
    Map<String, ?> parameters, String timeout, String type) {

    public WorkflowTriggerDTO(WorkflowTrigger workflowTrigger, List<WorkflowConnection> connections) {
        this(
            connections, workflowTrigger.getDescription(), workflowTrigger.getMetadata(), workflowTrigger.getName(),
            workflowTrigger.getLabel(), workflowTrigger.getParameters(), workflowTrigger.getTimeout(),
            workflowTrigger.getType());
    }
}
