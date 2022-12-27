
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

package com.bytechef.test.config.testcontainers;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

/**
 * @author Ivica Cardic
 */
public class RedisTestContainer implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(RedisTestContainer.class);

    private GenericContainer<?> redisContainer;

    @Override
    public void destroy() {
        if (null != redisContainer && redisContainer.isRunning()) {
            redisContainer.stop();
        }
    }

    @Override
    public void afterPropertiesSet() {
        if (null == redisContainer) {
            redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
                .withLogConsumer(new Slf4jLogConsumer(log))
                .withPrivilegedMode(true)
                .withReuse(true);
        }

        if (!redisContainer.isRunning()) {
            redisContainer.start();
        }
    }

    @SuppressFBWarnings("EI")
    public GenericContainer<?> getTestContainer() {
        return redisContainer;
    }
}
