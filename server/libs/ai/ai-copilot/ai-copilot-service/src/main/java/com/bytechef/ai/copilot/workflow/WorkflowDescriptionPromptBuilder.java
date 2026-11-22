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

package com.bytechef.ai.copilot.workflow;

import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class WorkflowDescriptionPromptBuilder {

    public String build(String workflowDefinition, String workflowNodeName) {
        StringBuilder builder = new StringBuilder();

        if (workflowNodeName == null) {
            builder.append(
                "You write a concise, human-readable description of an automation workflow based on its " +
                    "JSON definition (its triggers and tasks). Describe what the workflow does in 1-3 " +
                    "sentences. Return ONLY the description text, no preamble, no markdown, no code fences.\n\n");
            builder.append("Workflow definition:\n")
                .append(workflowDefinition);

            return builder.toString();
        }

        builder.append(
            "You write a short note describing a single step (node) of an automation workflow, based on the " +
                "workflow's JSON definition. Describe what the step named '")
            .append(workflowNodeName)
            .append("' does in 1-2 sentences. Return ONLY the note text, no preamble, no markdown, no code ")
            .append("fences.\n\n");
        builder.append("Workflow definition:\n")
            .append(workflowDefinition);

        return builder.toString();
    }
}
