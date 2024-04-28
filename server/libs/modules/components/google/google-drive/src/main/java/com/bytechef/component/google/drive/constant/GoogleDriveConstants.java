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

package com.bytechef.component.google.drive.constant;

import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ComponentDSL.ModifiableObjectProperty;
import java.util.Map;

/**
 * @author Mario Cvjetojevic
 * @author Monika Domiter
 */
public final class GoogleDriveConstants {

    public static final String CREATE_NEW_FOLDER = "createNewFolder";
    public static final String CREATE_NEW_TEXT_FILE = "createNewTextFile";
    public static final String FILE_ENTRY = "fileEntry";
    public static final String FILE_ID = "fileId";
    public static final String FILE_NAME = "fileName";
    public static final String FOLDER_NAME = "folderName";
    public static final String GOOGLE_DRIVE = "googleDrive";
    public static final String ID = "id";
    public static final String MIME_TYPE = "mimeType";
    public static final String NAME = "name";
    public static final String PARENT_FOLDER = "parentFolder";
    public static final String READ_FILE = "readFile";
    public static final String TEXT = "text";
    public static final String UPLOAD_FILE = "uploadFile";

    public static final ModifiableObjectProperty GOOGLE_FILE_OUTPUT_PROPERTY = object()
        .properties(
            string(ID),
            string(MIME_TYPE),
            string(NAME));

    public static final Map<String, String> GOOGLE_FILE_SAMPLE_OUTPUT = Map.of(
        ID, "1hPJ7kjhStTX90amAWSJ-V0K1-nhDlsIr",
        MIME_TYPE, "plain/text",
        NAME, "new-file.txt");

    private GoogleDriveConstants() {
    }
}
