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

package com.bytechef.automation.workspacefile.config;

import org.apache.tika.Tika;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

/**
 * @author Ivica Cardic
 */
@AutoConfiguration
@EnableJdbcRepositories(basePackages = "com.bytechef.automation.workspacefile.repository")
@EnableConfigurationProperties({
    AutomationWorkspaceFileQuotaProperties.class, AutomationWorkspaceFileOrphanCleanupProperties.class
})
@ConditionalOnBean(AbstractJdbcConfiguration.class)
public class AutomationWorkspaceFileJdbcRepositoryConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Tika workspaceFileTika() {
        return new Tika();
    }
}
