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

package com.bytechef.automation.configuration.dto;

import com.bytechef.platform.component.domain.ComponentDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ProjectTemplateDTO(
    List<ComponentDefinitionTuple> components, String description, boolean exported, ProjectInfo project,
    Integer projectVersion, String publicUrl, List<WorkflowInfo> workflows) {

    public ProjectTemplateDTO(boolean exported) {
        this(List.of(), null, exported, null, null, null, List.of());
    }

    public record ComponentDefinitionTuple(String key, List<ComponentDefinition> value) {
    }

    public record ProjectInfo(String name, String description) {
    }

    public record WorkflowInfo(String id, String label, String description) {
    }
}
