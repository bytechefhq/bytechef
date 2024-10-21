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

package com.bytechef.component.image.helper.action;

import static com.bytechef.component.definition.ComponentDsl.*;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.image.helper.constant.ImageHelperConstants.*;
import static com.bytechef.component.image.helper.util.ImageHelperUtils.storeBufferedImage;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * @author Jakub Smolnický
 */
public class ImageHelperCropImageAction {

    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("cropImage")
        .title("Crop Image")
        .description("Crops an image to the specified dimensions.")
        .properties(
            IMAGE_PROPERTY,
            integer(X_COORDINATE)
                .label("X Coordinate")
                .description("The horizontal starting point of the crop area")
                .required(true),
            integer(Y_COORDINATE)
                .label("Y Coordinate")
                .description("The vertical starting point of the crop area")
                .required(true),
            integer(WIDTH)
                .label("Width")
                .description("Width of the crop area")
                .required(true),
            integer(HEIGHT)
                .label("Height")
                .description("Height of the crop area")
                .required(true),
            RESULT_FILE_NAME_PROPERTY)
        .output(outputSchema(fileEntry()))
        .perform(ImageHelperCropImageAction::perform);

    private ImageHelperCropImageAction() {
    }

    protected static FileEntry perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws IOException {

        FileEntry imageFileEntry = inputParameters.getRequiredFileEntry(IMAGE);

        BufferedImage bufferedImage = ImageIO.read((File) actionContext.file(file -> file.toTempFile(imageFileEntry)));

        int x = inputParameters.getRequiredInteger(X_COORDINATE);
        int y = inputParameters.getRequiredInteger(Y_COORDINATE);
        int width = inputParameters.getRequiredInteger(WIDTH);
        int height = inputParameters.getRequiredInteger(HEIGHT);

        validate(x, y, width, height, bufferedImage.getWidth(), bufferedImage.getHeight());

        BufferedImage croppedImage = bufferedImage.getSubimage(x, y, width, height);

        return storeBufferedImage(
            actionContext, croppedImage, imageFileEntry.getExtension(),
            inputParameters.getRequiredString(RESULT_FILE_NAME));
    }

    private static void validate(int x, int y, int width, int height, int imageWidth, int imageHeight)
        throws IllegalArgumentException {
        if (x < 0 || y < 0 || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Invalid input parameters.");
        }

        if (x + width > imageWidth || y + height > imageHeight) {
            throw new IllegalArgumentException("Invalid crop area specified for cropping.");
        }
    }
}
