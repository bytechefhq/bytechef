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

package com.bytechef.platform.component.log.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.FileStorage.Provider;
import com.bytechef.file.storage.FileStorageServiceRegistry;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.file.storage.filesystem.service.FilesystemFileStorageService;
import com.bytechef.platform.component.log.EditorLogFileStorage;
import com.bytechef.platform.component.log.JdbcLogFileStorage;
import com.bytechef.platform.component.log.LogFileStorage;
import com.bytechef.platform.component.log.LogFileStorageImpl;
import com.bytechef.platform.component.log.TriggerLogFileStorage;
import com.bytechef.platform.component.log.repository.ComponentLogEntryRepository;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * @author Ivica Cardic
 */
class LogFileStorageConfigurationTest {

    @TempDir
    private Path baseDirectory;

    @Test
    void testUsesTheLogTableWithTheJdbcProvider() {
        contextRunner(Provider.JDBC)
            .run(context -> {
                assertThat(context.getBean(LogFileStorage.class)).isInstanceOf(JdbcLogFileStorage.class);
                assertThat(context.getBean(EditorLogFileStorage.class)).isNotNull();
                assertThat(context.getBean(TriggerLogFileStorage.class)).isNotNull();
            });
    }

    @Test
    void testUsesFilesWithTheFilesystemProvider() {
        contextRunner(Provider.FILESYSTEM)
            .run(context -> assertThat(context.getBean(LogFileStorage.class))
                .isInstanceOf(LogFileStorageImpl.class));
    }

    @Test
    void testUsesTheJdbcFileStorageServiceWithTheJdbcProviderWithoutADatabase() {
        ApplicationProperties applicationProperties = createApplicationProperties(Provider.JDBC);

        new ApplicationContextRunner()
            .withUserConfiguration(LogFileStorageConfiguration.class)
            .withBean(ApplicationProperties.class, () -> applicationProperties)
            .withBean(
                FileStorageServiceRegistry.class,
                () -> new FileStorageServiceRegistry(List.of(new Base64FileStorageService())))
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBean(LogFileStorage.class)).isInstanceOf(LogFileStorageImpl.class);
                assertThat(context.getBean(EditorLogFileStorage.class)).isNotNull();
                assertThat(context.getBean(TriggerLogFileStorage.class)).isNotNull();
            });
    }

    private ApplicationContextRunner contextRunner(Provider provider) {
        ApplicationProperties applicationProperties = createApplicationProperties(provider);

        return new ApplicationContextRunner()
            .withUserConfiguration(LogFileStorageConfiguration.class)
            .withBean(ApplicationProperties.class, () -> applicationProperties)
            .withBean(
                FileStorageServiceRegistry.class,
                () -> new FileStorageServiceRegistry(
                    List.of(new FilesystemFileStorageService(baseDirectory.toString()))))
            .withBean(ComponentLogEntryRepository.class, () -> mock(ComponentLogEntryRepository.class));
    }

    private static ApplicationProperties createApplicationProperties(Provider provider) {
        ApplicationProperties applicationProperties = new ApplicationProperties();
        ApplicationProperties.FileStorage fileStorage = applicationProperties.getFileStorage();

        fileStorage.setProvider(provider);

        return applicationProperties;
    }
}
