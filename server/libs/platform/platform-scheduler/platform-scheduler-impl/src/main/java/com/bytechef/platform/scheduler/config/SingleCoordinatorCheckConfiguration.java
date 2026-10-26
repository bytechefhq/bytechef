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

package com.bytechef.platform.scheduler.config;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.annotation.ConditionalOnCEVersion;
import com.bytechef.platform.scheduler.SingleCoordinatorCheck;
import javax.sql.DataSource;
import org.quartz.Scheduler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnCEVersion
@ConditionalOnCoordinator
@ConditionalOnProperty(
    prefix = "bytechef", name = "scheduler.provider", havingValue = "quartz", matchIfMissing = true)
public class SingleCoordinatorCheckConfiguration {

    @Bean
    SingleCoordinatorCheck singleCoordinatorCheck(DataSource dataSource, @Lazy Scheduler scheduler) {
        return new SingleCoordinatorCheck(new JdbcTemplate(dataSource), scheduler);
    }
}
