package com.bytechef.component.google.drive.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.FILE_ID;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.FILE_NAME;
import static com.bytechef.component.google.drive.constant.GoogleDriveConstants.PARENT_FOLDER;

import java.io.IOException;
import java.util.Collections;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.drive.util.GoogleDriveUtils;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

public class GoogleDriveCopyFileAction {

        public static final ModifiableActionDefinition ACTION_DEFINITION = action("copyFile")
                        .title("Copy file")
                        .description("Copy a selected file to a different location within Google Drive.")
                        .properties(
                                        string(FILE_ID)
                                                        .label("File")
                                                        .description("The id of the file to be copied.")
                                                        .options((ActionOptionsFunction<String>) GoogleDriveUtils::getFileOptions)
                                                        .required(true),
                                        string(FILE_NAME)
                                                        .label("New file name")
                                                        .description("The name of the new file created as a result of the copy operation.")
                                                        .required(true),
                                        string(PARENT_FOLDER)
                                                        .label("Destination folder")
                                                        .required(true)
                                                        .description(
                                                                        "The ID of the folder where the copied file will be stored.")
                                                        .options((ActionOptionsFunction<String>) GoogleDriveUtils::getFolderOptions))
                        .output(outputSchema(fileEntry()))
                        .perform(GoogleDriveCopyFileAction::perform);

        private GoogleDriveCopyFileAction() {
        }

        public static File perform(Parameters inputParameters, Parameters connectionParameters,
                        ActionContext actionContext) throws IOException {
                Drive drive = GoogleServices.getDrive(connectionParameters);
                File originalFile = drive.files().get(inputParameters.getRequiredString(FILE_ID)).execute();
                // Setting parent to copy it to correct destination folder
                File newFile = new File()
                                .setName(inputParameters.getRequiredString(FILE_NAME))
                                .setParents(Collections.singletonList(inputParameters.getRequiredString(PARENT_FOLDER)))
                                .setMimeType(originalFile.getMimeType());

                return drive.files().copy(inputParameters.getRequiredString(FILE_ID), newFile).execute();
        }

}
