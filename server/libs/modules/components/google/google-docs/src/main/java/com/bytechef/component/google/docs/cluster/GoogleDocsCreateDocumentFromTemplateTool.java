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

package com.bytechef.component.google.docs.cluster;

import static com.bytechef.component.definition.ai.agent.ToolFunction.TOOLS;
import static com.bytechef.component.google.docs.action.GoogleDocsCreateDocumentFromTemplateAction.PROPERTIES;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ai.agent.SingleConnectionToolFunction;
import com.bytechef.component.google.docs.action.GoogleDocsCreateDocumentFromTemplateAction;

/**
 * @author Monika Kušter
 */
public class GoogleDocsCreateDocumentFromTemplateTool {

    public static final ClusterElementDefinition<SingleConnectionToolFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<SingleConnectionToolFunction>clusterElement("createDocumentFromTemplate")
            .title("Create Document From Template")
            .description(
                "Creates a new document based on an existing one and can replace any placeholder variables found in " +
                    "your template document, like [[name]], [[email]], etc.")
            .type(TOOLS)
            .properties(PROPERTIES)
            .output()
            .object(() -> GoogleDocsCreateDocumentFromTemplateAction::perform);
}
