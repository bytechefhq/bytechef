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

package com.bytechef.component.image.helper.constant;

import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableFileEntryProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;

/**
 * @author Monika Kušter
 */
public class ImageHelperConstants {

    public static final String DEGREE = "degree";
    public static final String HEIGHT = "height";
    public static final String IMAGE = "image";
    public static final String QUALITY = "quality";
    public static final String RESULT_FILE_NAME = "resultFileName";
    public static final String WIDTH = "width";
    public static final String X_COORDINATE = "x";
    public static final String Y_COORDINATE = "y";

    public static final ModifiableFileEntryProperty IMAGE_PROPERTY = fileEntry(IMAGE)
        .label("Image")
        .description("The image file to process.")
        .required(true);

    public static final ModifiableStringProperty RESULT_FILE_NAME_PROPERTY = string(RESULT_FILE_NAME)
        .label("Result File Name")
        .description("Specifies the output file name for the result image.")
        .defaultValue("image")
        .required(true);

    private ImageHelperConstants() {
    }
}
