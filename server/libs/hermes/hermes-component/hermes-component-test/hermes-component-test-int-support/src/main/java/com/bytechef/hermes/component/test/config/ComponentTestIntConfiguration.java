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

package com.bytechef.hermes.component.test.config;

import com.bytechef.atlas.repository.config.WorkflowRepositoryConfiguration;
import com.bytechef.atlas.repository.resource.config.ResourceWorkflowRepositoryConfiguration;
import com.bytechef.atlas.sync.executor.config.WorkflowExecutorConfiguration;
import com.bytechef.encryption.Encryption;
import com.bytechef.encryption.EncryptionKey;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@ComponentScan("com.bytechef.hermes.component")
@EnableAutoConfiguration(
        exclude = {DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class})
@Import({
    ResourceWorkflowRepositoryConfiguration.class,
    WorkflowExecutorConfiguration.class,
    WorkflowRepositoryConfiguration.class
})
@SpringBootConfiguration
public class ComponentTestIntConfiguration {

    @MockBean
    private ConnectionService connectionService;

    @Bean
    FileStorageService base64FileStorageService() {
        return new Base64FileStorageService();
    }

    @EnableCaching
    @TestConfiguration
    public static class CacheConfiguration {}

    @TestConfiguration
    public static class EncryptionIntTestConfiguration {

        @Bean
        Encryption encryption(EncryptionKey encryptionKey) {
            return new Encryption(encryptionKey);
        }

        @Bean
        EncryptionKey encryptionKey() {
            return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
        }
    }
}
