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

import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.OptionsDataSource;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.google.drive.util.GoogleDriveUtils;
import com.google.api.services.drive.Drive;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Mario Cvjetojevic
 */
public final class GoogleDriveInputProperties {

    public static final String DRIVE_ID = "driveId";
    public static final String IGNORE_DEFAULT_VISIBILITY = "ignoreDefaultVisibility";
    public static final String KEEP_REVISION_FOREVER = "keepRevisionForever";
    public static final String OCR_LANGUAGE = "ocrLanguage";
    public static final String SUPPORTS_ALL_DRIVES = "supportsAllDrives";
    public static final String USE_CONTENT_AS_INDEXABLE_TEXT = "useContentAsIndexableText";
    public static final String INCLUDE_PERMISSIONS_FOR_VIEW = "includePermissionsForView";
    public static final String INCLUDE_LABELS = "includeLabels";

    public static final Map<String, Property> propertyMap;

    static {
        propertyMap = Map.of(
            DRIVE_ID,
            string(DRIVE_ID)
                .label("Folder")
                .description(
                    "The id of a folder where the file is uploaded.")
                .options((OptionsDataSource.ActionOptionsFunction) GoogleDriveInputProperties::getDriveOptions),
            IGNORE_DEFAULT_VISIBILITY,
            bool(IGNORE_DEFAULT_VISIBILITY)
                .label("Ignore default visibility")
                .description(
                    "Whether to ignore the domain's default visibility settings for the created file. " +
                        "Domain administrators can choose to make all uploaded files visible to the domain by " +
                        "default; this parameter bypasses that behavior for the request. Permissions are still " +
                        "inherited from arent folders."),
            KEEP_REVISION_FOREVER,
            bool(KEEP_REVISION_FOREVER)
                .label("Keep revision forever")
                .description(
                    "Whether to set the 'keepForever' field in the new head revision. This is only " +
                        "applicable to files with binary content in Google Drive. Only 200 revisions for the file " +
                        "can be kept forever. If the limit is reached, try deleting pinned revisions."),
            OCR_LANGUAGE,
            string(OCR_LANGUAGE)
                .label("OCR Language")
                .description(
                    "A language hint for OCR processing during image import (ISO 639-1 code)."),
            SUPPORTS_ALL_DRIVES,
            bool(SUPPORTS_ALL_DRIVES)
                .label("Supports all drives")
                .description(
                    "Whether the requesting application supports both My Drives and shared drives."),
            USE_CONTENT_AS_INDEXABLE_TEXT,
            bool(USE_CONTENT_AS_INDEXABLE_TEXT)
                .label("Use content as indexable text")
                .description(
                    "Whether to use the uploaded content as indexable text."),
            INCLUDE_PERMISSIONS_FOR_VIEW,
            string(INCLUDE_PERMISSIONS_FOR_VIEW)
                .label("Include permissions for view")
                .description(
                    "Specifies which additional view's permissions to include in the response. Only " +
                        "'published' is supported."),
            INCLUDE_LABELS,
            string(INCLUDE_LABELS)
                .label("Include labels")
                .description(
                    "A comma-separated list of IDs of labels to include in the " +
                        "labelInfo part of the response."));
    }

    public static List<Option<String>> getDriveOptions(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context)
        throws IOException {

        List<Option<String>> options;

        Drive service = GoogleDriveUtils.getDrive(connectionParameters);

        List<com.google.api.services.drive.model.Drive> drives = service.drives()
            .list()
            .execute()
            .getDrives();

        options = drives.stream()
            .filter(drive -> StringUtils.isNotEmpty(searchText) &&
                StringUtils.startsWith(drive.getName(), searchText))
            .map(drive -> (Option<String>) option(drive.getName(), drive.getId()))
            .toList();

        return options;
    }
}
