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

package com.bytechef.component.google.mail.cluster;

import static com.bytechef.component.definition.ai.agent.ToolFunction.TOOLS;
import static com.bytechef.component.google.mail.action.GoogleMailGetThreadAction.PROPERTIES;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.GET_THREAD;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.GET_THREAD_DESCRIPTION;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.GET_THREAD_TITLE;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ai.agent.SingleConnectionToolFunction;
import com.bytechef.component.google.mail.action.GoogleMailGetThreadAction;

/**
 * @author Monika Kušter
 */
public class GoogleMailGetThreadTool {

    public static final ClusterElementDefinition<SingleConnectionToolFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<SingleConnectionToolFunction>clusterElement(GET_THREAD)
            .title(GET_THREAD_TITLE)
            .description(GET_THREAD_DESCRIPTION)
            .type(TOOLS)
            .properties(PROPERTIES)
            .output(GoogleMailGetThreadAction::getOutput)
            .object(() -> GoogleMailGetThreadAction::perform);
}
