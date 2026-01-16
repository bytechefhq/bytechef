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

package com.bytechef.ai.copilot.config;

import com.bytechef.ee.ai.copilot.repository.converter.ListDoubleToPGObjectConverter;
import com.bytechef.ee.ai.copilot.repository.converter.MapToPGObjectConverter;
import com.bytechef.ee.ai.copilot.repository.converter.PGObjectToListDoubleConverter;
import com.bytechef.ee.ai.copilot.repository.converter.PGobjectToMapConverter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 */
@Configuration
public class JdbcConfiguration extends AbstractJdbcConfiguration {

    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI2")
    public JdbcConfiguration(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected List<?> userConverters() {
        // TODO Use JsonUtils directly
        return Arrays.asList(
            new MapToPGObjectConverter(objectMapper),
            new PGobjectToMapConverter(objectMapper),
            new ListDoubleToPGObjectConverter(),
            new PGObjectToListDoubleConverter());
    }
}
