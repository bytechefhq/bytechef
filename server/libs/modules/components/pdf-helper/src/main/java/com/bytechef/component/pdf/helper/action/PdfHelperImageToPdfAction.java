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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.pdf.helper.constant.PdfHelperConstants.FILENAME;
import static com.bytechef.component.pdf.helper.constant.PdfHelperConstants.IMAGE;
import static com.bytechef.component.pdf.helper.constant.PdfHelperConstants.IMAGES;
import static com.bytechef.component.pdf.helper.util.PdfHelperUtils.storeIntoFileEntry;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/**
 * @author Nikolina Spehar
 */
public class PdfHelperImageToPdfAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("imageToPdf")
        .title("Image to PDF")
        .description("Converts image to PDF.")
        .properties(
            array(IMAGES)
                .description("List of images that will be converted to one PDF.")
                .required(true)
                .items(
                    fileEntry(IMAGE)
                        .label("Image")
                        .description("The image which will be converted to PDF.")
                        .required(true)),
            string(FILENAME)
                .label("Filename")
                .description("The name of the PDF file.")
                .required(true))
        .output(
            outputSchema(
                fileEntry()
                    .description("PDF file")))
        .help("", "https://docs.bytechef.io/reference/components/pdf-helper_v1#image-to-pdf")
        .perform(PdfHelperImageToPdfAction::perform);

    private PdfHelperImageToPdfAction() {
    }

    public static FileEntry perform(Parameters inputParameters, Parameters connectionParameters, Context context)
        throws Exception {

        List<FileEntry> fileEntries = inputParameters.getRequiredList(IMAGES, FileEntry.class);

        PDDocument pdDocument = new PDDocument();

        int pageWidth = (int) PDRectangle.A4.getWidth();
        int pageHeight = (int) PDRectangle.A4.getHeight();

        for (FileEntry fileEntry : fileEntries) {
            File image = context.file(file -> file.toTempFile(fileEntry));

            PDPage pdPage = new PDPage(new PDRectangle(pageWidth, pageHeight));
            pdDocument.addPage(pdPage);

            PDPageContentStream pdPageContentStream = new PDPageContentStream(pdDocument, pdPage);
            PDImageXObject pdImageXObject = PDImageXObject.createFromFileByContent(image, pdDocument);

            float imageWidth = pdImageXObject.getWidth();
            float imageHeight = pdImageXObject.getHeight();

            float scale = Math.min(pageWidth / imageWidth, pageHeight / imageHeight);
            float scaledWidth = imageWidth * scale;
            float scaledHeight = imageHeight * scale;

            float x = (pageWidth - scaledWidth) / 2;
            float y = (pageHeight - scaledHeight) / 2;

            pdPageContentStream.drawImage(pdImageXObject, x, y, scaledWidth, scaledHeight);
            pdPageContentStream.close();
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        pdDocument.save(byteArrayOutputStream);
        pdDocument.close();

        String filename = inputParameters.getRequiredString(FILENAME) + ".pdf";

        return storeIntoFileEntry(context, byteArrayOutputStream, filename);
    }
}
