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

package com.bytechef.component.image.helper.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.image.helper.constant.ImageHelperConstants.IMAGE;
import static com.bytechef.component.image.helper.constant.ImageHelperConstants.IMAGE_PROPERTY;
import static com.bytechef.component.image.helper.constant.ImageHelperConstants.QUALITY;
import static com.bytechef.component.image.helper.constant.ImageHelperConstants.RESULT_FILE_NAME;
import static com.bytechef.component.image.helper.constant.ImageHelperConstants.RESULT_FILE_NAME_PROPERTY;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * @author Monika Kušter
 */
public class ImageHelperCompressImageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("compressImage")
        .title("Compress Image")
        .description("Compress image with specified quality.")
        .properties(
            IMAGE_PROPERTY,
            number(QUALITY)
                .label("Quality")
                .description("Compression quality of the image.")
                .minValue(0)
                .maxValue(1)
                .required(true),
            RESULT_FILE_NAME_PROPERTY)
        .output(outputSchema(fileEntry().description("Compressed image.")))
        .perform(ImageHelperCompressImageAction::perform);

    private ImageHelperCompressImageAction() {
    }

    protected static FileEntry perform(
        Parameters inputParameters, Parameters connectionParameters, Context actionContext) throws IOException {

        FileEntry imageFileEntry = inputParameters.getRequiredFileEntry(IMAGE);

        BufferedImage inputImage = ImageIO.read((File) actionContext.file(file -> file.toTempFile(imageFileEntry)));
        String fileExtension = imageFileEntry.getExtension();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(fileExtension);
        ImageWriter writer = writers.next();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(byteArrayOutputStream);
        writer.setOutput(imageOutputStream);

        ImageWriteParam params = writer.getDefaultWriteParam();
        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        params.setCompressionQuality(inputParameters.getRequiredFloat(QUALITY));

        writer.write(null, new IIOImage(inputImage, null, null), params);

        imageOutputStream.close();
        writer.dispose();

        InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

        return actionContext
            .file(file -> file.storeContent(
                inputParameters.getRequiredString(RESULT_FILE_NAME) + "." + fileExtension, inputStream));
    }
}
