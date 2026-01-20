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

package com.bytechef.component.filesystem.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.sampleOutput;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.filesystem.constant.FilesystemConstants.FILENAME;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.nio.file.NoSuchFileException;

/**
 * Filesystem get parent folder action for workflow automation. Returns the parent directory of a specified file path.
 *
 * @author Ivica Cardic
 */
public class FilesystemGetParentFolderAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getFilePath")
        .title("Get Parent Folder")
        .description("Gets the path of the parent folder of the file. If the file doesn't exist, it throws an error.")
        .properties(
            string(FILENAME)
                .label("File path")
                .description("The path to full filename.")
                .placeholder("/data/your_file.pdf")
                .required(true))
        .output(
            outputSchema(string().description("The path of the parent folder of the file.")),
            sampleOutput("/sample_data"))
        .perform(FilesystemGetParentFolderAction::perform);

    private FilesystemGetParentFolderAction() {
    }

    /**
     * Gets the full path from a full filename, which is the prefix + path, and also excluding the final directory
     * separator.
     *
     * <p>
     * This method will handle a file in either Unix or Windows format. The method is entirely text based and returns
     * the text before the last forward or backslash.
     *
     * <p>
     * <b>Security Note:</b> Path traversal is intentional for this component. The Filesystem component is designed to
     * allow workflow creators to access file paths as part of their automation workflows. Access to this component
     * should be restricted through workflow-level permissions and proper access control. The file path is provided by
     * the workflow creator, not end users.
     */
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    protected static String perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) throws NoSuchFileException {
        String filename = inputParameters.getRequiredString(FILENAME);
        File file = new File(filename);

        if (file.exists()) {
            return filename.substring(0, filename.lastIndexOf(File.separator));
        } else {
            throw new NoSuchFileException(filename);
        }
    }
}
