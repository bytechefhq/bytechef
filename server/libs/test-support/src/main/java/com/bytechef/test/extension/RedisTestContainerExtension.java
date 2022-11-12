/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.test.extension;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class RedisTestContainerExtension implements BeforeAllCallback {

    private static AtomicBoolean started = new AtomicBoolean(false);

    private static GenericContainer redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        if (!started.get()) {
            redis.start();

            System.setProperty("spring.redis.host", redis.getHost());
            System.setProperty("spring.redis.port", redis.getMappedPort(6379).toString());

            started.set(true);
        }
    }
}
