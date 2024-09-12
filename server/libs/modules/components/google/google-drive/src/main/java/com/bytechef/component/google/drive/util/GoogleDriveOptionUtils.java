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

package com.bytechef.component.google.drive.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.PARENT_FOLDER;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 * @author Monika Domiter
 */
public class GoogleDriveOptionUtils {

    protected static final String LAST_TIME_CHECKED = "lastTimeChecked";

    private GoogleDriveOptionUtils() {
    }

    public static List<Option<String>> getFileOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) throws IOException {

        Drive drive = GoogleServices.getDrive(connectionParameters);

        return drive.files()
            .list()
            .setQ("mimeType != 'application/vnd.google-apps.folder'")
            .execute()
            .getFiles()
            .stream()
            .map(file -> (Option<String>) option(file.getName(), file.getId()))
            .toList();
    }

    public static List<Option<String>> getFolderOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) throws IOException {

        Drive drive = GoogleServices.getDrive(connectionParameters);

        return drive.files()
            .list()
            .setQ("mimeType = 'application/vnd.google-apps.folder'")
            .execute()
            .getFiles()
            .stream()
            .map(folder -> (Option<String>) option(folder.getName(), folder.getId()))
            .toList();
    }

    public static PollOutput getPollOutput(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters, boolean newFile) {

        LocalDateTime startDate =
            closureParameters.getLocalDateTime(LAST_TIME_CHECKED, LocalDateTime.now(ZoneId.of("GMT")));
        LocalDateTime endDate = LocalDateTime.now(ZoneId.of("GMT"));

        Drive drive = GoogleServices.getDrive(connectionParameters);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        String startDateString = startDate.format(formatter);

        try {

            String mimeType = "mimeType = 'application/vnd.google-apps.folder'";
            if (newFile) {
                mimeType = "mimeType = 'application/vnd.google-apps.folder'";
            }

            FileList execute = drive
                .files()
                .list()
                .setQ(mimeType + " and '" + inputParameters.getRequiredString(PARENT_FOLDER) + "' in parents and " +
                    "trashed = false and createdTime > '" + startDateString + "'")
                .setFields("files(id, name, mimeType, webViewLink, kind)")
                .setOrderBy("createdTime asc")
                .execute();

            return new PollOutput(execute.getFiles(), Map.of(LAST_TIME_CHECKED, endDate), false);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
