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

package com.bytechef.component.text.helper.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.MARKDOWN;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.profile.pegdown.Extensions;
import com.vladsch.flexmark.profile.pegdown.PegdownOptionsAdapter;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.DataHolder;

/**
 * @author Monika Kušter
 */
public class TextHelperMarkdownToHTMLAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("markdownToHTML")
        .title("Markdown to HTML")
        .description("Converts markdown to HTML.")
        .properties(
            string(MARKDOWN)
                .label("Markdown content")
                .description("Markdown content to convert to HTML.")
                .controlType(ControlType.TEXT_AREA)
                .required(true))
        .output(outputSchema(string().description("HTML content.")))
        .perform(TextHelperMarkdownToHTMLAction::perform);

    private TextHelperMarkdownToHTMLAction() {
    }

    public static String perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        DataHolder dataHolder = PegdownOptionsAdapter.flexmarkOptions(true, Extensions.ALL);

        Document document = Parser
            .builder(dataHolder)
            .build()
            .parse(inputParameters.getRequiredString(MARKDOWN));

        return HtmlRenderer
            .builder(dataHolder)
            .build()
            .render(document);
    }
}
