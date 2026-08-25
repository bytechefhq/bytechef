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

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ObjectMapperSetupExtension.class)
public class TaskFileStorageSerializationTest {

    private final TaskFileStorage taskFileStorage = new TaskFileStorageImpl(new Base64FileStorageService());

    @Test
    public void testSerializedOutputNeverLeaksTheValueTag() {
        Object secondPrecisionOutput =
            List.of(Map.of("APPLYDATE", Timestamp.from(Instant.parse("2026-08-26T00:00:00Z"))));

        FileEntry secondPrecisionFileEntry = taskFileStorage.storeTaskExecutionOutput(1L, 1L, secondPrecisionOutput);

        assertEquals(
            "[{\"APPLYDATE\":\"2026-08-26T00:00:00Z\"}]",
            JsonUtils.write(taskFileStorage.readTaskExecutionOutput(secondPrecisionFileEntry)));

        Object nanosecondPrecisionOutput =
            List.of(Map.of("APPLYDATE", Timestamp.from(Instant.parse("2026-08-26T00:00:00.123456789Z"))));

        FileEntry nanosecondPrecisionFileEntry =
            taskFileStorage.storeTaskExecutionOutput(1L, 2L, nanosecondPrecisionOutput);

        assertEquals(
            "[{\"APPLYDATE\":\"2026-08-26T00:00:00.123456789Z\"}]",
            JsonUtils.write(taskFileStorage.readTaskExecutionOutput(nanosecondPrecisionFileEntry)));

        Object sqlDateOutput = List.of(Map.of("APPLYDATE", Date.valueOf("2026-08-26")));

        FileEntry sqlDateFileEntry = taskFileStorage.storeTaskExecutionOutput(1L, 3L, sqlDateOutput);

        assertEquals(
            "[{\"APPLYDATE\":\"2026-08-26\"}]",
            JsonUtils.write(taskFileStorage.readTaskExecutionOutput(sqlDateFileEntry)));
    }
}
