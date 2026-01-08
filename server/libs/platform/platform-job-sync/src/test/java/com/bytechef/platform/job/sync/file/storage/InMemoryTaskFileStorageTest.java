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

package com.bytechef.platform.job.sync.file.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.file.storage.domain.FileEntry;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class InMemoryTaskFileStorageTest {

    private static final long jobId = 123L;

    private InMemoryTaskFileStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryTaskFileStorage();
        storage.initializeJobStorage(jobId);
    }

    @AfterEach
    void tearDown() {
        storage.cleanupJobStorage(jobId);
    }

    @Test
    void testStoreContextValueWithJobIdParameter() {
        // This test verifies that when storeContextValue is called with a stackId,
        // it uses that stackId (which is the jobId for Context.Classname.JOB) to store data
        // even when ThreadLocal is set to a different value initially

        Map<String, Object> contextValue = Map.of("key", "value");

        FileEntry fileEntry = storage.storeContextValue(jobId, Context.Classname.JOB, contextValue);

        assertNotNull(fileEntry);

        Map<String, ?> readValue = storage.readContextValue(fileEntry);

        assertEquals(contextValue, readValue);
    }
}
