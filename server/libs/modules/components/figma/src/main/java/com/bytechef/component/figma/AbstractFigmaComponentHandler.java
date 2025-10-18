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

package com.bytechef.component.figma;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.figma.action.FigmaGetCommentsAction;
import com.bytechef.component.figma.action.FigmaPostCommentAction;
import com.bytechef.component.figma.connection.FigmaConnection;
import com.bytechef.component.figma.trigger.FigmaNewCommentTrigger;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractFigmaComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("figma")
            .title("Figma")
            .description(
                "Figma is a cloud-based design and prototyping tool that enables teams to collaborate in real-time on user interface and user experience projects."))
                    .actions(modifyActions(FigmaGetCommentsAction.ACTION_DEFINITION,
                        FigmaPostCommentAction.ACTION_DEFINITION))
                    .connection(modifyConnection(FigmaConnection.CONNECTION_DEFINITION))
                    .clusterElements(modifyClusterElements(tool(FigmaGetCommentsAction.ACTION_DEFINITION),
                        tool(FigmaPostCommentAction.ACTION_DEFINITION)))
                    .triggers(FigmaNewCommentTrigger.TRIGGER_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
