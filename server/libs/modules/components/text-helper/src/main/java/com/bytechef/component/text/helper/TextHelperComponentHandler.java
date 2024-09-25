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

package com.bytechef.component.text.helper;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXT_HELPER;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.text.helper.action.TextHelperBase64DecodeAction;
import com.bytechef.component.text.helper.action.TextHelperConcatenateAction;
import com.bytechef.component.text.helper.action.TextHelperExtractContentFromHtmlAction;
import com.bytechef.component.text.helper.action.TextHelperFindAction;
import com.bytechef.component.text.helper.action.TextHelperReplaceAction;
import com.bytechef.component.text.helper.action.TextHelperSplitAction;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class TextHelperComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(TEXT_HELPER)
        .title("Text Helper")
        .description("Helper component which contains operations to help you work with text.")
        .icon("path:assets/text-helper.svg")
        .categories(ComponentCategory.HELPERS)
        .actions(
            TextHelperBase64DecodeAction.ACTION_DEFINITION,
            TextHelperConcatenateAction.ACTION_DEFINITION,
            TextHelperExtractContentFromHtmlAction.ACTION_DEFINITION,
            TextHelperFindAction.ACTION_DEFINITION,
            TextHelperReplaceAction.ACTION_DEFINITION,
            TextHelperSplitAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
