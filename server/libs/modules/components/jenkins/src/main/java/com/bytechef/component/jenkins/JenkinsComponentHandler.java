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

package com.bytechef.component.jenkins;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.jenkins.action.JenkinsCreateJobAction;
import com.bytechef.component.jenkins.connection.JenkinsConnection;
import com.bytechef.component.jenkins.trigger.JenkinsNewJobStatusNotificationTrigger;
import com.google.auto.service.AutoService;

/**
 * @author Nikolina Spehar
 */
@AutoService(ComponentHandler.class)
public class JenkinsComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("jenkins")
        .title("Jenkins")
        .description(
            "Jenkins is leading open source automation server, Jenkins provides hundreds of plugins to support " +
                "building, deploying and automating any project.")
        .icon("path:assets/jenkins.svg")
        .categories(ComponentCategory.DEVELOPER_TOOLS)
        .actions(JenkinsCreateJobAction.ACTION_DEFINITION)
        .clusterElements(tool(JenkinsCreateJobAction.ACTION_DEFINITION))
        .connection(JenkinsConnection.CONNECTION_DEFINITION)
        .triggers(JenkinsNewJobStatusNotificationTrigger.TRIGGER_DEFINITION)
        .customAction(true);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
