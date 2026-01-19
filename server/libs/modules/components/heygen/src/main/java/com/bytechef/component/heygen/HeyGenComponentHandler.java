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

package com.bytechef.component.heygen;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.heygen.action.HeyGenGenerateVideoFromTemplateAction;
import com.bytechef.component.heygen.action.HeyGenTranslateVideoAction;
import com.bytechef.component.heygen.action.HeyGenUploadAssetAction;
import com.bytechef.component.heygen.connection.HeyGenConnection;
import com.bytechef.component.heygen.trigger.HeyGenVideoGenerationCompletedTrigger;
import com.bytechef.component.heygen.trigger.HeyGenVideoGenerationFailedTrigger;
import com.google.auto.service.AutoService;

/**
 * @author Marija Horvat
 */
@AutoService(ComponentHandler.class)
public class HeyGenComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("heyGen")
        .title("HeyGen")
        .description(
            "HeyGen is an AI Video Generator that lets you create explainer videos, marketing and sales promos, " +
                "product demos, training and onboarding content.")
        .icon("path:assets/heygen.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(HeyGenConnection.CONNECTION_DEFINITION)
        .actions(
            HeyGenGenerateVideoFromTemplateAction.ACTION_DEFINITION,
            HeyGenTranslateVideoAction.ACTION_DEFINITION,
            HeyGenUploadAssetAction.ACTION_DEFINITION)
        .clusterElements(
            tool(HeyGenGenerateVideoFromTemplateAction.ACTION_DEFINITION),
            tool(HeyGenTranslateVideoAction.ACTION_DEFINITION),
            tool(HeyGenUploadAssetAction.ACTION_DEFINITION))
        .triggers(
            HeyGenVideoGenerationCompletedTrigger.TRIGGER_DEFINITION,
            HeyGenVideoGenerationFailedTrigger.TRIGGER_DEFINITION)
        .customAction(true);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
