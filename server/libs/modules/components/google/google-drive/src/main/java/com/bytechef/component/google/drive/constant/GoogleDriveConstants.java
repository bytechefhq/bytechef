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

package com.bytechef.component.google.drive.constant;

import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import java.util.Map;

/**
 * @author Mario Cvjetojevic
 * @author Monika Kušter
 */
public final class GoogleDriveConstants {

    public static final String APPLICATION_VND_GOOGLE_APPS_FOLDER = "application/vnd.google-apps.folder";
    public static final String FILE_ENTRY = "fileEntry";
    public static final String FOLDER_NAME = "folderName";
    public static final String ID = "id";
    public static final String MIME_TYPE = "mimeType";
    public static final String NAME = "name";
    public static final String TEXT = "text";

    public static final ModifiableObjectProperty GOOGLE_FILE_OUTPUT_PROPERTY = object()
        .properties(
            string(ID)
                .description("The ID of the file."),
            string("kind")
                .description("Identifies what kind of resource this is."),
            string(MIME_TYPE)
                .description("The MIME type of the file."),
            string(NAME)
                .description("The name of the file."));

    public static final Map<String, String> GOOGLE_FILE_SAMPLE_OUTPUT = Map.of(
        ID, "1hPJ7kjhStTX90amAWSJ-V0K1-nhDlsIr",
        MIME_TYPE, "plain/text",
        NAME, "new-file.txt");

    private GoogleDriveConstants() {
    }
}
