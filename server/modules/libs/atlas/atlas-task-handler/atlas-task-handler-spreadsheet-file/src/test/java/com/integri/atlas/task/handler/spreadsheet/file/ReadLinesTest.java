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

package com.integri.atlas.task.handler.spreadsheet.file;

import com.integri.atlas.engine.core.task.SimpleTaskExecution;
import com.integri.atlas.task.handler.spreadsheet.file.ReadLines;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Igor Beslic
 */
public class ReadLinesTest {

    @Test
    public void testReadLines() throws IOException {
        ReadLines readLines = new ReadLines();

        ClassPathResource cpr = new ClassPathResource("ls/test_1.csv");

        SimpleTaskExecution task = new SimpleTaskExecution();

        task.set("path", cpr.getFile().getAbsolutePath());
        task.set("containsHeader", true);

        List<String> lines = readLines.handle(task);

        Assertions.assertEquals(
            Set.of("A;B;C;D", "1;2;3;4", "A;1;B;2", "ABCD;1234;EFGH;5678"),
            lines.stream().collect(Collectors.toSet())
        );
    }

    @Test
    public void testReadLinesNoHeaders() throws IOException {
        ReadLines readLines = new ReadLines();
        ClassPathResource cpr = new ClassPathResource("ls/test_2.csv");

        SimpleTaskExecution task = new SimpleTaskExecution();

        task.set("path", cpr.getFile().getAbsolutePath());
        task.set("containsHeader", false);

        List<String> lines = readLines.handle(task);

        Assertions.assertEquals(
            Set.of("A;B;C;D", "1;2;3;4", "A;1;B;2", "ABCD;1234;EFGH;5678"),
            lines.stream().collect(Collectors.toSet())
        );
    }
}
