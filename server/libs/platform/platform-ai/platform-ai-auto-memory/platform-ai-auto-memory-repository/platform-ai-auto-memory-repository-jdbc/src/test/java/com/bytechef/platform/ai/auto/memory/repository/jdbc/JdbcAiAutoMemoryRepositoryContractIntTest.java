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

package com.bytechef.platform.ai.auto.memory.repository.jdbc;

import com.bytechef.platform.ai.auto.memory.repository.AiAutoMemoryRepository;
import com.bytechef.platform.ai.auto.memory.repository.AiAutoMemoryRepositoryContractTests;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Runs the shared repository contract against the JDBC binding, so the relational backend and the file-storage backends
 * are held to the same observable behavior.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AiAutoMemoryRepositoryIntTest.IntTestConfiguration.class)
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
public class JdbcAiAutoMemoryRepositoryContractIntTest extends AiAutoMemoryRepositoryContractTests {

    @Autowired
    private AiAutoMemoryRepository aiAutoMemoryRepository;

    @AfterEach
    void afterEach() {
        aiAutoMemoryRepository.deleteAll();
    }

    @Override
    protected AiAutoMemoryRepository getAiAutoMemoryRepository() {
        return aiAutoMemoryRepository;
    }
}
