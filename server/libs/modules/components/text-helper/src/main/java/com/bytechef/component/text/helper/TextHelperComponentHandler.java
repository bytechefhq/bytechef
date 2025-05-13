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

package com.bytechef.component.text.helper;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.text.helper.action.TextHelperBase64EncodeDecodeAction;
import com.bytechef.component.text.helper.action.TextHelperConcatenateAction;
import com.bytechef.component.text.helper.action.TextHelperContainsAction;
import com.bytechef.component.text.helper.action.TextHelperExtractContentFromHtmlAction;
import com.bytechef.component.text.helper.action.TextHelperExtractUrlsAction;
import com.bytechef.component.text.helper.action.TextHelperFormatCurrencyAction;
import com.bytechef.component.text.helper.action.TextHelperHTMLToMarkdownAction;
import com.bytechef.component.text.helper.action.TextHelperLowerCaseAction;
import com.bytechef.component.text.helper.action.TextHelperMarkdownToHTMLAction;
import com.bytechef.component.text.helper.action.TextHelperReplaceAction;
import com.bytechef.component.text.helper.action.TextHelperSelectFirstNCharactersAction;
import com.bytechef.component.text.helper.action.TextHelperSelectLastNCharactersAction;
import com.bytechef.component.text.helper.action.TextHelperSplitAction;
import com.bytechef.component.text.helper.action.TextHelperTrimWhitespaceAction;
import com.bytechef.component.text.helper.action.TextHelperUpperCaseAction;
import com.bytechef.component.text.helper.action.TextHelperUrlEncodeDecodeAction;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class TextHelperComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("textHelper")
        .title("Text Helper")
        .description("Helper component which contains operations to help you work with text.")
        .icon("path:assets/text-helper.svg")
        .categories(ComponentCategory.HELPERS)
        .actions(
            TextHelperBase64EncodeDecodeAction.ACTION_DEFINITION,
            TextHelperConcatenateAction.ACTION_DEFINITION,
            TextHelperContainsAction.ACTION_DEFINITION,
            TextHelperExtractContentFromHtmlAction.ACTION_DEFINITION,
            TextHelperExtractUrlsAction.ACTION_DEFINITION,
            TextHelperFormatCurrencyAction.ACTION_DEFINITION,
            TextHelperHTMLToMarkdownAction.ACTION_DEFINITION,
            TextHelperLowerCaseAction.ACTION_DEFINITION,
            TextHelperMarkdownToHTMLAction.ACTION_DEFINITION,
            TextHelperReplaceAction.ACTION_DEFINITION,
            TextHelperSelectFirstNCharactersAction.ACTION_DEFINITION,
            TextHelperSelectLastNCharactersAction.ACTION_DEFINITION,
            TextHelperSplitAction.ACTION_DEFINITION,
            TextHelperTrimWhitespaceAction.ACTION_DEFINITION,
            TextHelperUpperCaseAction.ACTION_DEFINITION,
            TextHelperUrlEncodeDecodeAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
