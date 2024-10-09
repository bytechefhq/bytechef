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

package com.bytechef.ee.platform.scheduler.aws.config;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.scheduler.aws.AwsTriggerScheduler;
import com.bytechef.ee.platform.scheduler.aws.listeners.DynamicWebhookListener;
import com.bytechef.ee.platform.scheduler.aws.listeners.PollingListener;
import com.bytechef.ee.platform.scheduler.aws.listeners.ScheduleListener;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.configuration.instance.accessor.InstanceAccessorRegistry;
import com.bytechef.platform.workflow.execution.service.TriggerStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.scheduler.SchedulerClient;

@Configuration
@ConditionalOnProperty(prefix = "bytechef", name = "coordinator.trigger.scheduler.provider", havingValue = "aws")
public class AwsTriggerSchedulerConfiguration {
    private static final Logger log = LoggerFactory.getLogger(AwsTriggerSchedulerConfiguration.class);

    private final ApplicationProperties.Cloud.Aws aws;

    AwsTriggerSchedulerConfiguration(ApplicationProperties applicationProperties) {
        if (log.isInfoEnabled()) {
            log.info("Event Trigger Scheduler enabled");
        }

        this.aws = applicationProperties.getCloud()
            .getAws();
    }

    @Bean
    public AwsTriggerScheduler awsTriggerScheduler(
        AwsCredentialsProvider awsCredentialsProvider, AwsRegionProvider awsRegionProvider) {

        SchedulerClient client = SchedulerClient.builder()
            .credentialsProvider(awsCredentialsProvider)
            .region(awsRegionProvider.getRegion())
            .build();

        return new AwsTriggerScheduler(client, aws);
    }

    @Bean
    public DynamicWebhookListener dynamicWebhookListener(
        AwsCredentialsProvider awsCredentialsProvider, AwsRegionProvider awsRegionProvider,
        InstanceAccessorRegistry instanceAccessorRegistry, TriggerDefinitionFacade triggerDefinitionFacade,
        TriggerStateService triggerStateService, WorkflowService workflowService) {
        SchedulerClient client = SchedulerClient.builder()
            .credentialsProvider(awsCredentialsProvider)
            .region(awsRegionProvider.getRegion())
            .build();

        return new DynamicWebhookListener(client, instanceAccessorRegistry, triggerDefinitionFacade,
            triggerStateService, workflowService);
    }

    @Bean
    public ScheduleListener scheduleListener(ApplicationEventPublisher eventPublisher) {
        return new ScheduleListener(eventPublisher);
    }

    @Bean
    public PollingListener pollingListener(ApplicationEventPublisher eventPublisher) {
        return new PollingListener(eventPublisher);
    }
}
