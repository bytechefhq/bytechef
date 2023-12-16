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
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.filesystem.FilesystemComponentHandlerTest;
import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.ParameterMap;
import java.io.File;
import java.io.InputStream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
@Disabled
public class FilesystemReadFileActionTest {

    @Test
    public void testPerformReadFile() {
        ActionContext context = Mockito.mock(ActionContext.class);
        File file = getSampleFile();
        ParameterMap parameterMap = Mockito.mock(ParameterMap.class);

        Mockito.when(parameterMap.getRequiredString(Mockito.eq(FILENAME)))
            .thenReturn(file.getAbsolutePath());

        FilesystemReadFileAction.perform(parameterMap, parameterMap, context);

        ArgumentCaptor<String> filenameArgumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(context)
            .file(
                file1 -> file1.storeContent(filenameArgumentCaptor.capture(), Mockito.any(InputStream.class)));

        assertThat(filenameArgumentCaptor.getValue()).isEqualTo(file.getAbsolutePath());
    }

    private File getSampleFile() {
        return new File(FilesystemComponentHandlerTest.class
            .getClassLoader()
            .getResource("dependencies/sample.txt")
            .getFile());
    }
}
