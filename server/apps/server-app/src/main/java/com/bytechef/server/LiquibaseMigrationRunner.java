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

package com.bytechef.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Shuts down the application after Liquibase migrations complete. Active only with the {@code liquibase} profile so the
 * server starts normally in all other profiles.
 *
 * <p>
 * Since {@link liquibase.integration.spring.SpringLiquibase} runs during context initialization, by the time this
 * runner executes all migrations have already been applied.
 *
 * @author Ivica Cardic
 */
@Component
@Profile("liquibase")
class LiquibaseMigrationRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(LiquibaseMigrationRunner.class);

    private final ApplicationContext applicationContext;

    LiquibaseMigrationRunner(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(String... args) {
        logger.info("Liquibase migration completed successfully");

        SpringApplication.exit(applicationContext, () -> 0);
    }

}
