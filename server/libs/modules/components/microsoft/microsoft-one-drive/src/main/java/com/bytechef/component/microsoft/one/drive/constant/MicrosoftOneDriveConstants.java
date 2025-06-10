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

package com.bytechef.component.microsoft.one.drive.constant;

import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Monika Kušter
 */
public class MicrosoftOneDriveConstants {

    public static final String FILE = "file";
    public static final String ID = "id";
    public static final String MIME_TYPE = "mimeType";
    public static final String NAME = "name";
    public static final String PARENT_ID = "parentId";
    public static final String TEXT = "text";
    public static final String VALUE = "value";

    public static final ModifiableObjectProperty FILE_OUTPUT_PROPERTY = object()
        .properties(
            dateTime("createdDateTime")
                .description("The date and time when the file was created."),
            string("eTag"),
            string(ID)
                .description("ID of the file."),
            dateTime("lastModifiedDateTime")
                .description("The date and time when the file was last modified."),
            string(NAME)
                .description("Name of the created file."),
            integer("size")
                .description("Size of the file in bytes."),
            string("webUrl")
                .description("URL to access the file in a web browser."),
            object("createdBy")
                .properties(
                    object("user")
                        .properties(
                            string(ID)
                                .description("ID of the user who created the file."),
                            string("displayName")
                                .description("Display name of the user who created the file."))),
            object("lastModifiedBy")
                .properties(
                    object("user")
                        .properties(
                            string(ID)
                                .description("ID of the user who last modified file."),
                            string("displayName")
                                .description("Display name of the user who last modified the file."))),
            object(FILE)
                .properties(
                    object("hashes")
                        .description("Hashes of the file's binary content")
                        .properties(
                            string("quickXorHash")
                                .description(
                                    "A proprietary hash of the file that can be used to determine if the contents of " +
                                        "the file change.")),
                    string("mimeType")
                        .description("The MIME type for the file.")));

    public static final ModifiableObjectProperty FOLDER_OUTPUT_PROPERTY = object()
        .properties(
            dateTime("createdDateTime")
                .description("The date and time when the folder was created."),
            string("eTag"),
            string(ID)
                .description("ID of the folder."),
            dateTime("lastModifiedDateTime")
                .description("The date and time when the folder was last modified."),
            string(NAME)
                .description("Name of the folder."),
            integer("size")
                .description("Size of the folder in bytes."),
            string("webUrl")
                .description("URL to access the folder in a web browser."),
            object("createdBy")
                .properties(
                    object("user")
                        .properties(
                            string(ID)
                                .description("ID of the user who created the folder."),
                            string("displayName")
                                .description("Display name of the user who created the folder."))),
            object("lastModifiedBy")
                .properties(
                    object("user")
                        .properties(
                            string(ID)
                                .description("ID of the user who last modified the folder."),
                            string("displayName")
                                .description("Display name of the user who last modified the folder."))),
            object("folder")
                .properties(
                    integer("childCount")
                        .description("Number of items contained in the folder.")));

    private MicrosoftOneDriveConstants() {
    }
}
