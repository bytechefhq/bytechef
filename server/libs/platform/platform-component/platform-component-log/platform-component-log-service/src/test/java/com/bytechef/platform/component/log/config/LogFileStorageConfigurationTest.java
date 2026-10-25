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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.FileStorage.Provider;
import com.bytechef.file.storage.FileStorageServiceRegistry;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class LogFileStorageConfigurationTest {

    private final FileStorageServiceRegistry fileStorageServiceRegistry = mock(FileStorageServiceRegistry.class);

    @Test
    void testLogFileStorageUsesFilesystemWhenFileStorageProviderIsJdbc() {
        ApplicationProperties applicationProperties = createApplicationProperties(Provider.JDBC);

        LogFileStorageConfiguration logFileStorageConfiguration = new LogFileStorageConfiguration(
            applicationProperties);

        logFileStorageConfiguration.logFileStorage(applicationProperties, fileStorageServiceRegistry);
        logFileStorageConfiguration.editorLogFileStorage(applicationProperties, fileStorageServiceRegistry);
        logFileStorageConfiguration.triggerLogFileStorage(applicationProperties, fileStorageServiceRegistry);

        verify(fileStorageServiceRegistry, times(3)).getFileStorageService(Provider.FILESYSTEM.name());
        verify(fileStorageServiceRegistry, never()).getFileStorageService(Provider.JDBC.name());
    }

    @Test
    void testLogFileStorageUsesConfiguredProvider() {
        ApplicationProperties applicationProperties = createApplicationProperties(Provider.AWS);

        new LogFileStorageConfiguration(applicationProperties)
            .logFileStorage(applicationProperties, fileStorageServiceRegistry);

        verify(fileStorageServiceRegistry).getFileStorageService(Provider.AWS.name());
    }

    private static ApplicationProperties createApplicationProperties(Provider provider) {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.FileStorage fileStorage = applicationProperties.getFileStorage();

        fileStorage.setProvider(provider);

        return applicationProperties;
    }
}
