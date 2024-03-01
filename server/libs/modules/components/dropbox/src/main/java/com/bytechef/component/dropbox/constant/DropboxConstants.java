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

package com.bytechef.component.dropbox.constant;

/**
 * @author Mario Cvjetojevic
 */
public final class DropboxConstants {

    public static final String DROPBOX = "dropbox";
    public static final String SOURCE_FILENAME = "sourceFilename";
    public static final String DESTINATION_FILENAME = "destinationFilename";
    public static final String SEARCH_STRING = "searchString";
    public static final String FILE_ENTRY = "fileEntry";
    public static final String CLIENT_IDENTIFIER = "-";

    // actions

    public static final String COPY = "copy";
    public static final String CREATE_FOLDER = "createFolder";
    public static final String CREATE_TEXT_FILE = "createTextFile";
    public static final String DELETE = "delete";
    public static final String GET_FILE_LINK = "getFileLink";
    public static final String LIST_FOLDER = "listFolder";
    public static final String MOVE = "move";
    public static final String SEARCH = "search";
    public static final String UPLOAD_FILE = "uploadFile";

    private DropboxConstants() {
    }
}
