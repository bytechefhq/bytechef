/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.helios.configuration.config;

import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.hermes.configuration.facade.WorkflowConnectionFacade;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.execution.facade.InstanceJobFacade;
import com.bytechef.hermes.execution.facade.TriggerLifecycleFacade;
import com.bytechef.hermes.execution.service.InstanceJobService;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

/**
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = {
        "com.bytechef.atlas.configuration.repository.jdbc", "com.bytechef.category",
        "com.bytechef.helios.configuration", "com.bytechef.hermes.connection", "com.bytechef.liquibase.config",
        "com.bytechef.tag"
    })
@EnableAutoConfiguration
@Configuration
public class ProjectIntTestConfiguration {

    @MockBean
    private ConnectionService connectionService;

    @MockBean
    private InstanceJobFacade instanceJobFacade;

    @MockBean
    private InstanceJobService instanceJobService;

    @MockBean
    private JobFacade jobFacade;

    @MockBean
    private JobService jobService;

    @MockBean
    private TriggerLifecycleFacade triggerLifecycleFacade;

    @MockBean
    private WorkflowConnectionFacade workflowConnectionFacade;

    @EnableCaching
    @TestConfiguration
    public static class CacheConfiguration {
    }

    @EnableJdbcRepositories(
        basePackages = {
            "com.bytechef.atlas.configuration.repository.jdbc", "com.bytechef.category.repository",
            "com.bytechef.helios.configuration.repository", "com.bytechef.tag.repository"
        })
    public static class ProjectIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
    }
}
