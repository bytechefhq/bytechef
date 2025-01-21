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

package com.bytechef.google.commons;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FILE_ID;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FILE_NAME;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FOLDER_ID;

import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.OptionsDataSource.TriggerOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Monika Kušter
 */
public class GoogleUtils {

    public static File copyFileOnGoogleDrive(Parameters connectionParameters, Parameters inputParameters)
        throws IOException {
        Drive drive = GoogleServices.getDrive(connectionParameters);
        String fileId = inputParameters.getRequiredString(FILE_ID);
        String folder = inputParameters.getString(FOLDER_ID);

        File originalFile = drive.files()
            .get(fileId)
            .execute();

        File newFile = new File()
            .setName(inputParameters.getRequiredString(FILE_NAME))
            .setParents(folder == null ? originalFile.getParents() : List.of(folder))
            .setMimeType(originalFile.getMimeType());

        return drive
            .files()
            .copy(fileId, newFile)
            .execute();
    }

    public static ActionOptionsFunction<String> getFileOptionsByMimeType(String mimeType, boolean isEqualMimetype) {
        return (inputParameters, connectionParameters, arrayIndex, searchText, context) -> getFileOptions(mimeType,
            isEqualMimetype, connectionParameters);
    }

    public static TriggerOptionsFunction<String> getFileOptionsByMimeTypeForTriggers(
        String mimeType, boolean isEqualMimetype) {

        return (inputParameters, connectionParameters, arrayIndex, searchText, context) -> getFileOptions(mimeType,
            isEqualMimetype, connectionParameters);
    }

    private static List<Option<String>> getFileOptions(
        String mimeType, boolean isEqualMimetype, Parameters connectionParameters) throws IOException {

        Drive drive = GoogleServices.getDrive(connectionParameters);
        String operator = isEqualMimetype ? "=" : "!=";
        String query = String.format("mimeType %s '%s' and trashed = false", operator, mimeType);

        return drive.files()
            .list()
            .setQ(query)
            .execute()
            .getFiles()
            .stream()
            .map(folder -> (Option<String>) option(folder.getName(), folder.getId()))
            .toList();
    }
}
