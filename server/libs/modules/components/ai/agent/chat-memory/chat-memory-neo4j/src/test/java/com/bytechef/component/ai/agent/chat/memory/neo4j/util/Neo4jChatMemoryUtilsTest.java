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

package com.bytechef.component.ai.agent.chat.memory.neo4j.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;

/**
 * @author Ivica Cardic
 */
class Neo4jChatMemoryUtilsTest {

    @Test
    void testSameConnectionReusesOneDriver() {
        Driver firstDriver = Neo4jChatMemoryUtils.getSharedDriver(connectionParameters("secret-a"));
        Driver secondDriver = Neo4jChatMemoryUtils.getSharedDriver(connectionParameters("secret-a"));

        assertThat(secondDriver).isSameAs(firstDriver);
    }

    @Test
    void testDifferentCredentialsGetDifferentDrivers() {
        Driver firstDriver = Neo4jChatMemoryUtils.getSharedDriver(connectionParameters("secret-a"));
        Driver secondDriver = Neo4jChatMemoryUtils.getSharedDriver(connectionParameters("secret-b"));

        assertThat(secondDriver).isNotSameAs(firstDriver);
    }

    private static Parameters connectionParameters(String password) {
        return MockParametersFactory.create(
            Map.of("uri", "bolt://localhost:7687", "username", "neo4j", "password", password));
    }
}
