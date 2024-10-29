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

package com.bytechef.component.image.helper;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.image.helper.action.ImageHelperCropImageAction;
import com.bytechef.component.image.helper.action.ImageHelperImageToBase64Action;
import com.bytechef.component.image.helper.action.ImageHelperResizeImageAction;
import com.bytechef.component.image.helper.action.ImageHelperRotateImageAction;
import com.google.auto.service.AutoService;

/**
 * @author Monika Kušter
 */
@AutoService(ComponentHandler.class)
public class ImageHelperComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("imageHelper")
        .title("Image Helper")
        .description("Helper component which contains various actions for image manipulation.")
        .icon("path:assets/image-helper.svg")
        .categories(ComponentCategory.HELPERS)
        .actions(
            ImageHelperCropImageAction.ACTION_DEFINITION,
            ImageHelperImageToBase64Action.ACTION_DEFINITION,
            ImageHelperResizeImageAction.ACTION_DEFINITION,
            ImageHelperRotateImageAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
