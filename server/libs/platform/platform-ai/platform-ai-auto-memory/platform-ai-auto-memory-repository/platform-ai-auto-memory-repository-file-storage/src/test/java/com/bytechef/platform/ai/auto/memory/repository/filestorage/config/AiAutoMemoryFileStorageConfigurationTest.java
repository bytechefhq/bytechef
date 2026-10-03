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

package com.bytechef.platform.ai.auto.memory.repository.filestorage.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.file.storage.FileStorageServiceRegistry;
import com.bytechef.file.storage.filesystem.service.FilesystemFileStorageService;
import com.bytechef.platform.ai.auto.memory.repository.filestorage.FileStorageAiAutoMemoryRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
class AiAutoMemoryFileStorageConfigurationTest {

    private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(AiAutoMemoryFileStorageConfiguration.class))
        .withUserConfiguration(TestConfiguration.class);

    @Test
    void testNoFileStorageRepositoryByDefault() {
        applicationContextRunner.run(
            context -> assertThat(context).doesNotHaveBean(FileStorageAiAutoMemoryRepository.class));
    }

    @Test
    void testFilesystemProviderCreatesRepository() {
        applicationContextRunner
            .withPropertyValues("bytechef.ai.auto-memory.provider=FILESYSTEM")
            .run(context -> assertThat(context).hasSingleBean(FileStorageAiAutoMemoryRepository.class));
    }

    @Test
    void testAwsProviderFailsFastWhenServiceMissing() {
        applicationContextRunner
            .withPropertyValues("bytechef.ai.auto-memory.provider=AWS")
            .run(context -> assertThat(context).hasFailed()
                .getFailure()
                .rootCause()
                .hasMessageContaining("no FileStorageService of type AWS"));
    }

    @Configuration
    @EnableConfigurationProperties(ApplicationProperties.class)
    static class TestConfiguration {

        @Bean
        FileStorageServiceRegistry fileStorageServiceRegistry() {
            return new FileStorageServiceRegistry(
                List.of(new FilesystemFileStorageService(System.getProperty("java.io.tmpdir"))));
        }
    }
}
