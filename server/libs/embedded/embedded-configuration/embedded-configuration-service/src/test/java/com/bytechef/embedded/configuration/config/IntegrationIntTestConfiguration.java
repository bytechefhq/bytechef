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

package com.bytechef.embedded.configuration.config;

import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.encryption.EncryptionKey;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.facade.WorkflowConnectionFacade;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeParameterFacade;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.workflow.execution.facade.InstanceJobFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.InstanceJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = {
        "com.bytechef.atlas.configuration.repository.jdbc", "com.bytechef.encryption", "com.bytechef.platform.category",
        "com.bytechef.embedded.configuration", "com.bytechef.platform.tag"
    })
@EnableAutoConfiguration
@EnableCaching
@EnableConfigurationProperties(ApplicationProperties.class)
@Import(LiquibaseConfiguration.class)
@Configuration
public class IntegrationIntTestConfiguration {

    @MockitoBean
    private ComponentDefinitionService componentDefinitionService;

    @MockitoBean
    private ConnectionDefinitionService connectionDefinitionService;

    @MockitoBean
    private ConnectionService connectionService;

    @MockitoBean
    private ConnectedUserService connectedUserService;

    @MockitoBean
    private InstanceJobFacade instanceJobFacade;

    @MockitoBean
    private InstanceJobService instanceJobService;

    @MockitoBean
    private JobFacade jobFacade;

    @MockitoBean
    private JobService jobService;

    @MockitoBean
    private OAuth2Service oAuth2Service;

    @MockitoBean
    private TriggerDefinitionService triggerDefinitionService;

    @MockitoBean
    private TriggerExecutionService triggerExecutionService;

    @MockitoBean
    private TriggerLifecycleFacade triggerLifecycleFacade;

    @MockitoBean
    private WorkflowConnectionFacade workflowConnectionFacade;

    @MockitoBean
    private WorkflowFacade workflowFacade;

    @MockitoBean
    private WorkflowNodeParameterFacade workflowNodeParameterFacade;

    @MockitoBean
    private WorkflowNodeTestOutputService workflowNodeTestOutputService;

    @MockitoBean
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    @Bean
    EncryptionKey encryptionKey() {
        return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
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
            "com.bytechef.embedded.configuration.repository", "com.bytechef.platform.tag.repository"
        })
    public static class IntegrationIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
    }
}
