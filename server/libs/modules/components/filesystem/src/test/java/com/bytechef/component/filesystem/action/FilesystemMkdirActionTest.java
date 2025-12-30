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

import static com.bytechef.component.filesystem.constant.FilesystemConstants.PATH;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("PATH_TRAVERSAL_IN")
class FilesystemMkdirActionTest {

    @Test
    void testCreateDir1() throws IOException {
        Parameters parameters = Mockito.mock(Parameters.class);
        String tempDir = System.getProperty("java.io.tmpdir") + "/" + UUID.randomUUID()
            .toString()
            .replace("-", "");

        Mockito.when(parameters.getRequiredString(Mockito.eq(PATH)))
            .thenReturn(tempDir);

        FilesystemMkdirAction.perform(parameters, parameters, Mockito.mock(ActionContext.class));

        Assertions.assertTrue(new File(tempDir).exists());
    }

    @Test
    void testCreateDir2() {
        Parameters parameters = Mockito.mock(Parameters.class);

        Assertions.assertThrows(IOException.class, () -> {
            Mockito.when(parameters.getRequiredString(Mockito.eq(PATH)))
                .thenReturn("/no/such/thing");

            FilesystemMkdirAction.perform(parameters, parameters, Mockito.mock(ActionContext.class));
        });

    }
}
