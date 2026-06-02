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

package com.bytechef.ai.mcp.tool.platform;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Platform-level workflow authoring instruction tools.
 *
 * @author Marko Kriskovic
 */
@Component
public class WorkflowInstructionTools {

    private static final String DEFAULT_DEFINITION = """
        {
            "label": "workflowName",
            "description": "workflowDescription",
            "inputs": [],
            "triggers": [
                {
                    "label": "Manual",
                    "name": "trigger_1",
                    "type": "manual/v1/manual"
                }
            ],
            "tasks": []
        }
        """;

    private final String clusterElementsInstructions;
    private final String scriptCodeInstructions;
    private final String workflowBuildInstructions;

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public WorkflowInstructionTools(
        @Value("classpath:instruction_script_code.txt") Resource scriptCodeInstructionsResource,
        @Value("classpath:instruction_cluster_elements.txt") Resource clusterElementsInstructionsResource,
        @Value("classpath:instruction_workflow_build.txt") Resource workflowBuildInstructionsResource) {

        this.scriptCodeInstructions = readResource(scriptCodeInstructionsResource);
        this.clusterElementsInstructions = readResource(clusterElementsInstructionsResource);
        this.workflowBuildInstructions = readResource(workflowBuildInstructionsResource);
    }

    @Tool(description = "Instructions for working with cluster elements")
    public String getClusterElementsInstructions() {
        return clusterElementsInstructions;
    }

    @Tool(description = "Instructions for writing custom code in Script component")
    public String getScriptCodeInstructions() {
        return scriptCodeInstructions;
    }

    @SuppressFBWarnings("VA")
    @Tool(description = "Instructions for building workflows")
    public String getWorkflowBuildInstructions() {
        return workflowBuildInstructions.formatted(DEFAULT_DEFINITION);
    }

    private static String readResource(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ioException) {
            throw new IllegalStateException("Failed to read resource: " + resource.getDescription(), ioException);
        }
    }
}
