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

package com.bytechef.component.x;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.x.action.XCreatePostAction;
import com.bytechef.component.x.action.XDeletePostAction;
import com.bytechef.component.x.action.XLikePostAction;
import com.bytechef.component.x.action.XRepostPostAction;
import com.bytechef.component.x.action.XSendDirectMessageAction;
import com.bytechef.component.x.connection.XConnection;
import com.bytechef.component.x.trigger.XNewPostTrigger;
import com.google.auto.service.AutoService;

/**
 * @author Monika Kušter
 */
@AutoService(ComponentHandler.class)
public class XComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("x")
        .title("X")
        .description(
            "X (formerly known as Twitter) is a social media platform that enables users to share short messages, " +
                "known as posts or tweets, and interact with others in real time.")
        .icon("path:assets/x.svg")
        .categories(ComponentCategory.SOCIAL_MEDIA)
        .connection(XConnection.CONNECTION_DEFINITION)
        .actions(
            XCreatePostAction.ACTION_DEFINITION,
            XDeletePostAction.ACTION_DEFINITION,
            XLikePostAction.ACTION_DEFINITION,
            XRepostPostAction.ACTION_DEFINITION,
            XSendDirectMessageAction.ACTION_DEFINITION)
        .clusterElements(
            tool(XCreatePostAction.ACTION_DEFINITION),
            tool(XDeletePostAction.ACTION_DEFINITION),
            tool(XLikePostAction.ACTION_DEFINITION),
            tool(XRepostPostAction.ACTION_DEFINITION),
            tool(XSendDirectMessageAction.ACTION_DEFINITION))
        .triggers(XNewPostTrigger.TRIGGER_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
