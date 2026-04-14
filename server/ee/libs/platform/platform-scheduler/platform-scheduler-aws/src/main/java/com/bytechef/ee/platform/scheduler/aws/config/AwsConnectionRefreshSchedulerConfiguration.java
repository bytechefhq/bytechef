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

package com.bytechef.ee.platform.scheduler.aws.config;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.scheduler.aws.AwsConnectionRefreshScheduler;
import com.bytechef.ee.platform.scheduler.aws.listener.ConnectionRefreshListener;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.facade.ConnectionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.service.TriggerStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.scheduler.SchedulerClient;

/**
 * @author Nikolina Spehar
 */
@Configuration
@ConditionalOnProperty(
    prefix = "bytechef", name = "coordinator.connection.scheduler.provider", havingValue = "aws")
@ConditionalOnEEVersion
public class AwsConnectionRefreshSchedulerConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AwsConnectionRefreshSchedulerConfiguration.class);

    private final ApplicationProperties applicationProperties;

    AwsConnectionRefreshSchedulerConfiguration(ApplicationProperties applicationProperties) {
        if (log.isInfoEnabled()) {
            log.info("Connection refresh scheduler provider type enabled: aws");
        }

        this.applicationProperties = applicationProperties;
    }

    @Bean
    AwsConnectionRefreshScheduler awsConnectionRefreshScheduler(
        AwsCredentialsProvider awsCredentialsProvider, AwsRegionProvider awsRegionProvider) {

        ApplicationProperties.Cloud.Aws aws = applicationProperties.getCloud()
            .getAws();
        ApplicationProperties.Coordinator.Trigger.Polling polling = applicationProperties
            .getCoordinator()
            .getTrigger()
            .getPolling();

        SchedulerClient schedulerClient = SchedulerClient.builder()
            .credentialsProvider(awsCredentialsProvider)
            .region(awsRegionProvider.getRegion())
            .build();

        return new AwsConnectionRefreshScheduler(aws, polling, schedulerClient);
    }

    @Bean
    ConnectionRefreshListener connectionRefreshListener(
        AwsCredentialsProvider awsCredentialsProvider, AwsRegionProvider awsRegionProvider,
        ConnectionDefinitionFacade remoteConnectionDefinitionFacade) {

        SchedulerClient schedulerClient = SchedulerClient.builder()
            .credentialsProvider(awsCredentialsProvider)
            .region(awsRegionProvider.getRegion())
            .build();

        return new ConnectionRefreshListener(schedulerClient, remoteConnectionDefinitionFacade);
    }
}
