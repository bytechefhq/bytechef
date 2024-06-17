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

package com.bytechef.automation.configuration.config;

import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.component.registry.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.facade.WorkflowConnectionFacade;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeParameterFacade;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.facade.InstanceJobFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.InstanceJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

/**
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = {
        "com.bytechef.atlas.configuration.repository.jdbc", "com.bytechef.platform.category",
        "com.bytechef.automation.configuration", "com.bytechef.platform.connection", "com.bytechef.platform.tag"
    })
@EnableAutoConfiguration
@EnableCaching
@Import(LiquibaseConfiguration.class)
@Configuration
@SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
public class ProjectIntTestConfiguration {

    @MockBean
    private AuthorityService authorityService;

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
    private TriggerDefinitionService triggerDefinitionService;

    @MockBean
    private TriggerExecutionService triggerExecutionService;

    @MockBean
    private TriggerLifecycleFacade triggerLifecycleFacade;

    @MockBean
    private UserService userService;

    @MockBean
    private WorkflowConnectionFacade workflowConnectionFacade;

    @MockBean
    private WorkflowNodeParameterFacade workflowNodeParameterFacade;

    @MockBean
    private WorkflowNodeTestOutputService workflowNodeTestOutputService;

    @MockBean
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    @MockBean
    private WorkflowFacade workflowFacade;

    @Bean
    MapUtils mapUtils() {
        return new MapUtils() {
            {
                objectMapper = objectMapper();
            }
        };
    }

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module());
    }

    @EnableJdbcRepositories(
        basePackages = {
            "com.bytechef.atlas.configuration.repository.jdbc", "com.bytechef.platform.category.repository",
            "com.bytechef.automation.configuration.repository", "com.bytechef.platform.tag.repository"
        })
    public static class ProjectIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
    }
}
