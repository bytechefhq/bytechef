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

package com.bytechef.component.vercel;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.vercel.action.VercelCancelDeploymentAction;
import com.bytechef.component.vercel.action.VercelCreateDeploymentAction;
import com.bytechef.component.vercel.action.VercelGetDeploymentAction;
import com.bytechef.component.vercel.action.VercelListProjectsAction;
import com.bytechef.component.vercel.connection.VercelConnection;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class VercelComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("vercel")
        .title("Vercel")
        .description("Vercel is a cloud platform that builds, deploys and hosts web applications from Git.")
        .icon("path:assets/vercel.svg")
        .categories(ComponentCategory.DEVELOPER_TOOLS)
        .connection(VercelConnection.CONNECTION_DEFINITION)
        .customAction(true)
        .customActionHelp("", "https://vercel.com/docs/rest-api")
        .actions(
            VercelListProjectsAction.ACTION_DEFINITION, VercelCreateDeploymentAction.ACTION_DEFINITION,
            VercelGetDeploymentAction.ACTION_DEFINITION, VercelCancelDeploymentAction.ACTION_DEFINITION)
        .clusterElements(
            tool(VercelListProjectsAction.ACTION_DEFINITION), tool(VercelCreateDeploymentAction.ACTION_DEFINITION),
            tool(VercelGetDeploymentAction.ACTION_DEFINITION), tool(VercelCancelDeploymentAction.ACTION_DEFINITION))
        .version(1);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
