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

package com.bytechef.platform.knowledgebase.worker.etl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

class KnowledgeBaseVectorStoreWriterTest {

    private final VectorStore vectorStore = mock(VectorStore.class);
    private final KnowledgeBaseVectorStoreWriter writer = new KnowledgeBaseVectorStoreWriter(vectorStore);

    @AfterEach
    void tearDown() {
        EnvironmentContext.clear();
    }

    @Test
    void testSetsEnvironmentDuringAddAndClearsAfter() {
        AtomicReference<Environment> observed = new AtomicReference<>();

        doAnswer(invocation -> {
            observed.set(EnvironmentContext.getCurrentEnvironment());

            return null;
        }).when(vectorStore)
            .add(anyList());

        writer.write(List.of(new Document("hello")), 1L, 2L, Environment.STAGING.ordinal(), List.of());

        assertThat(observed.get()).isEqualTo(Environment.STAGING);
        assertThat(EnvironmentContext.getCurrentEnvironment()).isEqualTo(Environment.PRODUCTION);
    }

    @Test
    void testWriteChunkSetsEnvironmentDuringAddAndClearsAfter() {
        AtomicReference<Environment> observed = new AtomicReference<>();

        doAnswer(invocation -> {
            observed.set(EnvironmentContext.getCurrentEnvironment());

            return null;
        }).when(vectorStore)
            .add(anyList());

        writer.writeChunk(new Document("hello"), 1L, 2L, 3L, Environment.STAGING.ordinal(), List.of());

        assertThat(observed.get()).isEqualTo(Environment.STAGING);
        assertThat(EnvironmentContext.getCurrentEnvironment()).isEqualTo(Environment.PRODUCTION);
    }
}
