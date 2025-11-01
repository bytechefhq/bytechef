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

import static com.bytechef.component.filesystem.constant.FilesystemConstants.FILENAME;
import static com.bytechef.component.filesystem.constant.FilesystemConstants.FILE_ENTRY;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.filesystem.FilesystemComponentHandlerTest;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
@Disabled
class FilesystemWriteFileActionTest {

    @Test
    @SuppressFBWarnings("OBL")
    void testPerformWriteFile() throws IOException {
        ActionContext context = Mockito.mock(ActionContext.class);
        File file = getSampleFile();
        Parameters parameters = Mockito.mock(Parameters.class);

        Mockito.when(context.file(file1 -> file1.getInputStream(Mockito.any(FileEntry.class))))
            .thenReturn(new FileInputStream(file));

        Mockito.when(parameters.getRequiredFileEntry(Mockito.eq(FILE_ENTRY)))
            .thenReturn(Mockito.mock(FileEntry.class));
        Mockito.when(parameters.getRequiredString(Mockito.eq(FILENAME)))
            .thenReturn(file.getAbsolutePath());

        assertThat(FilesystemWriteFileAction.perform(parameters, parameters, context))
            .hasFieldOrPropertyWithValue("bytes", 5L);
    }

    private File getSampleFile() {
        return new File(FilesystemComponentHandlerTest.class
            .getClassLoader()
            .getResource("dependencies/filesystem/sample.txt")
            .getFile());
    }
}
