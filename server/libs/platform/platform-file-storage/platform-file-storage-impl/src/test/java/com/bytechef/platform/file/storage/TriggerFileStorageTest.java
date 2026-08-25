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

package com.bytechef.platform.file.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.commons.util.CompressionUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
public class TriggerFileStorageTest {

    private final FileStorageService fileStorageService = new Base64FileStorageService();
    private final TriggerFileStorage triggerFileStorage = new TriggerFileStorageImpl(fileStorageService);

    @Test
    public void testTriggerExecutionOutputPreservesTemporalValues() {
        Object output = Map.of("createdDate", Timestamp.from(Instant.parse("2026-08-26T00:00:00Z")));

        FileEntry fileEntry = triggerFileStorage.storeTriggerExecutionOutput(1L, output);

        assertEquals(
            Map.of("createdDate", ZonedDateTime.parse("2026-08-26T00:00:00Z")),
            triggerFileStorage.readTriggerExecutionOutput(fileEntry));
    }

    @Test
    public void testTriggerExecutionOutputPreservesBigDecimal() {
        Object output = Map.of("amount", new BigDecimal("1234.56"));

        FileEntry fileEntry = triggerFileStorage.storeTriggerExecutionOutput(2L, output);

        assertEquals(Map.of("amount", new BigDecimal("1234.56")), triggerFileStorage.readTriggerExecutionOutput(
            fileEntry));
    }

    @Test
    public void testTriggerExecutionOutputLeavesStringsAsStrings() {
        Object output = Map.of("modifiedDate", "2026-08-24T22:00:00Z");

        FileEntry fileEntry = triggerFileStorage.storeTriggerExecutionOutput(3L, output);

        assertEquals(output, triggerFileStorage.readTriggerExecutionOutput(fileEntry));
    }

    @Test
    public void testTriggerExecutionOutputReadsLegacyUntaggedJson() {
        Map<String, ?> legacyOutput = Map.of("name", "Acme", "count", 5);

        FileEntry fileEntry = fileStorageService.storeFileContent(
            "outputs/workflow_trigger_executions", "legacy.json",
            CompressionUtils.compress(JsonUtils.write(legacyOutput)));

        assertEquals(legacyOutput, triggerFileStorage.readTriggerExecutionOutput(fileEntry));
    }
}
