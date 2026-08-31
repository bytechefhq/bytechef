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

package com.bytechef.atlas.file.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
public class TaskFileStorageTest {

    private final TaskFileStorage taskFileStorage = new TaskFileStorageImpl(new Base64FileStorageService());

    @Test
    public void testTaskExecutionOutputPreservesTemporalValues() {
        Object output = List.of(Map.of("APPLYDATE", Timestamp.from(Instant.parse("2026-08-26T00:00:00Z"))));

        FileEntry fileEntry = taskFileStorage.storeTaskExecutionOutput(1L, 1L, output);

        assertEquals(
            List.of(Map.of("APPLYDATE", ZonedDateTime.parse("2026-08-26T00:00:00Z"))),
            taskFileStorage.readTaskExecutionOutput(fileEntry));
    }

    @Test
    public void testTaskExecutionOutputLeavesStringsAsStrings() {
        Object output = Map.of("latestModifyDate", "2026-08-24T22:00:00Z");

        FileEntry fileEntry = taskFileStorage.storeTaskExecutionOutput(1L, 2L, output);

        assertEquals(output, taskFileStorage.readTaskExecutionOutput(fileEntry));
    }

    @Test
    public void testContextValuePreservesTemporalValues() {
        Map<String, ?> context = Map.of("dbDate", Timestamp.from(Instant.parse("2026-08-26T00:00:00Z")));

        FileEntry fileEntry = taskFileStorage.storeContextValue(1L, Context.Classname.JOB, context);

        assertEquals(
            Map.of("dbDate", ZonedDateTime.parse("2026-08-26T00:00:00Z")),
            taskFileStorage.readContextValue(fileEntry));
    }

    @Test
    public void testJobOutputsPreserveTemporalValues() {
        Map<String, ?> outputs = Map.of("result", Timestamp.from(Instant.parse("2026-08-26T00:00:00Z")));

        FileEntry fileEntry = taskFileStorage.storeJobOutputs(1L, outputs);

        assertEquals(
            Map.of("result", ZonedDateTime.parse("2026-08-26T00:00:00Z")),
            taskFileStorage.readJobOutputs(fileEntry));
    }
}
