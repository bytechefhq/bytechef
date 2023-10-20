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

package com.bytechef.task.handler.mysql.v1_0;

import static com.bytechef.hermes.descriptor.model.DSL.option;

import com.bytechef.hermes.descriptor.handler.TaskDescriptorHandler;
import com.bytechef.hermes.descriptor.model.DSL;
import com.bytechef.hermes.descriptor.model.TaskDescriptor;
import com.bytechef.task.handler.mysql.MySQLTaskConstants;
import com.bytechef.task.jdbc.JdbcTaskConstants;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class MySQLTaskDescriptorHandler implements TaskDescriptorHandler {

    private static final TaskDescriptor TASK_DESCRIPTOR = DSL.createTaskDescriptor(MySQLTaskConstants.MYSQL)
            .displayName("MySQL")
            .description("Query, insert nd update data from MySQL.")
            .version(MySQLTaskConstants.VERSION_1_0)
            .auth(option(MySQLTaskConstants.MYSQL))
            .operations(JdbcTaskConstants.TASK_OPERATIONS);

    @Override
    public TaskDescriptor getTaskDescriptor() {
        return TASK_DESCRIPTOR;
    }
}
