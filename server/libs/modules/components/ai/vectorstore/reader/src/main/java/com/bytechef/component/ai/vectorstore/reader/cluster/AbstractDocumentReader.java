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

package com.bytechef.component.ai.vectorstore.reader.cluster;

import com.bytechef.component.ai.vectorstore.reader.util.DocumentReaderConstants;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import java.io.File;
import org.springframework.core.io.FileSystemResource;

/**
 * @author Monika Kušter
 */
public abstract class AbstractDocumentReader {

    protected static FileResult getFile(Parameters inputParameters, Context context) {
        FileEntry fileEntry = inputParameters.getFileEntry(DocumentReaderConstants.DOCUMENT);

        File file = context.file(curfile -> curfile.toTempFile(fileEntry));

        FileSystemResource fileSystemResource = new FileSystemResource(file);

        return new FileResult(fileEntry, fileSystemResource);
    }

    protected record FileResult(FileEntry fileEntry, FileSystemResource fileSystemResource) {
    }
}
