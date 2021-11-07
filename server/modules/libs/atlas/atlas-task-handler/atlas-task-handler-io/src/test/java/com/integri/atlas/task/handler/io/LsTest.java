/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.task.handler.io;

import com.integri.atlas.engine.core.task.SimpleTaskExecution;
import com.integri.atlas.task.handler.io.Ls.FileInfo;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

public class LsTest {

    @Test
    public void test1() throws IOException {
        Ls ls = new Ls();
        ClassPathResource cpr = new ClassPathResource("ls");
        SimpleTaskExecution task = new SimpleTaskExecution();
        task.set("path", cpr.getFile().getAbsolutePath());
        task.set("recursive", true);
        List<FileInfo> files = ls.handle(task);
        Assertions.assertEquals(
            Set.of("C.txt", "B.txt", "A.txt"),
            files.stream().map(FileInfo::getName).collect(Collectors.toSet())
        );
    }

    @Test
    public void test2() throws IOException {
        Ls ls = new Ls();
        ClassPathResource cpr = new ClassPathResource("ls");
        SimpleTaskExecution task = new SimpleTaskExecution();
        task.set("path", cpr.getFile().getAbsolutePath());
        task.set("recursive", true);
        List<FileInfo> files = ls.handle(task);
        Assertions.assertEquals(
            Set.of("sub1/C.txt", "B.txt", "A.txt"),
            files.stream().map(FileInfo::getRelativePath).collect(Collectors.toSet())
        );
    }

    @Test
    public void test3() throws IOException {
        Ls ls = new Ls();
        ClassPathResource cpr = new ClassPathResource("ls");
        SimpleTaskExecution task = new SimpleTaskExecution();
        task.set("path", cpr.getFile().getAbsolutePath());
        List<FileInfo> files = ls.handle(task);
        Assertions.assertEquals(
            Set.of("B.txt", "A.txt"),
            files.stream().map(FileInfo::getName).collect(Collectors.toSet())
        );
    }
}
