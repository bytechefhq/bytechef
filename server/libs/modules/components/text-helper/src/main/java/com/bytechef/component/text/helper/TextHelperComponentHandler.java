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
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.text.helper.action.TextHelperBase64EncodeDecodeAction;
import com.bytechef.component.text.helper.action.TextHelperChangeTypeAction;
import com.bytechef.component.text.helper.action.TextHelperConcatenateAction;
import com.bytechef.component.text.helper.action.TextHelperContainsAction;
import com.bytechef.component.text.helper.action.TextHelperEscapeCharactersAction;
import com.bytechef.component.text.helper.action.TextHelperExtractAllRegExAction;
import com.bytechef.component.text.helper.action.TextHelperExtractContentFromHtmlAction;
import com.bytechef.component.text.helper.action.TextHelperExtractKeyRegExAction;
import com.bytechef.component.text.helper.action.TextHelperExtractRegExAction;
import com.bytechef.component.text.helper.action.TextHelperExtractUrlsAction;
import com.bytechef.component.text.helper.action.TextHelperFormatCurrencyAction;
import com.bytechef.component.text.helper.action.TextHelperGetDomainFromEmailAction;
import com.bytechef.component.text.helper.action.TextHelperGetDomainFromUrlAction;
import com.bytechef.component.text.helper.action.TextHelperGetFirstMiddleLastNameAction;
import com.bytechef.component.text.helper.action.TextHelperGetTextAfterAction;
import com.bytechef.component.text.helper.action.TextHelperGetTextBeforeAction;
import com.bytechef.component.text.helper.action.TextHelperGetTextBetweenAction;
import com.bytechef.component.text.helper.action.TextHelperGetTextLengthAction;
import com.bytechef.component.text.helper.action.TextHelperHexEncodeDecodeAction;
import com.bytechef.component.text.helper.action.TextHelperHtmlToMarkdownAction;
import com.bytechef.component.text.helper.action.TextHelperIsDomainAction;
import com.bytechef.component.text.helper.action.TextHelperIsEmailAction;
import com.bytechef.component.text.helper.action.TextHelperIsNumericAction;
import com.bytechef.component.text.helper.action.TextHelperIsUrlAction;
import com.bytechef.component.text.helper.action.TextHelperLowerCaseAction;
import com.bytechef.component.text.helper.action.TextHelperMarkdownToHtmlAction;
import com.bytechef.component.text.helper.action.TextHelperMatchAction;
import com.bytechef.component.text.helper.action.TextHelperParseEmailAction;
import com.bytechef.component.text.helper.action.TextHelperParseEmailListAction;
import com.bytechef.component.text.helper.action.TextHelperParseUrlAction;
import com.bytechef.component.text.helper.action.TextHelperProperCaseAction;
import com.bytechef.component.text.helper.action.TextHelperRegularExpressionMatchTestAction;
import com.bytechef.component.text.helper.action.TextHelperRemoveCharactersAction;
import com.bytechef.component.text.helper.action.TextHelperRemoveSpecialCharactersAction;
import com.bytechef.component.text.helper.action.TextHelperReplaceAction;
import com.bytechef.component.text.helper.action.TextHelperSelectFirstNCharactersAction;
import com.bytechef.component.text.helper.action.TextHelperSelectLastNCharactersAction;
import com.bytechef.component.text.helper.action.TextHelperSentenceCaseAction;
import com.bytechef.component.text.helper.action.TextHelperShortenAction;
import com.bytechef.component.text.helper.action.TextHelperSplitAction;
import com.bytechef.component.text.helper.action.TextHelperStripHtmlTagsAction;
import com.bytechef.component.text.helper.action.TextHelperTrimWhitespaceAction;
import com.bytechef.component.text.helper.action.TextHelperUnderscoreAction;
import com.bytechef.component.text.helper.action.TextHelperUpperCaseAction;
import com.bytechef.component.text.helper.action.TextHelperUrlEncodeDecodeAction;
import com.bytechef.component.text.helper.action.TextHelperUrlEncodeDecodeKeyValuePairAction;
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
            TextHelperChangeTypeAction.ACTION_DEFINITION,
            TextHelperConcatenateAction.ACTION_DEFINITION,
            TextHelperContainsAction.ACTION_DEFINITION,
            TextHelperEscapeCharactersAction.ACTION_DEFINITION,
            TextHelperExtractAllRegExAction.ACTION_DEFINITION,
            TextHelperExtractContentFromHtmlAction.ACTION_DEFINITION,
            TextHelperExtractKeyRegExAction.ACTION_DEFINITION,
            TextHelperExtractRegExAction.ACTION_DEFINITION,
            TextHelperExtractUrlsAction.ACTION_DEFINITION,
            TextHelperFormatCurrencyAction.ACTION_DEFINITION,
            TextHelperGetDomainFromEmailAction.ACTION_DEFINITION,
            TextHelperGetDomainFromUrlAction.ACTION_DEFINITION,
            TextHelperGetFirstMiddleLastNameAction.ACTION_DEFINITION,
            TextHelperGetTextAfterAction.ACTION_DEFINITION,
            TextHelperGetTextBeforeAction.ACTION_DEFINITION,
            TextHelperGetTextBetweenAction.ACTION_DEFINITION,
            TextHelperGetTextLengthAction.ACTION_DEFINITION,
            TextHelperHexEncodeDecodeAction.ACTION_DEFINITION,
            TextHelperHtmlToMarkdownAction.ACTION_DEFINITION,
            TextHelperIsDomainAction.ACTION_DEFINITION,
            TextHelperIsEmailAction.ACTION_DEFINITION,
            TextHelperIsNumericAction.ACTION_DEFINITION,
            TextHelperIsUrlAction.ACTION_DEFINITION,
            TextHelperLowerCaseAction.ACTION_DEFINITION,
            TextHelperMarkdownToHtmlAction.ACTION_DEFINITION,
            TextHelperMatchAction.ACTION_DEFINITION,
            TextHelperParseEmailAction.ACTION_DEFINITION,
            TextHelperParseEmailListAction.ACTION_DEFINITION,
            TextHelperParseUrlAction.ACTION_DEFINITION,
            TextHelperProperCaseAction.ACTION_DEFINITION,
            TextHelperRegularExpressionMatchTestAction.ACTION_DEFINITION,
            TextHelperRemoveCharactersAction.ACTION_DEFINITION,
            TextHelperRemoveSpecialCharactersAction.ACTION_DEFINITION,
            TextHelperReplaceAction.ACTION_DEFINITION,
            TextHelperSelectFirstNCharactersAction.ACTION_DEFINITION,
            TextHelperSelectLastNCharactersAction.ACTION_DEFINITION,
            TextHelperSentenceCaseAction.ACTION_DEFINITION,
            TextHelperShortenAction.ACTION_DEFINITION,
            TextHelperSplitAction.ACTION_DEFINITION,
            TextHelperStripHtmlTagsAction.ACTION_DEFINITION,
            TextHelperTrimWhitespaceAction.ACTION_DEFINITION,
            TextHelperUnderscoreAction.ACTION_DEFINITION,
            TextHelperUpperCaseAction.ACTION_DEFINITION,
            TextHelperUrlEncodeDecodeAction.ACTION_DEFINITION,
            TextHelperUrlEncodeDecodeKeyValuePairAction.ACTION_DEFINITION)
        .clusterElements(
            tool(TextHelperBase64EncodeDecodeAction.ACTION_DEFINITION),
            tool(TextHelperChangeTypeAction.ACTION_DEFINITION),
            tool(TextHelperConcatenateAction.ACTION_DEFINITION),
            tool(TextHelperContainsAction.ACTION_DEFINITION),
            tool(TextHelperEscapeCharactersAction.ACTION_DEFINITION),
            tool(TextHelperExtractAllRegExAction.ACTION_DEFINITION),
            tool(TextHelperExtractContentFromHtmlAction.ACTION_DEFINITION),
            tool(TextHelperExtractKeyRegExAction.ACTION_DEFINITION),
            tool(TextHelperExtractRegExAction.ACTION_DEFINITION),
            tool(TextHelperExtractUrlsAction.ACTION_DEFINITION),
            tool(TextHelperFormatCurrencyAction.ACTION_DEFINITION),
            tool(TextHelperGetDomainFromEmailAction.ACTION_DEFINITION),
            tool(TextHelperGetDomainFromUrlAction.ACTION_DEFINITION),
            tool(TextHelperGetFirstMiddleLastNameAction.ACTION_DEFINITION),
            tool(TextHelperGetTextAfterAction.ACTION_DEFINITION),
            tool(TextHelperGetTextBeforeAction.ACTION_DEFINITION),
            tool(TextHelperGetTextBetweenAction.ACTION_DEFINITION),
            tool(TextHelperGetTextLengthAction.ACTION_DEFINITION),
            tool(TextHelperHexEncodeDecodeAction.ACTION_DEFINITION),
            tool(TextHelperHtmlToMarkdownAction.ACTION_DEFINITION),
            tool(TextHelperIsDomainAction.ACTION_DEFINITION),
            tool(TextHelperIsEmailAction.ACTION_DEFINITION),
            tool(TextHelperIsNumericAction.ACTION_DEFINITION),
            tool(TextHelperIsUrlAction.ACTION_DEFINITION),
            tool(TextHelperLowerCaseAction.ACTION_DEFINITION),
            tool(TextHelperMarkdownToHtmlAction.ACTION_DEFINITION),
            tool(TextHelperMatchAction.ACTION_DEFINITION),
            tool(TextHelperParseEmailAction.ACTION_DEFINITION),
            tool(TextHelperParseEmailListAction.ACTION_DEFINITION),
            tool(TextHelperParseUrlAction.ACTION_DEFINITION),
            tool(TextHelperProperCaseAction.ACTION_DEFINITION),
            tool(TextHelperRegularExpressionMatchTestAction.ACTION_DEFINITION),
            tool(TextHelperRemoveCharactersAction.ACTION_DEFINITION),
            tool(TextHelperRemoveSpecialCharactersAction.ACTION_DEFINITION),
            tool(TextHelperReplaceAction.ACTION_DEFINITION),
            tool(TextHelperSelectFirstNCharactersAction.ACTION_DEFINITION),
            tool(TextHelperSelectLastNCharactersAction.ACTION_DEFINITION),
            tool(TextHelperSentenceCaseAction.ACTION_DEFINITION),
            tool(TextHelperShortenAction.ACTION_DEFINITION),
            tool(TextHelperSplitAction.ACTION_DEFINITION),
            tool(TextHelperStripHtmlTagsAction.ACTION_DEFINITION),
            tool(TextHelperTrimWhitespaceAction.ACTION_DEFINITION),
            tool(TextHelperUnderscoreAction.ACTION_DEFINITION),
            tool(TextHelperUpperCaseAction.ACTION_DEFINITION),
            tool(TextHelperUrlEncodeDecodeAction.ACTION_DEFINITION),
            tool(TextHelperUrlEncodeDecodeKeyValuePairAction.ACTION_DEFINITION))
        .version(1);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
