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

package com.bytechef.component.pdf.helper.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import java.io.File;
import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * @author Monika Kušter
 */
public class PdfHelperExtractTextAction {

    protected static final String FILE = "file";

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("extractText")
        .title("Extract Text")
        .description("Extracts text from a PDF file.")
        .properties(
            fileEntry(FILE)
                .label("PDF File")
                .description("The PDF file from which to extract text.")
                .required(true))
        .output(outputSchema(string().description("Extracted text from the PDF file.")))
        .perform(PdfHelperExtractTextAction::perform);

    private PdfHelperExtractTextAction() {
    }

    protected static String perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws IOException {

        FileEntry fileEntry = inputParameters.getRequiredFileEntry(FILE);

        File file1 = actionContext.file(file -> file.toTempFile(fileEntry));

        PDDocument document = Loader.loadPDF(file1);

        PDFTextStripper stripper = new PDFTextStripper();

        return stripper.getText(document);
    }
}
