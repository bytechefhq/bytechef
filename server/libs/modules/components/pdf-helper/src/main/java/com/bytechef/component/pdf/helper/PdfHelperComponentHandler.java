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

package com.bytechef.component.pdf.helper;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.pdf.helper.action.PdfHelperConvertToImageAction;
import com.bytechef.component.pdf.helper.action.PdfHelperExtractTextAction;
import com.bytechef.component.pdf.helper.action.PdfHelperImageToPdfAction;
import com.bytechef.component.pdf.helper.action.PdfHelperTextToPdfAction;
import com.google.auto.service.AutoService;

/**
 * @author Monika Kušter
 */
@AutoService(ComponentHandler.class)
public class PdfHelperComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("pdfHelper")
        .title("PDF Helper")
        .icon("path:assets/pdf-helper.svg")
        .categories(ComponentCategory.HELPERS)
        .actions(
            PdfHelperConvertToImageAction.ACTION_DEFINITION,
            PdfHelperExtractTextAction.ACTION_DEFINITION,
            PdfHelperImageToPdfAction.ACTION_DEFINITION,
            PdfHelperTextToPdfAction.ACTION_DEFINITION)
        .clusterElements(
            tool(PdfHelperConvertToImageAction.ACTION_DEFINITION),
            tool(PdfHelperExtractTextAction.ACTION_DEFINITION),
            tool(PdfHelperImageToPdfAction.ACTION_DEFINITION),
            tool(PdfHelperTextToPdfAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
