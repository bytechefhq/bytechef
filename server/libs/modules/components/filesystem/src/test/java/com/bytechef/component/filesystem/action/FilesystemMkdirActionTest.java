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

import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.ParameterMap;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
import java.io.File;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
public class FilesystemMkdirActionTest {

    @Test
    public void testCreateDir1() {
        ParameterMap parameterMap = Mockito.mock(ParameterMap.class);
        String tempDir = System.getProperty("java.io.tmpdir") + "/" + UUID.randomUUID()
            .toString()
            .replace("-", "");

        Mockito.when(parameterMap.getRequiredString(Mockito.eq(PATH)))
            .thenReturn(tempDir);

        FilesystemMkdirAction.perform(parameterMap, parameterMap, Mockito.mock(ActionContext.class));

        Assertions.assertTrue(new File(tempDir).exists());
    }

    @Test
    public void testCreateDir2() {
        ParameterMap parameterMap = Mockito.mock(ParameterMap.class);

        Assertions.assertThrows(ComponentExecutionException.class, () -> {
            Mockito.when(parameterMap.getRequiredString(Mockito.eq(PATH)))
                .thenReturn("/no/such/thing");

            FilesystemMkdirAction.perform(parameterMap, parameterMap, Mockito.mock(ActionContext.class));
        });

    }
}
