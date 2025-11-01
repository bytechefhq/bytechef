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

package com.bytechef.component.linkedin;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.linkedin.action.LinkedInCreatePostAction;
import com.bytechef.component.linkedin.action.LinkedInDeletePostAction;
import com.bytechef.component.linkedin.connection.LinkedInConnection;
import com.bytechef.component.linkedin.trigger.LinkedInNewPostTrigger;
import com.google.auto.service.AutoService;

/**
 * @author Monika Kušter
 */
@AutoService(ComponentHandler.class)
public final class LinkedInComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("linkedin")
        .title("LinkedIn")
        .description(
            "LinkedIn is a professional networking platform that enables users to connect with colleagues, discover " +
                "job opportunities, and share industry-related content.")
        .customAction(true)
        .icon("path:assets/linkedin.svg")
        .categories(ComponentCategory.COMMUNICATION, ComponentCategory.SOCIAL_MEDIA)
        .connection(LinkedInConnection.CONNECTION_DEFINITION)
        .actions(
            LinkedInCreatePostAction.ACTION_DEFINITION,
            LinkedInDeletePostAction.ACTION_DEFINITION)
        .triggers(LinkedInNewPostTrigger.TRIGGER_DEFINITION)
        .clusterElements(
            tool(LinkedInCreatePostAction.ACTION_DEFINITION),
            tool(LinkedInDeletePostAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
