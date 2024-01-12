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

import static com.bytechef.component.filesystem.constant.FilesystemConstants.PATH;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
public class FilesystemRmActionTest {

    @Test
    public void testDelete() throws IOException {
        Parameters parameters = Mockito.mock(Parameters.class);
        File tempDir = java.nio.file.Files.createTempDirectory("rm_")
            .toFile();

        Assertions.assertTrue(tempDir.exists());

        Mockito.when(parameters.getRequiredString(Mockito.eq(PATH)))
            .thenReturn(tempDir.getAbsolutePath());

        FilesystemRmAction.perform(parameters, parameters, Mockito.mock(ActionContext.class));

        Assertions.assertFalse(tempDir.exists());
    }
}
