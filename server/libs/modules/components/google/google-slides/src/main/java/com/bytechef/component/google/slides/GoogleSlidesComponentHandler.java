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

package com.bytechef.component.google.slides;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;
import static com.bytechef.component.google.slides.connection.GoogleSlidesConnection.CONNECTION_DEFINITION;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.google.slides.action.GoogleSlidesCreatePresentationBasedOnTemplateAction;
import com.google.auto.service.AutoService;

/**
 * @author Monika Kušter
 */
@AutoService(ComponentHandler.class)
public class GoogleSlidesComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("googleSlides")
        .title("Google Slides")
        .description(
            "Google Slides is a cloud-based presentation software that allows users to create, edit, and " +
                "collaborate on presentations online in real-time.")
        .customAction(true)
        .customActionHelp("", "https://developers.google.com/workspace/slides/api/reference/rest")
        .icon("path:assets/google-slides.svg")
        .categories(ComponentCategory.FILE_STORAGE)
        .connection(CONNECTION_DEFINITION)
        .actions(GoogleSlidesCreatePresentationBasedOnTemplateAction.ACTION_DEFINITION)
        .clusterElements(tool(GoogleSlidesCreatePresentationBasedOnTemplateAction.ACTION_DEFINITION))
        .version(1);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
