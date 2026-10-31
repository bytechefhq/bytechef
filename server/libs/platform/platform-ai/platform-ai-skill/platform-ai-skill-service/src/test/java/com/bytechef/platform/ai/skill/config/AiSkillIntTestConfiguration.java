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

package com.bytechef.platform.ai.skill.config;

import com.bytechef.commons.data.jdbc.converter.FileEntryToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToFileEntryConverter;
import com.bytechef.file.storage.filesystem.service.FilesystemFileStorageService;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.ai.skill.file.storage.AiSkillFileStorage;
import com.bytechef.platform.ai.skill.file.storage.AiSkillFileStorageImpl;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = {
        "com.bytechef.platform.ai.skill.facade", "com.bytechef.platform.ai.skill.service",
        "com.bytechef.platform.tag.service"
    },
    excludeFilters = @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class))
@EnableAutoConfiguration
@Import(LiquibaseConfiguration.class)
@Configuration
public class AiSkillIntTestConfiguration {

    @Bean
    Path aiSkillFileStorageBaseDirectory() {
        try {
            return Files.createTempDirectory("ai-skill-int-test");
        } catch (IOException ioException) {
            throw new UncheckedIOException(ioException);
        }
    }

    @Bean
    AiSkillFileStorage aiSkillFileStorage(Path aiSkillFileStorageBaseDirectory) {
        return new AiSkillFileStorageImpl(new FilesystemFileStorageService(aiSkillFileStorageBaseDirectory.toString()));
    }

    @Bean
    AuditorAware<String> aiSkillAuditorProvider() {
        return () -> Optional.of(
            SecurityUtils.fetchCurrentUserLogin()
                .orElse("system"));
    }

    @EnableJdbcAuditing(auditorAwareRef = "aiSkillAuditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class AiSkillIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .build();

        @Override
        protected List<?> userConverters() {
            return List.of(
                new FileEntryToStringConverter(OBJECT_MAPPER), new StringToFileEntryConverter(OBJECT_MAPPER));
        }
    }
}
