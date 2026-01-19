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
import static com.bytechef.component.text.helper.constant.TextHelperConstants.HTML;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;

/**
 * @author Monika Kušter
 */
public class TextHelperHTMLToMarkdownAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("HTMLToMarkdown")
        .title("HTML to Markdown")
        .description("Converts HTML to markdown.")
        .properties(
            string(HTML)
                .label("HTML Content")
                .description("HTML content to be converted to markdown.")
                .controlType(ControlType.TEXT_AREA)
                .required(true))
        .output(outputSchema(string().description("Markdown content.")))
        .perform(TextHelperHTMLToMarkdownAction::perform);

    private TextHelperHTMLToMarkdownAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return FlexmarkHtmlConverter.builder()
            .build()
            .convert(inputParameters.getRequiredString(HTML));
    }
}
