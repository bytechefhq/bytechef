
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

import com.bytechef.component.filesystem.FilesystemComponentHandlerTest;
import com.bytechef.hermes.component.definition.Context;
import com.bytechef.hermes.component.util.MapUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.bytechef.component.filesystem.constant.FilesystemConstants.PATH;
import static com.bytechef.component.filesystem.constant.FilesystemConstants.RECURSIVE;

/**
 * @author Ivica Cardic
 */
public class FilesystemLsActionTest {

    @Test
    public void testLs1() {
        File file = getLsFile();

        try (MockedStatic<MapUtils> mockedStatic = Mockito.mockStatic(MapUtils.class)) {
            mockedStatic.when(() -> Paths.get(MapUtils.getRequiredString(Mockito.anyMap(), Mockito.eq(PATH))))
                .thenReturn(file.getAbsolutePath());
            mockedStatic.when(() -> MapUtils.getBoolean(
                Mockito.anyMap(), Mockito.eq(RECURSIVE), Mockito.eq(false)))
                .thenReturn(true);

            List<FilesystemLsAction.FileInfo> files = FilesystemLsAction.perform(
                Map.of(), Mockito.mock(Context.class));

            Assertions.assertEquals(
                Set.of("C.txt", "B.txt", "A.txt"),
                files.stream()
                    .map(FilesystemLsAction.FileInfo::getName)
                    .collect(Collectors.toSet()));
        }
    }

    @Test
    public void testLs2() {
        File file = getLsFile();

        try (MockedStatic<MapUtils> mockedStatic = Mockito.mockStatic(MapUtils.class)) {
            mockedStatic.when(() -> Paths.get(MapUtils.getRequiredString(Mockito.anyMap(), Mockito.eq(PATH))))
                .thenReturn(file.getAbsolutePath());
            mockedStatic.when(() -> MapUtils.getBoolean(
                Mockito.anyMap(), Mockito.eq(RECURSIVE), Mockito.eq(false)))
                .thenReturn(true);

            List<FilesystemLsAction.FileInfo> files = FilesystemLsAction.perform(
                Map.of(), Mockito.mock(Context.class));

            Assertions.assertEquals(
                Set.of("sub1/C.txt", "B.txt", "A.txt"),
                files.stream()
                    .map(FilesystemLsAction.FileInfo::getRelativePath)
                    .collect(Collectors.toSet()));
        }
    }

    @Test
    public void testLs3() {
        File file = getLsFile();

        try (MockedStatic<MapUtils> mockedStatic = Mockito.mockStatic(MapUtils.class)) {
            mockedStatic.when(() -> Paths.get(MapUtils.getRequiredString(Mockito.anyMap(), Mockito.eq(PATH))))
                .thenReturn(file.getAbsolutePath());
            mockedStatic.when(() -> MapUtils.getBoolean(
                Mockito.anyMap(), Mockito.eq(RECURSIVE), Mockito.eq(false)))
                .thenReturn(false);

            List<FilesystemLsAction.FileInfo> files = FilesystemLsAction.perform(
                Map.of(), Mockito.mock(Context.class));

            Assertions.assertEquals(
                Set.of("B.txt", "A.txt"),
                files.stream()
                    .map(FilesystemLsAction.FileInfo::getName)
                    .collect(Collectors.toSet()));
        }
    }

    private static File getLsFile() {
        return new File(FilesystemComponentHandlerTest.class
            .getClassLoader()
            .getResource("dependencies/ls")
            .getFile());
    }
}
