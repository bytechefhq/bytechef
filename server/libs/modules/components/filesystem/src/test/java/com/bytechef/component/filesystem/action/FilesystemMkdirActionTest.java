
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
import com.bytechef.hermes.component.util.MapValueUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.util.Map;
import java.util.UUID;

import static com.bytechef.component.filesystem.constant.FilesystemConstants.PATH;

/**
 * @author Ivica Cardic
 */
public class FilesystemMkdirActionTest {

    @Test
    public void testCreateDir1() {
        String tempDir = System.getProperty("java.io.tmpdir") + "/" + UUID.randomUUID()
            .toString()
            .replace("-", "");

        try (MockedStatic<MapValueUtils> mockedStatic = Mockito.mockStatic(MapValueUtils.class)) {
            mockedStatic.when(() -> MapValueUtils.getRequiredString(Mockito.anyMap(), Mockito.eq(PATH)))
                .thenReturn(tempDir);

            FilesystemMkdirAction.executeMkdir(Mockito.mock(Context.class), Map.of());

            Assertions.assertTrue(new File(tempDir).exists());
        }
    }

    @Test
    public void testCreateDir2() {
        try (MockedStatic<MapValueUtils> mockedStatic = Mockito.mockStatic(MapValueUtils.class)) {
            Assertions.assertThrows(ComponentExecutionException.class, () -> {
                mockedStatic.when(() -> MapValueUtils.getRequiredString(Mockito.anyMap(), Mockito.eq(PATH)))
                    .thenReturn("/no/such/thing");

                FilesystemMkdirAction.executeMkdir(Mockito.mock(Context.class), Map.of());
            });
        }
    }
}
