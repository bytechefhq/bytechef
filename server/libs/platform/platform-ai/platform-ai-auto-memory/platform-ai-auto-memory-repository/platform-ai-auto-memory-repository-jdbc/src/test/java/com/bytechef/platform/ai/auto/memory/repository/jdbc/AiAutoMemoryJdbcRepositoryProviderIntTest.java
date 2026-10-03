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

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.ai.auto.memory.repository.AiAutoMemoryRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * With a file-backed provider selected, the JDBC binding must stay off even though a JDBC configuration is present;
 * otherwise the file-storage binding's repository would be a second {@link AiAutoMemoryRepository} and the server would
 * fail to start. The default case — no provider set, JDBC on — is what every other test in this module runs with.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = AiAutoMemoryRepositoryIntTest.IntTestConfiguration.class,
    properties = "bytechef.ai.auto-memory.provider=filesystem")
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
public class AiAutoMemoryJdbcRepositoryProviderIntTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void testAFileBackedProviderSwitchesTheJdbcBindingOff() {
        assertThat(applicationContext.getBeanNamesForType(AiAutoMemoryRepository.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(AiAutoMemoryOwnerGuard.class)).isEmpty();
    }
}
