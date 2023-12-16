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

package com.bytechef.component.filesystem.action;

import static com.bytechef.component.filesystem.constant.FilesystemConstants.FILENAME;
import static com.bytechef.component.filesystem.constant.FilesystemConstants.FILE_ENTRY;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.filesystem.FilesystemComponentHandlerTest;
import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.ParameterMap;
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
public class FilesystemWriteFileActionTest {

    @Test
    @SuppressFBWarnings("OBL")
    public void testPerformWriteFile() throws IOException {
        ActionContext context = Mockito.mock(ActionContext.class);
        File file = getSampleFile();
        ParameterMap parameterMap = Mockito.mock(ParameterMap.class);

        Mockito.when(context.file(file1 -> file1.getStream(Mockito.any(ActionContext.FileEntry.class))))
            .thenReturn(new FileInputStream(file));

        Mockito.when(parameterMap.getRequiredFileEntry(Mockito.eq(FILE_ENTRY)))
            .thenReturn(Mockito.mock(ActionContext.FileEntry.class));
        Mockito.when(parameterMap.getRequiredString(Mockito.eq(FILENAME)))
            .thenReturn(file.getAbsolutePath());

        assertThat(FilesystemWriteFileAction.perform(parameterMap, parameterMap, context))
            .hasFieldOrPropertyWithValue("bytes", 5L);
    }

    private File getSampleFile() {
        return new File(FilesystemComponentHandlerTest.class
            .getClassLoader()
            .getResource("dependencies/sample.txt")
            .getFile());
    }
}
