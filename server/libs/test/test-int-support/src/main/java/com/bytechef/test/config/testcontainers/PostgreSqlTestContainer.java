
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
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

/**
 * @author Ivica Cardic
 */
public class PostgreSqlTestContainer implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(PostgreSqlTestContainer.class);
    private final long memoryInBytes = 100 * 1024 * 1024;
    private final long memorySwapInBytes = 200 * 1024 * 1024;

    private PostgreSQLContainer<?> postgreSQLContainer;

    @Override
    public void destroy() {
        if (null != postgreSQLContainer && postgreSQLContainer.isRunning()) {
            postgreSQLContainer.stop();
        }
    }

    @Override
    public void afterPropertiesSet() {
        if (null == postgreSQLContainer) {
            postgreSQLContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:14-alpine"))
                .withCreateContainerCmdModifier(
                    cmd -> cmd.getHostConfig()
                        .withMemory(memoryInBytes)
                        .withMemorySwap(memorySwapInBytes))
                .withDatabaseName("bytechef")
                .withLogConsumer(new Slf4jLogConsumer(log))
                .withPrivilegedMode(true)
                .withReuse(true)
                .withTmpFs(Collections.singletonMap("/testtmpfs", "rw"));
        }

        if (!postgreSQLContainer.isRunning()) {
            postgreSQLContainer.start();
        }
    }

    @SuppressFBWarnings("EI")
    public JdbcDatabaseContainer<?> getTestContainer() {
        return postgreSQLContainer;
    }
}
