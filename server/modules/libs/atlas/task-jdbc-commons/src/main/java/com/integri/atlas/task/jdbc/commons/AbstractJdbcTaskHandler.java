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

package com.integri.atlas.task.jdbc.commons;

import com.integri.atlas.engine.task.execution.TaskExecution;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractJdbcTaskHandler {

    private final DataSourceFactory dataSourceFactory;

    protected final TransactionTemplate transactionTemplate;

    public AbstractJdbcTaskHandler(DataSourceFactory dataSourceFactory, PlatformTransactionManager transactionManager) {
        this.dataSourceFactory = dataSourceFactory;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    protected JdbcTemplate createJdbcTemplate(TaskExecution taskExecution) {
        return new JdbcTemplate(dataSourceFactory.createDataSource(taskExecution));
    }

    protected NamedParameterJdbcTemplate createNamedParameterJdbcTemplate(TaskExecution taskExecution) {
        return new NamedParameterJdbcTemplate(dataSourceFactory.createDataSource(taskExecution));
    }
}
