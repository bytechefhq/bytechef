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

package com.bytechef.component.klaviyo;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.klaviyo.action.KlaviyoGetListsAction;
import com.bytechef.component.klaviyo.action.KlaviyoSubscribeProfilesAction;
import com.bytechef.component.klaviyo.action.KlaviyoUpdateProfileAction;
import com.bytechef.component.klaviyo.connection.KlaviyoConnection;
import com.google.auto.service.AutoService;

/**
 * @author Marija Horvat
 */
@AutoService(ComponentHandler.class)
public class KlaviyoComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("klaviyo")
        .title("Klaviyo")
        .description(
            "Klaviyo is a marketing automation platform primarily used for email and SMS marketing, especially by " +
                "e-commerce businesses.")
        .icon("path:assets/klaviyo.svg")
        .categories(ComponentCategory.MARKETING_AUTOMATION)
        .customAction(true)
        .connection(KlaviyoConnection.CONNECTION_DEFINITION)
        .actions(
            KlaviyoGetListsAction.ACTION_DEFINITION,
            KlaviyoSubscribeProfilesAction.ACTION_DEFINITION,
            KlaviyoUpdateProfileAction.ACTION_DEFINITION)
        .clusterElements(
            tool(KlaviyoGetListsAction.ACTION_DEFINITION),
            tool(KlaviyoSubscribeProfilesAction.ACTION_DEFINITION),
            tool(KlaviyoUpdateProfileAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
