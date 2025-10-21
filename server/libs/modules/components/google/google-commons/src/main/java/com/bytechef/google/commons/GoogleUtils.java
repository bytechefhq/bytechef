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

package com.bytechef.google.commons;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FILE_ID;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FILE_NAME;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FOLDER_ID;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.exception.ProviderException;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Setting;
import com.google.api.services.calendar.model.Settings;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Monika Kušter
 */
public class GoogleUtils {

    public static File copyFileOnGoogleDrive(Parameters connectionParameters, Parameters inputParameters) {
        Drive drive = GoogleServices.getDrive(connectionParameters);
        String fileId = inputParameters.getRequiredString(FILE_ID);
        String folder = inputParameters.getString(FOLDER_ID);

        File originalFile;
        try {
            originalFile = drive.files()
                .get(fileId)
                .execute();
        } catch (IOException e) {
            throw translateGoogleIOException(e);
        }

        File newFile = new File()
            .setName(inputParameters.getRequiredString(FILE_NAME))
            .setParents(folder == null ? originalFile.getParents() : List.of(folder))
            .setMimeType(originalFile.getMimeType());

        try {
            return drive
                .files()
                .copy(fileId, newFile)
                .execute();
        } catch (IOException e) {
            throw translateGoogleIOException(e);
        }
    }

    public static OptionsFunction<String> getFileOptionsByMimeType(String mimeType, boolean isEqualMimetype) {
        return (inputParameters, connectionParameters, arrayIndex, searchText, context) -> getFileOptions(mimeType,
            isEqualMimetype, connectionParameters);
    }

    public static OptionsFunction<String> getFileOptionsByMimeTypeForTriggers(
        String mimeType, boolean isEqualMimetype) {

        return (inputParameters, connectionParameters, arrayIndex, searchText, context) -> getFileOptions(mimeType,
            isEqualMimetype, connectionParameters);
    }

    private static List<Option<String>> getFileOptions(
        String mimeType, boolean isEqualMimetype, Parameters connectionParameters) {

        String operator = isEqualMimetype ? "=" : "!=";
        String query = String.format("mimeType %s '%s' and trashed = false", operator, mimeType);

        List<File> files = fetchAllFiles(connectionParameters, query);

        return files.stream()
            .map(folder -> option(folder.getName(), folder.getId()))
            .collect(Collectors.toList());
    }

    public static List<File> fetchAllFiles(Parameters connectionParameters, String query) {
        Drive drive = GoogleServices.getDrive(connectionParameters);

        List<File> files = new ArrayList<>();
        String nextPageToken = null;

        do {
            FileList fileList;
            try {
                fileList = drive.files()
                    .list()
                    .setQ(query)
                    .setPageSize(1000)
                    .setPageToken(nextPageToken)
                    .execute();
            } catch (IOException e) {
                throw translateGoogleIOException(e);
            }

            files.addAll(fileList.getFiles());
            nextPageToken = fileList.getNextPageToken();
        } while (nextPageToken != null);
        return files;
    }

    public static String getCalendarTimezone(Calendar calendar) {
        List<Setting> settings = fetchAllCalendarSettings(calendar);

        return settings.stream()
            .filter(setting -> Objects.equals(setting.getId(), "timezone"))
            .findFirst()
            .map(Setting::getValue)
            .orElseThrow(() -> new ProviderException("Timezone setting not found."));
    }

    private static List<Setting> fetchAllCalendarSettings(Calendar calendar) {
        List<Setting> allSettings = new ArrayList<>();

        String nextPageToken = null;

        do {
            Settings settings;
            try {
                settings = calendar.settings()
                    .list()
                    .setMaxResults(250)
                    .setPageToken(nextPageToken)
                    .execute();
            } catch (IOException e) {
                throw translateGoogleIOException(e);
            }

            allSettings.addAll(settings.getItems());
            nextPageToken = settings.getNextPageToken();
        } while (nextPageToken != null);

        return allSettings;
    }

    public static ProviderException processErrorResponse(int statusCode, Object body, Context context) {
        String message;

        Object json = context.json(json1 -> json1.read((String) body));

        if (json instanceof Map<?, ?> map && map.get("error") instanceof Map<?, ?> error) {
            message = (String) error.get("message");
        } else {
            message = body == null ? null : body.toString();
        }

        return new ProviderException(statusCode, message);
    }

    public static ProviderException translateGoogleIOException(IOException e) {
        if (e instanceof GoogleJsonResponseException googleException) {
            GoogleJsonError googleJsonError = googleException.getDetails();
            int statusCode = googleException.getStatusCode();

            return new ProviderException(
                statusCode, googleJsonError == null ? googleException.getMessage() : googleJsonError.getMessage());
        }

        return new ProviderException(e);
    }
}
