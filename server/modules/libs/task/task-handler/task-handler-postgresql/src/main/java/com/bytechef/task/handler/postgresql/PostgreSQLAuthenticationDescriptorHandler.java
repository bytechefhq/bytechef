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

package com.bytechef.task.handler.postgresql;

import static com.bytechef.hermes.descriptor.domain.DSL.INTEGER_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.STRING_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.createAuthenticationDescriptor;
import static com.bytechef.task.jdbc.JdbcTaskConstants.HOST;
import static com.bytechef.task.jdbc.JdbcTaskConstants.PASSWORD;
import static com.bytechef.task.jdbc.JdbcTaskConstants.PORT;
import static com.bytechef.task.jdbc.JdbcTaskConstants.USERNAME;

import com.bytechef.hermes.descriptor.domain.AuthenticationDescriptor;
import com.bytechef.hermes.descriptor.domain.AuthenticationDescriptors;
import com.bytechef.hermes.descriptor.domain.DSL;
import com.bytechef.hermes.descriptor.handler.AuthenticationDescriptorHandler;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class PostgreSQLAuthenticationDescriptorHandler implements AuthenticationDescriptorHandler {

    private static final List<AuthenticationDescriptor> TASK_AUTH_DESCRIPTORS =
            List.of(createAuthenticationDescriptor(PostgreSQLTaskConstants.POSTGRESQL)
                    .displayName("PostgreSQL")
                    .properties(
                            STRING_PROPERTY(HOST).displayName("Host").required(true),
                            INTEGER_PROPERTY(PORT).displayName("Port").required(true),
                            STRING_PROPERTY(USERNAME).displayName("Username").required(true),
                            STRING_PROPERTY(PASSWORD).displayName("Password").required(true)));

    @Override
    public AuthenticationDescriptors getAuthenticationDescriptors() {
        return DSL.createAuthenticationDescriptors(PostgreSQLTaskConstants.POSTGRESQL, TASK_AUTH_DESCRIPTORS);
    }
}
