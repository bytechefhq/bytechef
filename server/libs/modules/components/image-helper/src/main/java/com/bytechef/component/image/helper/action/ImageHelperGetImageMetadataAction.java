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
import static com.bytechef.component.image.helper.constant.ImageHelperConstants.IMAGE;
import static com.bytechef.component.image.helper.constant.ImageHelperConstants.IMAGE_PROPERTY;
import static com.bytechef.component.image.helper.constant.ImageHelperConstants.RESULT_FILE_NAME_PROPERTY;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;

/**
 * @author Monika Kušter
 */
public class ImageHelperGetImageMetadataAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getImageMetadata")
        .title("Get Image Metadata")
        .description("Get metadata of the image.")
        .properties(
            IMAGE_PROPERTY,
            RESULT_FILE_NAME_PROPERTY)
        .output()
        .perform(ImageHelperGetImageMetadataAction::perform);

    private ImageHelperGetImageMetadataAction() {
    }

    protected static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context actionContext) throws IOException {

        FileEntry imageFileEntry = inputParameters.getRequiredFileEntry(IMAGE);

        File imageFile = actionContext.file(file -> file.toTempFile(imageFileEntry));
        ImageInputStream iis = ImageIO.createImageInputStream(imageFile);
        Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);

        Map<String, Object> metadataMap = new HashMap<>();

        if (readers.hasNext()) {
            ImageReader reader = readers.next();
            reader.setInput(iis, true);
            IIOMetadata metadata = reader.getImageMetadata(0);

            String[] metadataFormatNames = metadata.getMetadataFormatNames();
            for (String formatName : metadataFormatNames) {
                metadataMap.put(formatName, metadata.getAsTree(formatName));
            }
        }

        return metadataMap;
    }
}
