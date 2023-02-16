
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

package com.bytechef.hermes.connection.config;

import com.bytechef.encryption.Encryption;
import com.bytechef.hermes.connection.converter.EncryptedMapWrapperToStringConverter;
import com.bytechef.hermes.connection.converter.EncryptedStringToMapWrapperConverter;
import com.bytechef.hermes.connection.facade.ConnectionFacade;
import com.bytechef.hermes.connection.facade.ConnectionFacadeImpl;
import com.bytechef.hermes.connection.repository.ConnectionRepository;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.connection.service.impl.ConnectionServiceImpl;
import com.bytechef.tag.service.TagService;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class ConnectionConfiguration extends AbstractJdbcConfiguration {

    private final Encryption encryption;
    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI2")
    public ConnectionConfiguration(Encryption encryption, ObjectMapper objectMapper) {
        this.encryption = encryption;
        this.objectMapper = objectMapper;
    }

    @Bean
    ConnectionFacade connectionFacade(ConnectionService connectionService, TagService tagService) {
        return new ConnectionFacadeImpl(connectionService, tagService);
    }

    @Bean
    ConnectionService connectionService(ConnectionRepository connectionRepository) {
        return new ConnectionServiceImpl(connectionRepository);
    }

    @Override
    protected List<?> userConverters() {
        return Arrays.asList(
            new EncryptedMapWrapperToStringConverter(encryption, objectMapper),
            new EncryptedStringToMapWrapperConverter(encryption, objectMapper));
    }
}
