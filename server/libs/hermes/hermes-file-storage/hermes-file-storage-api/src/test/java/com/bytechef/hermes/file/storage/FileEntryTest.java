
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

package com.bytechef.hermes.file.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.atlas.task.evaluator.TaskEvaluator;
import com.bytechef.commons.utils.MapUtils;
import com.bytechef.hermes.file.storage.domain.FileEntry;
import java.util.Collections;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class FileEntryTest {

    @Test
    public void testOf1() {
        Assertions.assertThat(new FileEntry("/tmp/fileName.txt"))
            .hasFieldOrPropertyWithValue("extension", "txt")
            .hasFieldOrPropertyWithValue("mimeType", "text/plain")
            .hasFieldOrPropertyWithValue("name", "fileName.txt")
            .hasFieldOrPropertyWithValue("url", "/tmp/fileName.txt");
    }

    @Test
    public void testOf2() {
        Assertions.assertThat(new FileEntry("name.txt", "/tmp/fileName.txt"))
            .hasFieldOrPropertyWithValue("extension", "txt")
            .hasFieldOrPropertyWithValue("mimeType", "text/plain")
            .hasFieldOrPropertyWithValue("name", "name.txt")
            .hasFieldOrPropertyWithValue("url", "/tmp/fileName.txt");
    }

    @Test
    public void testSpelEvaluation() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(
            WorkflowTask.of("result", "${fileEntry.name} ${fileEntry.url}"));

        TaskExecution evaluatedTaskExecution = evaluator.evaluate(
            taskExecution,
            new Context(Collections.singletonMap("fileEntry", new FileEntry("sample.txt", "/tmp/fileName.txt"))));

        assertEquals(
            "sample.txt /tmp/fileName.txt", MapUtils.getString(evaluatedTaskExecution.getParameters(), "result"));
    }
}
