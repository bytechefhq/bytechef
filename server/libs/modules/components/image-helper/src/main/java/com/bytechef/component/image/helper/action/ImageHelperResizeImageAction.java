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
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.image.helper.constant.ImageHelperConstants.HEIGHT;
import static com.bytechef.component.image.helper.constant.ImageHelperConstants.IMAGE;
import static com.bytechef.component.image.helper.constant.ImageHelperConstants.IMAGE_PROPERTY;
import static com.bytechef.component.image.helper.constant.ImageHelperConstants.RESULT_FILE_NAME;
import static com.bytechef.component.image.helper.constant.ImageHelperConstants.RESULT_FILE_NAME_PROPERTY;
import static com.bytechef.component.image.helper.constant.ImageHelperConstants.WIDTH;
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
public class ImageHelperResizeImageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("resizeImage")
        .title("Resize Image")
        .description("Resizes an image to the specified width and height.")
        .help("", "https://docs.bytechef.io/reference/components/image-helper_v1#resize-image")
        .properties(
            IMAGE_PROPERTY,
            integer(WIDTH)
                .label("Width")
                .description("The target width of the image in pixels.")
                .required(true),
            integer(HEIGHT)
                .label("Height")
                .description("The target height of the image in pixels.")
                .required(true),
            RESULT_FILE_NAME_PROPERTY)
        .output(outputSchema(fileEntry().description("Resized image.")))
        .perform(ImageHelperResizeImageAction::perform);

    private ImageHelperResizeImageAction() {
    }

    public static FileEntry perform(Parameters inputParameters, Parameters connectionParameters, Context context)
        throws IOException {

        FileEntry image = inputParameters.getRequiredFileEntry(IMAGE);

        BufferedImage bufferedImage = ImageIO.read((File) context.file(file -> file.toTempFile(image)));

        BufferedImage resizedImage = getResizedImage(
            inputParameters.getRequiredInteger(WIDTH), inputParameters.getRequiredInteger(HEIGHT), bufferedImage);

        return storeBufferedImage(
            context, resizedImage, image.getExtension(), inputParameters.getRequiredString(RESULT_FILE_NAME));
    }

    private static BufferedImage getResizedImage(int width, int height, BufferedImage bufferedImage) {
        BufferedImage resizedImage = new BufferedImage(width, height, bufferedImage.getType());

        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.drawImage(bufferedImage, 0, 0, width, height, null);

        return resizedImage;
    }
}
