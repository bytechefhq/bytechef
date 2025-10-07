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

package com.bytechef.component.google.sheets.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.FOLDER_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.TITLE;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.bytechef.google.commons.GoogleUtils;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import java.io.IOException;

/**
 * @author Marija Horvat
 */
public class GoogleSheetsCreateSpreadsheetAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createSpreadsheet")
        .title("Create Spreadsheet")
        .description("Create a new spreadsheet in a specified folder.")
        .properties(
            string(TITLE)
                .label("Title")
                .description("Title of the new spreadsheet to be created.")
                .required(true),
            string(FOLDER_ID)
                .label("Folder ID")
                .description(
                    "ID of the folder where the new spreadsheet will be stored. If no folder is selected, the folder " +
                        "will be created in the root folder.")
                .options(GoogleUtils.getFileOptionsByMimeType("application/vnd.google-apps.folder", true))
                .required(false))
        .output()
        .perform(GoogleSheetsCreateSpreadsheetAction::perform);

    private GoogleSheetsCreateSpreadsheetAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Spreadsheet spreadsheet = new Spreadsheet()
            .setProperties(
                new SpreadsheetProperties()
                    .setTitle(inputParameters.getRequiredString(TITLE)));

        Sheets sheets = GoogleServices.getSheets(connectionParameters);

        Spreadsheet newSpreadsheet;
        try {
            newSpreadsheet = sheets.spreadsheets()
                .create(spreadsheet)
                .execute();
        } catch (IOException e) {
            throw GoogleUtils.translateGoogleIOException(e);
        }

        String folderId = inputParameters.getString(FOLDER_ID);

        if (folderId != null) {
            moveSpreadsheetToFolder(connectionParameters, newSpreadsheet.getSpreadsheetId(), folderId);
        }

        return newSpreadsheet;
    }

    private static void moveSpreadsheetToFolder(
        Parameters connectionParameters, String spreadsheetId, String folderId) {

        Drive drive = GoogleServices.getDrive(connectionParameters);

        File newFile;
        try {
            newFile = drive.files()
                .get(spreadsheetId)
                .setFields("parents")
                .execute();
        } catch (IOException e) {
            throw GoogleUtils.translateGoogleIOException(e);
        }

        try {
            drive.files()
                .update(spreadsheetId, null)
                .setAddParents(folderId)
                .setRemoveParents(String.join(",", newFile.getParents()))
                .setFields("id, parents")
                .execute();
        } catch (IOException e) {
            throw GoogleUtils.translateGoogleIOException(e);
        }
    }
}
