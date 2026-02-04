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

package com.bytechef.component.pdf.helper.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.pdf.helper.constant.PdfHelperConstants.FILENAME;
import static com.bytechef.component.pdf.helper.constant.PdfHelperConstants.TEXT;
import static com.bytechef.component.pdf.helper.util.PdfHelperUtils.storeIntoFileEntry;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;

/**
 * @author Nikolina Spehar
 */
public class PdfHelperTextToPdfAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("textToPdf")
        .title("Text to PDF")
        .description("Converts text to PDF.")
        .properties(
            string(TEXT)
                .label("Text")
                .description("The text which will be converted to PDF.")
                .controlType(ControlType.TEXT_AREA)
                .required(true),
            string(FILENAME)
                .label("Filename")
                .description("The name of the PDF file.")
                .required(true))
        .output(
            outputSchema(
                fileEntry()
                    .description("PDF file")))
        .perform(PdfHelperTextToPdfAction::perform);

    private PdfHelperTextToPdfAction() {
    }

    public static FileEntry perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Document document = new Document();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        PdfWriter.getInstance(document, byteArrayOutputStream);

        document.open();
        document.add(new Paragraph(inputParameters.getRequiredString(TEXT)));
        document.close();

        String filename = inputParameters.getRequiredString(FILENAME) + ".pdf";

        return storeIntoFileEntry(context, byteArrayOutputStream, filename);
    }
}
