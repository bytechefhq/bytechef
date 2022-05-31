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

package com.integri.atlas.task.handler.mysql;

import static com.integri.atlas.task.commons.jdbc.JdbcTaskConstants.PROPERTY_HOST;
import static com.integri.atlas.task.commons.jdbc.JdbcTaskConstants.PROPERTY_PASSWORD;
import static com.integri.atlas.task.commons.jdbc.JdbcTaskConstants.PROPERTY_PORT;
import static com.integri.atlas.task.commons.jdbc.JdbcTaskConstants.PROPERTY_USERNAME;
import static com.integri.atlas.task.definition.model.DSL.INTEGER_PROPERTY;
import static com.integri.atlas.task.definition.model.DSL.STRING_PROPERTY;
import static com.integri.atlas.task.definition.model.DSL.option;
import static com.integri.atlas.task.handler.mysql.MySQLTaskConstants.TASK_MYSQL;

import com.integri.atlas.task.commons.jdbc.JdbcTaskConstants;
import com.integri.atlas.task.definition.AbstractTaskDefinitionHandler;
import com.integri.atlas.task.definition.model.DSL;
import com.integri.atlas.task.definition.model.TaskAuthDefinition;
import com.integri.atlas.task.definition.model.TaskDefinition;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class MySQLTaskDefinitionHandler extends AbstractTaskDefinitionHandler {

    private static final List<TaskAuthDefinition> TASK_AUTH_DEFINITIONS = List.of(
        DSL
            .createTaskAuthDefinition(TASK_MYSQL)
            .displayName("MySQL")
            .properties(
                STRING_PROPERTY(PROPERTY_HOST).displayName("Host").required(true),
                INTEGER_PROPERTY(PROPERTY_PORT).displayName("Port").required(true),
                STRING_PROPERTY(PROPERTY_USERNAME).displayName("Username").required(true),
                STRING_PROPERTY(PROPERTY_PASSWORD).displayName("Password").required(true)
            )
    );

    private static final TaskDefinition TASK_DEFINITION = DSL
        .createTaskDefinition(TASK_MYSQL)
        .displayName("MySQL")
        .description("Query, insert nd update data from MySQL.")
        .auth(option(TASK_MYSQL))
        .operations(JdbcTaskConstants.TASK_OPERATIONS);

    @Override
    public List<TaskAuthDefinition> getTaskAuthDefinitions() {
        return TASK_AUTH_DEFINITIONS;
    }

    @Override
    public TaskDefinition getTaskDefinition() {
        return TASK_DEFINITION;
    }
}
