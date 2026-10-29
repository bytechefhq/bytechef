/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.config;

import com.bytechef.commons.data.jdbc.converter.EncryptedMapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.EncryptedStringToMapWrapperConverter;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.datastream.DataStreamComponentHandler;
import com.bytechef.component.datastream.batch.InMemoryBatchJobFactory;
import com.bytechef.component.json.file.JsonFileComponentHandler;
import com.bytechef.component.schedule.ScheduleComponentHandler;
import com.bytechef.component.webhook.WebhookComponentHandler;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.encryption.Encryption;
import com.bytechef.encryption.EncryptionImpl;
import com.bytechef.encryption.EncryptionKey;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.aspect.TokenRefreshHandler;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.context.ContextFactoryImpl;
import com.bytechef.platform.component.context.jackson.FileEntryDeserializer;
import com.bytechef.platform.component.log.EditorLogFileStorage;
import com.bytechef.platform.component.log.EditorLogFileStorageImpl;
import com.bytechef.platform.component.log.LogFileStorage;
import com.bytechef.platform.component.log.LogFileStorageImpl;
import com.bytechef.platform.component.log.TriggerLogFileStorage;
import com.bytechef.platform.component.log.TriggerLogFileStorageImpl;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionServiceImpl;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.connection.service.ConnectionServiceImpl;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.file.storage.EditorTempFileStorage;
import com.bytechef.platform.file.storage.EditorTempFileStorageImpl;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.file.storage.TempFileStorageImpl;
import com.bytechef.platform.scheduler.QuartzTriggerScheduler;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.tracing.Tracer;
import java.time.Duration;
import java.util.List;
import org.mockito.Mockito;
import org.quartz.Scheduler;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@EnableAutoConfiguration
@Import({
    ComponentWorkerFactory.class, FileEntryDeserializer.class, JacksonConfiguration.class, LiquibaseConfiguration.class,
    ComponentRemoteClientIntTestConfiguration.ComponentJdbcConfiguration.class, PostgreSQLContainerConfiguration.class
})
@SpringBootConfiguration
public class ComponentRemoteClientIntTestConfiguration {

    public static ComponentDefinitionRegistry componentDefinitionRegistry(
        ApplicationProperties applicationProperties, List<ComponentHandler> componentHandlers) {

        return new ComponentDefinitionRegistry(applicationProperties, componentHandlers, List::of, List.of());
    }

    @Bean
    ApplicationProperties applicationProperties() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Remote remote = applicationProperties.getRemote();

        ApplicationProperties.Remote.Grpc grpc = remote.getGrpc();

        grpc.setExecutionDeadline(Duration.ofSeconds(5));

        return applicationProperties;
    }

    @Bean
    CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }

    @Bean
    ComponentDefinitionRegistry componentDefinitionRegistry(
        ApplicationProperties applicationProperties, TriggerScheduler triggerScheduler) {

        return componentDefinitionRegistry(
            applicationProperties,
            List.of(
                new DataStreamComponentHandler(
                    Mockito.mock(Job.class), Mockito.mock(JobLauncher.class),
                    Mockito.mock(InMemoryBatchJobFactory.class)),
                new GrpcTestComponentHandler(1, "gRPC Test"), new JsonFileComponentHandler(),
                new ScheduleComponentHandler(triggerScheduler), new WebhookComponentHandler()));
    }

    @Bean
    ConnectionDefinitionService connectionDefinitionService(
        ComponentDefinitionRegistry componentDefinitionRegistry, ContextFactory contextFactory,
        @Lazy TokenRefreshHandler tokenRefreshHandler) {

        return new ConnectionDefinitionServiceImpl(componentDefinitionRegistry, contextFactory, tokenRefreshHandler);
    }

    @Bean
    ConnectionService connectionService(ConnectionRepository connectionRepository) {
        return new ConnectionServiceImpl(connectionRepository);
    }

    @Bean
    ContextFactory contextFactory(
        ApplicationContext applicationContext, ApplicationProperties applicationProperties, CacheManager cacheManager,
        EditorLogFileStorage editorLogFileStorage, EditorTempFileStorage editorTempFileStorage,
        ObjectProvider<ApprovalTokens> approvalTokensProvider, LogFileStorage logFileStorage,
        TempFileStorage tempFileStorage, TriggerLogFileStorage triggerLogFileStorage) {

        return new ContextFactoryImpl(
            applicationContext, applicationProperties, cacheManager, Mockito.mock(DataStorage.class),
            editorLogFileStorage, editorTempFileStorage, applicationContext, approvalTokensProvider, logFileStorage,
            tempFileStorage, Tracer.NOOP, triggerLogFileStorage);
    }

    @Bean
    EditorLogFileStorage editorLogFileStorage() {
        return new EditorLogFileStorageImpl(new Base64FileStorageService());
    }

    @Bean
    EditorTempFileStorage editorTempFileStorage() {
        return new EditorTempFileStorageImpl(new Base64FileStorageService());
    }

    @Bean
    Encryption encryption(EncryptionKey encryptionKey) {
        return new EncryptionImpl(encryptionKey);
    }

    @Bean
    EncryptionKey encryptionKey() {
        return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
    }

    @Bean
    LogFileStorage logFileStorage() {
        return new LogFileStorageImpl(new Base64FileStorageService());
    }

    @Bean
    TempFileStorage tempFileStorage() {
        return new TempFileStorageImpl(new Base64FileStorageService());
    }

    @Bean
    TokenRefreshHandler tokenRefreshHandler(
        CacheManager cacheManager, @Lazy ConnectionDefinitionService connectionDefinitionService,
        ConnectionService connectionService) {

        return new TokenRefreshHandler(cacheManager, connectionDefinitionService, connectionService);
    }

    @Bean
    TriggerLogFileStorage triggerLogFileStorage() {
        return new TriggerLogFileStorageImpl(new Base64FileStorageService());
    }

    @Bean
    TriggerScheduler triggerScheduler(ApplicationProperties applicationProperties, Scheduler scheduler) {
        ApplicationProperties.Coordinator coordinator = applicationProperties.getCoordinator();

        ApplicationProperties.Coordinator.Trigger trigger = coordinator.getTrigger();

        return new QuartzTriggerScheduler(trigger.getPolling(), scheduler);
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    static class ComponentJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final Encryption encryption;
        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        ComponentJdbcConfiguration(Encryption encryption, ObjectMapper objectMapper) {
            this.encryption = encryption;
            this.objectMapper = objectMapper;
        }

        @Override
        protected List<?> userConverters() {
            return List.of(
                new EncryptedMapWrapperToStringConverter(encryption, objectMapper),
                new EncryptedStringToMapWrapperConverter(encryption, objectMapper));
        }
    }
}
