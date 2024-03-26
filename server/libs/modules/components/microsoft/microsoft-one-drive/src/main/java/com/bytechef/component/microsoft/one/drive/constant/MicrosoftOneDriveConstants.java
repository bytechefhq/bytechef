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

package com.bytechef.component.microsoft.one.drive.constant;

import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ComponentDSL.ModifiableStringProperty;
import com.bytechef.component.definition.OptionsDataSource;
import com.bytechef.component.microsoft.one.drive.util.MicrosoftOneDriveUtils;

/**
 * @author Monika Domiter
 */
public class MicrosoftOneDriveConstants {

    public static final String BASE_URL = "https://graph.microsoft.com/v1.0/me/drive";
    public static final String DOWNLOAD_FILE = "downloadFile";
    public static final String FILE = "file";
    public static final String ID = "id";
    public static final String LIST_FILES = "listFiles";
    public static final String LIST_FOLDERS = "listFolders";
    public static final String MICROSOFT_ONEDRIVE = "microsoftOneDrive";
    public static final String PARENT_ID = "parentId";
    public static final String TENANT_ID = "tenantId";
    public static final String UPLOAD_FILE = "uploadFile";

    public static final ModifiableStringProperty PARENT_ID_PROPERTY = string(PARENT_ID)
        .label("Parent folder")
        .options((OptionsDataSource.ActionOptionsFunction<String>) MicrosoftOneDriveUtils::getFolderIdOptions)
        .required(false);

    private MicrosoftOneDriveConstants() {
    }
}
