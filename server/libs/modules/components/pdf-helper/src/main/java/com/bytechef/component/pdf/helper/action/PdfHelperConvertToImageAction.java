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
import static com.bytechef.component.pdf.helper.constant.PdfHelperConstants.DPI;
import static com.bytechef.component.pdf.helper.constant.PdfHelperConstants.FILE;
import static com.bytechef.component.pdf.helper.constant.PdfHelperConstants.FILENAME;
import static com.bytechef.component.pdf.helper.util.PdfHelperUtils.storeIntoFileEntry;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

/**
 * @author Nikolina Spehar
 */
public class PdfHelperConvertToImageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("convertToImage")
        .title("Convert to Image")
        .description("Converts pdf to image.")
        .properties(
            fileEntry(FILE)
                .label("PDF File")
                .description("The PDF file which will be converted to image.")
                .required(true),
            string(FILENAME)
                .label("Image Name")
                .description("Name of the image. Every image will have index of the corresponding page in its name.")
                .required(true))
        .output(
            outputSchema(
                array()
                    .items(
                        fileEntry()
                            .description("Image of a page in the PDF file."))))
        .help("", "https://docs.bytechef.io/reference/components/pdf-helper_v1#convert-to-image")
        .perform(PdfHelperConvertToImageAction::perform);

    private PdfHelperConvertToImageAction() {
    }

    public static List<FileEntry> perform(Parameters inputParameters, Parameters connectionParameters, Context context)
        throws Exception {

        FileEntry fileEntry = inputParameters.getRequiredFileEntry(FILE);

        File pdfFile = context.file(file -> file.toTempFile(fileEntry));

        PDDocument pdDocument = Loader.loadPDF(pdfFile);

        PDFRenderer pdfRenderer = new PDFRenderer(pdDocument);

        List<FileEntry> images = new ArrayList<>();

        for (int pageIndex = 0; pageIndex < pdDocument.getNumberOfPages(); pageIndex++) {
            BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(pageIndex, DPI, ImageType.RGB);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            ImageIO.write(bufferedImage, "jpeg", byteArrayOutputStream);

            String filename = inputParameters.getRequiredString(FILENAME) + (pageIndex + 1) + ".jpeg";

            images.add(storeIntoFileEntry(context, byteArrayOutputStream, filename));
        }

        return images;
    }
}
