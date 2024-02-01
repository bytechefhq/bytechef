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

package com.bytechef.component.google.drive.properties;

import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.ComponentDSL.ModifiableObjectProperty;

/**
 * @author Mario Cvjetojevic
 */
public final class GoogleDriveOutputProperties {

    private static final String COPY_REQUIRES_WRITER_PERMISSION = "copyRequiresWriterPermission";
    private static final String CREATED_TIME = "createdTime";
    private static final String DESCRIPTION = "description";
    private static final String EXPLICITLY_TRASHED = "explicitlyTrashed";
    private static final String FILE_EXTENSION = "fileExtension";
    private static final String FULL_FILE_EXTENSIONS = "fullFileExtension";
    private static final String HAS_AUGMENTED_PERMISSIONS = "hasAugmentedPermissions";
    private static final String HAS_THUMBNAIL = "headRevisionId";
    private static final String HEAD_REVISION_ID = "iconLink";
    private static final String ICON_LINK = "iconLink";
    private static final String ID = "id";
    private static final String IS_APP_AUTHORIZED = "isAppAuthorized";
    private static final String LAST_MODIFYING_USER = "lastModifyingUser";
    private static final String MD5_CHECKSUM = "md5Checksum";
    private static final String NAME = "name";
    private static final String ORIGINAL_FILENAME = "originalFilename";
    private static final String OWNERS = "owners";
    private static final String PARENTS = "parents";
    private static final String PERMISSION_IDS = "permissionIds";
    private static final String QUOTA_BYTES_USED = "quotaBytesUsed";
    private static final String RESOURCE_KEY = "resourceKey";
    private static final String SHA1_CHECKSUM = "sha1Checksum";
    private static final String SHA256_CHECKSUM = "sha256Checksum";
    private static final String SHARED = "shared";
    private static final String SHARING_USER = "sharingUser";
    private static final String SIZE = "size";
    private static final String STARRED = "starred";
    private static final String TEAM_DRIVE_ID = "teamDriveId";
    private static final String THUMBNAIL_LINK = "thumbnailLink";
    private static final String THUMBNAIL_VERSION = "thumbnailVersion";
    private static final String TRASHED = "trashed";
    private static final String TRASHING_USER = "trashingUser";
    private static final String VERSION = "version";
    private static final String VIEWERS_CAN_COPY_CONTENT = "viewersCanCopyContent";
    private static final String WEB_CONTENT_LINK = "webContentLink";
    private static final String WEB_VIEW_LINK = "webViewLink";
    private static final String WRITERS_CAN_SHARE = "writersCanShare";
    private static final String DISPLAY_NAME = "displayName";
    private static final String EMAIL_ADDRESS = "emailAddress";
    private static final String ME = "me";
    private static final String PERMISSION_ID = "permissionId";
    private static final String PHOTO_LINK = "photoLink";
    private static final String KIND = "kind";

    private static final ComponentDSL.ModifiableValueProperty<?, ?>[] USER_PROPERTIES = {
        string(DISPLAY_NAME),
        string(EMAIL_ADDRESS),
        string(KIND),
        string(ME),
        string(PERMISSION_ID),
        string(PHOTO_LINK)
    };

    public static final ModifiableObjectProperty FILE_PROPERTY = object("file")
        .properties(
            bool(COPY_REQUIRES_WRITER_PERMISSION),
            dateTime(CREATED_TIME),
            string(DESCRIPTION),
            bool(EXPLICITLY_TRASHED),
            string(FILE_EXTENSION),
            string(FULL_FILE_EXTENSIONS),
            bool(HAS_AUGMENTED_PERMISSIONS),
            bool(HAS_THUMBNAIL),
            string(HEAD_REVISION_ID),
            string(ICON_LINK),
            string(ID),
            bool(IS_APP_AUTHORIZED),
            object(LAST_MODIFYING_USER)
                .properties(USER_PROPERTIES),
            string(MD5_CHECKSUM),
            string(NAME),
            string(ORIGINAL_FILENAME),
            array(OWNERS)
                .items(USER_PROPERTIES),
            array(PARENTS)
                .items(
                    string()),
            array(PERMISSION_IDS)
                .items(
                    string()),
            number(QUOTA_BYTES_USED),
            string(RESOURCE_KEY),
            string(SHA1_CHECKSUM),
            string(SHA256_CHECKSUM),
            bool(SHARED),
            object(SHARING_USER)
                .properties(USER_PROPERTIES),
            number(SIZE),
            bool(STARRED),
            string(TEAM_DRIVE_ID),
            string(THUMBNAIL_LINK),
            number(THUMBNAIL_VERSION),
            bool(TRASHED),
            object(TRASHING_USER)
                .properties(USER_PROPERTIES),
            number(VERSION),
            bool(VIEWERS_CAN_COPY_CONTENT),
            string(WEB_CONTENT_LINK),
            string(WEB_VIEW_LINK),
            bool(WRITERS_CAN_SHARE));

    private GoogleDriveOutputProperties() {
    }
}
