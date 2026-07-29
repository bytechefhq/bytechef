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

package com.bytechef.component.daytona;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.daytona.action.DaytonaCreateSandboxAction;
import com.bytechef.component.daytona.action.DaytonaDeleteSandboxAction;
import com.bytechef.component.daytona.action.DaytonaExecuteCodeAction;
import com.bytechef.component.daytona.action.DaytonaUploadFileAction;
import com.bytechef.component.daytona.connection.DaytonaConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * Registers the Daytona component: a secure sandbox for running AI-generated code. The {@code executeCode} action is
 * also exposed as an AI Agent tool (TOOLS cluster element) so an agent can generate and run code on the fly.
 *
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class DaytonaComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("daytona")
        .title("Daytona")
        .description(
            "Daytona provides secure and elastic infrastructure for running AI-generated code in isolated " +
                "sandboxes.")
        .icon("path:assets/daytona.svg")
        .categories(ComponentCategory.DEVELOPER_TOOLS, ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(DaytonaConnection.CONNECTION_DEFINITION)
        .actions(
            DaytonaExecuteCodeAction.ACTION_DEFINITION,
            DaytonaCreateSandboxAction.ACTION_DEFINITION,
            DaytonaDeleteSandboxAction.ACTION_DEFINITION,
            DaytonaUploadFileAction.ACTION_DEFINITION)
        .clusterElements(
            tool(DaytonaExecuteCodeAction.ACTION_DEFINITION),
            tool(DaytonaCreateSandboxAction.ACTION_DEFINITION),
            tool(DaytonaDeleteSandboxAction.ACTION_DEFINITION),
            tool(DaytonaUploadFileAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
