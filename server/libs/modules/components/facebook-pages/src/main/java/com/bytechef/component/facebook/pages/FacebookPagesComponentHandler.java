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

package com.bytechef.component.facebook.pages;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.facebook.pages.action.FacebookPagesCreatePhotoPostAction;
import com.bytechef.component.facebook.pages.action.FacebookPagesCreatePostAction;
import com.bytechef.component.facebook.pages.action.FacebookPagesCreateVideoPostAction;
import com.bytechef.component.facebook.pages.action.FacebookPagesGetCommentsAction;
import com.bytechef.component.facebook.pages.connection.FacebookPagesConnection;
import com.bytechef.component.facebook.pages.trigger.FacebookPagesNewCommentTrigger;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public final class FacebookPagesComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("facebookPages")
        .title("Facebook Pages")
        .description(
            "Facebook Pages lets businesses, brands and public figures publish posts, photos and videos and " +
                "engage with their audience through comments.")
        .customAction(true)
        .customActionHelp("", "https://developers.facebook.com/docs/pages-api")
        .icon("path:assets/facebook-pages.svg")
        .categories(ComponentCategory.SOCIAL_MEDIA, ComponentCategory.MARKETING_AUTOMATION)
        .connection(FacebookPagesConnection.CONNECTION_DEFINITION)
        .actions(
            FacebookPagesCreatePostAction.ACTION_DEFINITION,
            FacebookPagesCreatePhotoPostAction.ACTION_DEFINITION,
            FacebookPagesCreateVideoPostAction.ACTION_DEFINITION,
            FacebookPagesGetCommentsAction.ACTION_DEFINITION)
        .triggers(FacebookPagesNewCommentTrigger.TRIGGER_DEFINITION)
        .clusterElements(
            tool(FacebookPagesCreatePostAction.ACTION_DEFINITION),
            tool(FacebookPagesCreatePhotoPostAction.ACTION_DEFINITION),
            tool(FacebookPagesCreateVideoPostAction.ACTION_DEFINITION),
            tool(FacebookPagesGetCommentsAction.ACTION_DEFINITION))
        .version(1);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
