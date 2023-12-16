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
import static com.bytechef.component.filesystem.constant.FilesystemConstants.RECURSIVE;

import com.bytechef.component.filesystem.FilesystemComponentHandlerTest;
import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.ParameterMap;
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
public class FilesystemLsActionTest {

    @Test
    public void testLs1() {
        File file = getLsFile();
        ParameterMap parameterMap = Mockito.mock(ParameterMap.class);

        Mockito.when(parameterMap.getRequiredString(Mockito.eq(PATH)))
            .thenReturn(file.getAbsolutePath());
        Mockito.when(parameterMap.getBoolean(Mockito.eq(RECURSIVE), Mockito.eq(false)))
            .thenReturn(true);

        List<FilesystemLsAction.FileInfo> files = (List<FilesystemLsAction.FileInfo>) FilesystemLsAction.perform(
            parameterMap, parameterMap, Mockito.mock(ActionContext.class));

        Assertions.assertEquals(
            Set.of("C.txt", "B.txt", "A.txt"),
            files.stream()
                .map(FilesystemLsAction.FileInfo::getName)
                .collect(Collectors.toSet()));
    }

    @Test
    public void testLs2() {
        File file = getLsFile();
        ParameterMap parameterMap = Mockito.mock(ParameterMap.class);

        Mockito.when(parameterMap.getRequiredString(Mockito.eq(PATH)))
            .thenReturn(file.getAbsolutePath());
        Mockito.when(parameterMap.getBoolean(Mockito.eq(RECURSIVE), Mockito.eq(false)))
            .thenReturn(true);

        List<FilesystemLsAction.FileInfo> files = (List<FilesystemLsAction.FileInfo>) FilesystemLsAction.perform(
            parameterMap, parameterMap, Mockito.mock(ActionContext.class));

        Assertions.assertEquals(
            Set.of("sub1/C.txt", "B.txt", "A.txt"),
            files.stream()
                .map(FilesystemLsAction.FileInfo::getRelativePath)
                .collect(Collectors.toSet()));
    }

    @Test
    public void testLs3() {
        File file = getLsFile();
        ParameterMap parameterMap = Mockito.mock(ParameterMap.class);

        Mockito.when(parameterMap.getRequiredString(Mockito.eq(PATH)))
            .thenReturn(file.getAbsolutePath());
        Mockito.when(parameterMap.getBoolean(Mockito.eq(RECURSIVE), Mockito.eq(false)))
            .thenReturn(false);

        List<FilesystemLsAction.FileInfo> files = (List<FilesystemLsAction.FileInfo>) FilesystemLsAction.perform(
            parameterMap, parameterMap, Mockito.mock(ActionContext.class));

        Assertions.assertEquals(
            Set.of("B.txt", "A.txt"),
            files.stream()
                .map(FilesystemLsAction.FileInfo::getName)
                .collect(Collectors.toSet()));
    }

    private static File getLsFile() {
        return new File(FilesystemComponentHandlerTest.class
            .getClassLoader()
            .getResource("dependencies/ls")
            .getFile());
    }
}
