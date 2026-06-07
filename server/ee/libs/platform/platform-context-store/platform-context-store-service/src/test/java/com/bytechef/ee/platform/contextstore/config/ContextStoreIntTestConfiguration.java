/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.config;

import com.bytechef.commons.data.jdbc.converter.MapWrapperToPGObjectConverter;
import com.bytechef.commons.data.jdbc.converter.PGobjectToMapWrapperConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration-test configuration for the Context Store service module. Scans only the context-store packages so the
 * test classpath stays narrow; other module configurations should not leak into this context.
 *
 * @author Ivica Cardic
 * @version ee
 */
@ComponentScan(
    basePackages = "com.bytechef.ee.platform.contextstore",
    excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ContextStorePgVectorConfiguration.class))
@EnableAutoConfiguration(excludeName = {
    "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration",
    "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration",
    "org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration",
    "org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration",
    "org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration",
    "org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration",
    "org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration"
})
@Import({
    JacksonConfiguration.class, LiquibaseConfiguration.class
})
@Configuration
public class ContextStoreIntTestConfiguration {

    // After moving the workspace-aware facade and tool-facade to the automation-side module, the platform-CS slice is
    // pure data-plane: entities, repositories, the platform CRUD service, the query service, the sync writer, and the
    // sync listener. None of those need atlas/automation-config beans, so this slice configuration no longer mocks
    // them.

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class ContextStoreIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        public ContextStoreIntTestJdbcConfiguration(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        // Mirror the production JdbcConfiguration wiring for MapWrapper columns. Without these converters Spring
        // Data JDBC treats MapWrapper as a @MappedCollection and emits an outer join on a non-existent table,
        // breaking every query that touches ContextStoreEntity (indexed_fields, stored_fields, ...) and
        // ContextStoreRecord (payload).
        //
        // Write path: MapWrapperToPGObjectConverter binds JSON as a PGobject('jsonb'), which Postgres accepts for
        // both JSONB columns (the entity/record columns here) and TEXT columns (via the implicit jsonb→text
        // assignment cast).
        // Read path: JSONB columns return PGobject (handled by PGobjectToMapWrapperConverter) and TEXT columns
        // return String (handled by StringToMapWrapperConverter).
        @Override
        protected @NonNull List<?> userConverters() {
            return Arrays.asList(
                new MapWrapperToPGObjectConverter(objectMapper),
                new PGobjectToMapWrapperConverter(objectMapper),
                new StringToMapWrapperConverter(objectMapper));
        }
    }
}
