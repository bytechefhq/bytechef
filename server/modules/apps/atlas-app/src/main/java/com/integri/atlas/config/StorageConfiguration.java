/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.integri.atlas.config;

import com.integri.atlas.engine.core.storage.StorageService;
import com.integri.atlas.engine.core.storage.base64.Base64StorageService;
import com.integri.atlas.engine.core.storage.file.FileStorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class StorageConfiguration {

    @Bean
    @ConditionalOnProperty(name = "atlas.storage.provider", havingValue = "base64")
    StorageService base64StorageService() {
        return new Base64StorageService();
    }

    @Bean
    @ConditionalOnProperty(name = "atlas.storage.provider", havingValue = "file")
    StorageService fileStorageService(StorageProperties storageProperties) {
        return new FileStorageService(storageProperties.getFileStorageDir());
    }
}
