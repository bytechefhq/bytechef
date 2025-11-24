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

package com.bytechef.component.google.drive.util;

import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.APPLICATION_VND_GOOGLE_APPS_FOLDER;
import static com.bytechef.google.commons.GoogleUtils.fetchAllFiles;
import static com.bytechef.google.commons.GoogleUtils.translateGoogleIOException;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FOLDER_ID;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 * @author Monika Kušter
 */
public class GoogleDriveUtils {

    protected static final String LAST_TIME_CHECKED = "lastTimeChecked";

    private GoogleDriveUtils() {
    }

    public static PollOutput getPollOutput(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext triggerContext, boolean newFile) {

        Drive drive = GoogleServices.getDrive(connectionParameters);
        String mimeType = newFile
            ? "mimeType != '" + APPLICATION_VND_GOOGLE_APPS_FOLDER + "'"
            : "mimeType = '" + APPLICATION_VND_GOOGLE_APPS_FOLDER + "'";
        boolean editorEnvironment = triggerContext.isEditorEnvironment();
        Instant now = Instant.now();

        Instant startDate = closureParameters.get(
            LAST_TIME_CHECKED, Instant.class, editorEnvironment ? now.minus(Duration.ofHours(3)) : now);

        String timestamp = DateTimeFormatter.ISO_INSTANT.format(startDate);

        List<File> files = new ArrayList<>();
        int pageSize = editorEnvironment ? 1 : 1000;
        String nextPageToken = null;

        do {
            FileList fileList;

            try {
                fileList = drive.files()
                    .list()
                    .setQ(mimeType + " and '" + inputParameters.getRequiredString(FOLDER_ID)
                        + "' in parents and " +
                        "trashed = false and createdTime > '" + timestamp + "'")
                    .setFields("files(id, name, mimeType, webViewLink, kind)")
                    .setOrderBy("createdTime desc")
                    .setPageSize(pageSize)
                    .setPageToken(nextPageToken)
                    .execute();
            } catch (IOException e) {
                throw translateGoogleIOException(e);
            }

            files.addAll(fileList.getFiles());
            nextPageToken = fileList.getNextPageToken();

            if (editorEnvironment) {
                break;
            }
        } while (nextPageToken != null);

        return new PollOutput(files, Map.of(LAST_TIME_CHECKED, now), false);
    }

    public static List<File> listFiles(String folderId, boolean isEqualMimetype, Parameters connectionParameters) {
        String operator = isEqualMimetype ? "=" : "!=";
        String query = "mimeType %s '%s' and trashed = false and parents in '%s'".formatted(
            operator, APPLICATION_VND_GOOGLE_APPS_FOLDER, folderId);

        return fetchAllFiles(connectionParameters, query);
    }
}
