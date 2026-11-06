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

package com.bytechef.platform.scheduler.db.config;

import static com.bytechef.platform.scheduler.db.task.DbSchedulerTaskDescriptors.QUARTZ_IMPORT;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.scheduler.ConnectionRefreshScheduler;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.platform.scheduler.db.DbConnectionRefreshScheduler;
import com.bytechef.platform.scheduler.db.DbTriggerScheduler;
import com.bytechef.platform.scheduler.db.importer.ImportSummary;
import com.bytechef.platform.scheduler.db.importer.QuartzImporter;
import com.bytechef.platform.scheduler.db.importer.QuartzJobReader;
import com.bytechef.platform.scheduler.db.task.DynamicWebhookRefreshData;
import com.bytechef.platform.scheduler.db.task.DynamicWebhookRefreshTaskFactory;
import com.bytechef.platform.scheduler.db.task.DynamicWebhookRefresher;
import com.bytechef.platform.scheduler.db.task.OAuth2TokenRefreshData;
import com.bytechef.platform.scheduler.db.task.OAuth2TokenRefreshTaskFactory;
import com.bytechef.platform.scheduler.db.task.OneTimeResumeData;
import com.bytechef.platform.scheduler.db.task.OneTimeResumeTaskFactory;
import com.bytechef.platform.scheduler.db.task.PollingTriggerData;
import com.bytechef.platform.scheduler.db.task.PollingTriggerTaskFactory;
import com.bytechef.platform.scheduler.db.task.ScheduleTriggerData;
import com.bytechef.platform.scheduler.db.task.ScheduleTriggerTaskFactory;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.service.TriggerStateService;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.boot.autoconfigure.Jackson3Serializer;
import com.github.kagkarlsson.scheduler.boot.config.DbSchedulerCustomizer;
import com.github.kagkarlsson.scheduler.serializer.Serializer;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import java.time.Duration;
import java.util.Optional;
import org.quartz.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef", name = "scheduler.provider", havingValue = "db-scheduler")
public class DbSchedulerConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DbSchedulerConfiguration.class);

    @Bean
    DbSchedulerCustomizer dbSchedulerCustomizer(ObjectProvider<JsonMapper> jsonMapperProvider) {
        JsonMapper jsonMapper = jsonMapperProvider.getIfAvailable(() -> JsonMapper.builder()
            .build());

        return new DbSchedulerCustomizer() {

            @Override
            public Optional<Serializer> serializer() {
                return Optional.of(new Jackson3Serializer(jsonMapper));
            }
        };
    }

    @Bean
    ConnectionRefreshScheduler dbConnectionRefreshScheduler(@Lazy SchedulerClient schedulerClient) {
        return new DbConnectionRefreshScheduler(schedulerClient);
    }

    @Bean
    TriggerScheduler dbTriggerScheduler(
        ApplicationProperties applicationProperties, @Lazy SchedulerClient schedulerClient) {

        ApplicationProperties.Coordinator.Trigger.Polling polling = applicationProperties.getCoordinator()
            .getTrigger()
            .getPolling();

        return new DbTriggerScheduler(schedulerClient, polling.getCheckPeriod());
    }

    @Bean
    DynamicWebhookRefresher dynamicWebhookRefresher(
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry, TriggerDefinitionFacade triggerDefinitionFacade,
        TriggerStateService triggerStateService, WorkflowService workflowService) {

        return new DynamicWebhookRefresher(
            jobPrincipalAccessorRegistry, triggerDefinitionFacade, triggerStateService, workflowService);
    }

    @Bean
    Task<DynamicWebhookRefreshData> dynamicWebhookRefreshTask(DynamicWebhookRefresher dynamicWebhookRefresher) {
        return DynamicWebhookRefreshTaskFactory.create(dynamicWebhookRefresher);
    }

    @Bean
    Task<OAuth2TokenRefreshData> oauth2TokenRefreshTask(ConnectionFacade connectionFacade) {
        return OAuth2TokenRefreshTaskFactory.create(connectionFacade);
    }

    @Bean
    Task<OneTimeResumeData> oneTimeResumeTask(ApplicationEventPublisher eventPublisher) {
        return OneTimeResumeTaskFactory.create(eventPublisher);
    }

    @Bean
    Task<PollingTriggerData> pollingTriggerTask(ApplicationEventPublisher eventPublisher) {
        return PollingTriggerTaskFactory.create(eventPublisher);
    }

    @Bean
    Task<ScheduleTriggerData> scheduleTriggerTask(ApplicationEventPublisher eventPublisher) {
        return ScheduleTriggerTaskFactory.create(eventPublisher);
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "bytechef", name = "scheduler.db-scheduler.importer.enabled", havingValue = "true",
        matchIfMissing = true)
    Task<Void> quartzImportTask(
        ApplicationProperties applicationProperties, ObjectProvider<Scheduler> quartzSchedulerProvider,
        @Lazy SchedulerClient schedulerClient) {

        ApplicationProperties.Coordinator.Trigger.Polling polling = applicationProperties.getCoordinator()
            .getTrigger()
            .getPolling();

        return Tasks.recurring(QUARTZ_IMPORT, FixedDelay.of(Duration.ofDays(3650)))
            .onFailure(new FailureHandler.OnFailureRetryLater<>(Duration.ofMinutes(5)))
            .execute((taskInstance, executionContext) -> {
                Scheduler quartzScheduler = quartzSchedulerProvider.getIfUnique();

                if (quartzScheduler == null) {
                    log.info("No Quartz scheduler bean present, nothing to import");

                    return;
                }

                QuartzImporter quartzImporter = new QuartzImporter(
                    new QuartzJobReader(quartzScheduler), schedulerClient, polling.getCheckPeriod());

                ImportSummary summary = quartzImporter.importJobs();

                log.info(
                    "Quartz import: scanned={}, imported={}, alreadyPresent={}, skippedStatic={}, "
                        + "skippedUnknown={}, skippedComplete={}, failed={}, quartzReadable={}",
                    summary.scanned(), summary.imported(), summary.alreadyPresent(), summary.skippedStatic(),
                    summary.skippedUnknown(), summary.skippedComplete(), summary.failed(),
                    summary.quartzReadable());
            });
    }
}
