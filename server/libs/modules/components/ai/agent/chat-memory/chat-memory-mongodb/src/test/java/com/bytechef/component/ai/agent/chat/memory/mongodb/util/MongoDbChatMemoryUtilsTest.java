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

package com.bytechef.component.ai.agent.chat.memory.mongodb.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.mongodb.client.MongoClient;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class MongoDbChatMemoryUtilsTest {

    @Test
    void testSameConnectionReusesOneMongoClient() {
        MongoClient firstMongoClient = MongoDbChatMemoryUtils.getSharedMongoClient(connectionParameters("secret-a"));
        MongoClient secondMongoClient = MongoDbChatMemoryUtils.getSharedMongoClient(connectionParameters("secret-a"));

        assertThat(secondMongoClient).isSameAs(firstMongoClient);
    }

    @Test
    void testDifferentCredentialsGetDifferentMongoClients() {
        MongoClient firstMongoClient = MongoDbChatMemoryUtils.getSharedMongoClient(connectionParameters("secret-a"));
        MongoClient secondMongoClient = MongoDbChatMemoryUtils.getSharedMongoClient(connectionParameters("secret-b"));

        assertThat(secondMongoClient).isNotSameAs(firstMongoClient);
    }

    private static Parameters connectionParameters(String password) {
        return MockParametersFactory.create(
            Map.of(
                "connectionString", "mongodb://localhost:27017", "databaseName", "spring_ai", "username", "user",
                "password", password));
    }
}
