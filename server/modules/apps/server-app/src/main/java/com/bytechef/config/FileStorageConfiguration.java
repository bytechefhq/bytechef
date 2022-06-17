/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.config;

import com.bytechef.atlas.MapObject;
import com.bytechef.hermes.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.hermes.file.storage.converter.FileEntryConverter;
import com.bytechef.hermes.file.storage.filesystem.FileSystemFileStorageService;
import com.bytechef.hermes.file.storage.job.FileStorageFileRetentionCheckJobListener;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import javax.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class FileStorageConfiguration {

    @PostConstruct
    void afterPropertiesSet() {
        MapObject.addConverter(new FileEntryConverter());
    }

    @Bean
    FileStorageFileRetentionCheckJobListener fileStorageFileRetentionCheckJobListener(
            FileStorageService fileStorageService) {
        return new FileStorageFileRetentionCheckJobListener(fileStorageService);
    }

    @Bean
    @ConditionalOnProperty(name = "atlas.storage.provider", havingValue = "base64")
    FileStorageService base64FileStorageService() {
        return new Base64FileStorageService();
    }

    @Bean
    @ConditionalOnProperty(name = "atlas.storage.provider", havingValue = "file")
    FileStorageService fileSystemFileStorageService(AtlasProperties atlasProperties) {
        StorageProperties storageProperties = atlasProperties.getStorage();

        return new FileSystemFileStorageService(storageProperties.getFileStorageDir());
    }
}
