
/*
 * Copyright 2021 <your company/name>.
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

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.Map;
import java.util.UUID;

/**
 * @author Ivica Cardic
 */
public class FilesystemMkdirActionTest {

    @Test
    public void testCreateDir1() {
        String tempDir = System.getProperty("java.io.tmpdir") + "/" + UUID.randomUUID()
            .toString()
            .replace("-", "");

        Map<String, ?> inputParameters = Map.of("path", tempDir);

        FilesystemMkdirAction.executeMkdir(Mockito.mock(Context.class), inputParameters);

        Assertions.assertTrue(new File(tempDir).exists());
    }

    @Test
    public void testCreateDir2() {
        Assertions.assertThrows(ComponentExecutionException.class, () -> {
            Map<String, ?> inputParameters = Map.of("path", "/no/such/thing");

            FilesystemMkdirAction.executeMkdir(Mockito.mock(Context.class), inputParameters);
        });
    }
}
