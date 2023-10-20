/*
 * Copyright 2021 <your company/name>.
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
 */

package com.integri.atlas.file.storage.spel;

import com.integri.atlas.engine.core.context.MapContext;
import com.integri.atlas.engine.core.task.SimpleTaskExecution;
import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.core.task.evaluator.spel.SpelTaskEvaluator;
import com.integri.atlas.file.storage.FileEntry;
import com.integri.atlas.file.storage.base64.Base64FileStorageService;
import java.util.Base64;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class FileStorageServiceSpelTaskEvaluatorTest {

    @Test
    public void testReadFile() {
        Base64FileStorageService base64FileStorageService = new Base64FileStorageService();

        SpelTaskEvaluator evaluator = SpelTaskEvaluator
            .builder()
            .methodExecutor("readFile", new ReadFile(base64FileStorageService))
            .build();

        MapContext mapContext = new MapContext();

        mapContext.put("fileURL", "base64:" + Base64.getEncoder().encodeToString("data".getBytes()));

        TaskExecution taskExecution = evaluator.evaluate(
            SimpleTaskExecution.of("fileContent", "${readFile(fileURL)}"),
            mapContext
        );

        Assertions.assertThat((String) taskExecution.get("fileContent")).isEqualTo("data");
    }

    @Test
    public void testStoreFile() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator
            .builder()
            .methodExecutor("storeFile", new StoreFile(new Base64FileStorageService()))
            .build();

        TaskExecution taskExecution = evaluator.evaluate(
            SimpleTaskExecution.of("fileEntry", "${storeFile('sample.txt', 'data')}"),
            new MapContext()
        );

        Assertions
            .assertThat((FileEntry) taskExecution.get("fileEntry"))
            .hasFieldOrPropertyWithValue("url", "base64:" + Base64.getEncoder().encodeToString("data".getBytes()))
            .hasFieldOrPropertyWithValue("extension", "txt")
            .hasFieldOrPropertyWithValue("mimeType", "text/plain")
            .hasFieldOrPropertyWithValue("name", "sample.txt");
    }
}
