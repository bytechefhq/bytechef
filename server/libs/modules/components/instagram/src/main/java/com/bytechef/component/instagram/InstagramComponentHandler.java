/*
 * Copyright 2026 ByteChef
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

package com.bytechef.component.instagram;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.instagram.connection.InstagramConnection;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public final class InstagramComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("instagram")
        .title("Instagram")
        .description(
            "Instagram is a photo and video sharing social network. Publish posts and reels and manage comments " +
                "on an Instagram professional account.")
        .customAction(true)
        .customActionHelp(
            "", "https://developers.facebook.com/docs/instagram-platform/instagram-api-with-instagram-login")
        .icon("path:assets/instagram.svg")
        .categories(ComponentCategory.SOCIAL_MEDIA, ComponentCategory.MARKETING_AUTOMATION)
        .connection(InstagramConnection.CONNECTION_DEFINITION)
        .version(1);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
