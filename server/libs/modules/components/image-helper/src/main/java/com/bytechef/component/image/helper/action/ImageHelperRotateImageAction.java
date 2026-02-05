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
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.image.helper.constant.ImageHelperConstants.DEGREE;
import static com.bytechef.component.image.helper.constant.ImageHelperConstants.IMAGE;
import static com.bytechef.component.image.helper.constant.ImageHelperConstants.IMAGE_PROPERTY;
import static com.bytechef.component.image.helper.constant.ImageHelperConstants.RESULT_FILE_NAME;
import static com.bytechef.component.image.helper.constant.ImageHelperConstants.RESULT_FILE_NAME_PROPERTY;
import static com.bytechef.component.image.helper.util.ImageHelperUtils.storeBufferedImage;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * @author Monika Kušter
 */
public class ImageHelperRotateImageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("rotateImage")
        .title("Rotate Image")
        .description("Rotates an image by a specified degree.")
        .help("", "https://docs.bytechef.io/reference/components/image-helper_v1#rotate-image")
        .properties(
            IMAGE_PROPERTY,
            integer(DEGREE)
                .label("Degree")
                .description("Specifies the degree of clockwise rotation applied to the image.")
                .options(
                    option("90°", 90),
                    option("180°", 180),
                    option("270°", 270))
                .required(true),
            RESULT_FILE_NAME_PROPERTY)
        .output(outputSchema(fileEntry().description("Rotated image.")))
        .perform(ImageHelperRotateImageAction::perform);

    private ImageHelperRotateImageAction() {
    }

    protected static FileEntry perform(
        Parameters inputParameters, Parameters connectionParameters, Context actionContext) throws IOException {

        FileEntry imageFileEntry = inputParameters.getRequiredFileEntry(IMAGE);

        BufferedImage bufferedImage = ImageIO.read((File) actionContext.file(file -> file.toTempFile(imageFileEntry)));

        BufferedImage rotatedImage =
            rotateImage(inputParameters.getRequiredInteger(DEGREE), bufferedImage);

        return storeBufferedImage(
            actionContext, rotatedImage, imageFileEntry.getExtension(),
            inputParameters.getRequiredString(RESULT_FILE_NAME));
    }

    private static BufferedImage rotateImage(int degree, BufferedImage bufferedImage) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        BufferedImage newImage = new BufferedImage(width, height, bufferedImage.getType());

        Graphics2D graphics2D = newImage.createGraphics();

        graphics2D.rotate(Math.toRadians(degree), (double) width / 2, (double) height / 2);
        graphics2D.drawImage(bufferedImage, null, 0, 0);

        return newImage;
    }
}
