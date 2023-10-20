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

package com.integri.atlas.task.handler.postgresql.v1_0;

import static com.integri.atlas.task.commons.jdbc.JdbcTaskConstants.HOST;
import static com.integri.atlas.task.commons.jdbc.JdbcTaskConstants.PASSWORD;
import static com.integri.atlas.task.commons.jdbc.JdbcTaskConstants.PORT;
import static com.integri.atlas.task.commons.jdbc.JdbcTaskConstants.USERNAME;
import static com.integri.atlas.task.descriptor.model.DSL.INTEGER_PROPERTY;
import static com.integri.atlas.task.descriptor.model.DSL.STRING_PROPERTY;
import static com.integri.atlas.task.descriptor.model.DSL.createTaskAuthDescriptor;
import static com.integri.atlas.task.descriptor.model.DSL.option;
import static com.integri.atlas.task.handler.postgresql.PostgreSQLTaskConstants.POSTGRESQL;
import static com.integri.atlas.task.handler.postgresql.PostgreSQLTaskConstants.VERSION_1_0;

import com.integri.atlas.task.commons.jdbc.JdbcTaskConstants;
import com.integri.atlas.task.descriptor.handler.AbstractTaskDescriptorHandler;
import com.integri.atlas.task.descriptor.model.DSL;
import com.integri.atlas.task.descriptor.model.TaskAuthDescriptor;
import com.integri.atlas.task.descriptor.model.TaskDescriptor;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class PostgreSQLTaskDescriptorHandler extends AbstractTaskDescriptorHandler {

    private static final List<TaskAuthDescriptor> TASK_AUTH_DESCRIPTORS = List.of(
        createTaskAuthDescriptor(POSTGRESQL)
            .displayName("PostgreSQL")
            .properties(
                STRING_PROPERTY(HOST).displayName("Host").required(true),
                INTEGER_PROPERTY(PORT).displayName("Port").required(true),
                STRING_PROPERTY(USERNAME).displayName("Username").required(true),
                STRING_PROPERTY(PASSWORD).displayName("Password").required(true)
            )
    );

    private static final TaskDescriptor TASK_DESCRIPTOR = DSL
        .createTaskDescriptor(POSTGRESQL)
        .displayName("PostgreSQL")
        .description("Query, insert nd update data from PostgreSQL.")
        .version(VERSION_1_0)
        .auth(option(POSTGRESQL))
        .operations(JdbcTaskConstants.TASK_OPERATIONS);

    @Override
    public List<TaskAuthDescriptor> getTaskAuthDescriptors() {
        return TASK_AUTH_DESCRIPTORS;
    }

    @Override
    public TaskDescriptor getTaskDescriptor() {
        return TASK_DESCRIPTOR;
    }
}
