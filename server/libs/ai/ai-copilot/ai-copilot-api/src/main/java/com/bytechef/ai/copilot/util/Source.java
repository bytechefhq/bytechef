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

package com.bytechef.ai.copilot.util;

import java.util.Locale;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public enum Source {

    WORKFLOW_EDITOR,
    CODE_EDITOR,
    CONVERTER(WorkflowOwner.PROJECT, true),
    CLUSTER_ELEMENT,
    SKILLS,
    JSON_SCHEMA_BUILDER,
    SAMPLE_OUTPUT,
    WORKFLOW_EXECUTION,
    WORKFLOW_CODE_EDITOR,
    KNOWLEDGE_BASE,
    DATA_TABLE,
    AI_AGENT,
    DEPLOYMENT,
    PROJECT,
    PROJECT_DEPLOYMENT,
    API_COLLECTION,
    ASSET_FILE,
    COMPONENT_RULE,
    MCP_SERVER(WorkflowOwner.PROJECT, true),
    CODE_WORKFLOW,
    CODE_WORKFLOW_EMBEDDED(WorkflowOwner.INTEGRATION, false),
    WORKFLOW_EDITOR_EMBEDDED(WorkflowOwner.INTEGRATION, false),
    WORKFLOW_EXECUTION_EMBEDDED(WorkflowOwner.INTEGRATION, false);

    private final WorkflowOwner workflowOwner;
    private final boolean buildOnly;

    Source() {
        this(WorkflowOwner.PROJECT, false);
    }

    Source(WorkflowOwner workflowOwner, boolean buildOnly) {
        this.workflowOwner = workflowOwner;
        this.buildOnly = buildOnly;
    }

    public static Optional<Source> fromId(String id) {
        for (Source source : values()) {
            if (id.equals(source.id())) {
                return Optional.of(source);
            }
        }

        return Optional.empty();
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String agentId(Mode mode) {
        Mode effectiveMode = resolveMode(mode);

        String agentId = name() + "_" + effectiveMode.name();

        return agentId.toLowerCase(Locale.ROOT);
    }

    public Mode resolveMode(@Nullable Mode requestedMode) {
        if (buildOnly) {
            return Mode.BUILD;
        }

        if (requestedMode == null) {
            throw new IllegalArgumentException("Source " + name() + " requires a mode");
        }

        return requestedMode;
    }

    public boolean isBuildOnly() {
        return buildOnly;
    }

    public WorkflowOwner workflowOwner() {
        return workflowOwner;
    }

    public enum WorkflowOwner {

        PROJECT,

        INTEGRATION
    }
}
