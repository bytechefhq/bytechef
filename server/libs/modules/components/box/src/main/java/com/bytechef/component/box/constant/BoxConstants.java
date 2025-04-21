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

package com.bytechef.component.box.constant;

import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Monika Domiter
 */
public class BoxConstants {

    private BoxConstants() {
    }

    public static final String FILE = "file";
    public static final String FILE_ID = "fileId";
    public static final String FOLDER = "folder";
    public static final String FOLDER_ID = "folderId";
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String PARENT = "parent";
    public static final String TYPE = "type";

    public static final ModifiableObjectProperty FILE_OUTPUT_PROPERTY = object()
        .properties(
            string(TYPE)
                .description("This is always set to file."),
            string(ID)
                .description("ID of the uploaded file."),
            string(NAME)
                .description("Name of the uploaded file."),
            object(PARENT)
                .description(
                    "Folder that uploaded file is located within. This value may be null " +
                        "for some folders such as the root folder or the trash folder.")
                .properties(
                    string(TYPE)
                        .description("This is always set to folder."),
                    string(ID)
                        .description("ID of the parent folder."),
                    string(NAME)
                        .description("Name of the parent folder.")));

    public static final ModifiableObjectProperty FOLDER_OUTPUT_PROPERTY = object()
        .properties(
            string(TYPE)
                .description("This is always set to folder."),
            string(ID)
                .description("ID of the new folder."),
            string(NAME)
                .description("Name of the new folder."),
            object(PARENT)
                .description(
                    "Folder that new folder is located within. This value may be null for some folders such as " +
                        "the root folder or the trash folder.")
                .properties(
                    string(TYPE)
                        .description("This is always set to folder."),
                    string(ID)
                        .description("ID of the parent folder."),
                    string(NAME)
                        .description("Name of the parent folder.")));
}
