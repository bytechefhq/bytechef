
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

package com.bytechef.hermes.component.jdbc.operation.config;

import com.bytechef.event.EventPublisher;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan({
    "com.bytechef.liquibase.config", "com.bytechef.hermes.component.jdbc"
})
@EnableAutoConfiguration
@SpringBootConfiguration
public class JdbcOperationIntTestConfiguration {

    @MockBean
    private ConnectionService connectionService;

    @MockBean
    private EventPublisher eventPublisher;

    @MockBean
    private FileStorageService fileStorageService;
}
