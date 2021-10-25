/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.repository.engine.jdbc.task;

/**
 * @author Ivica Cardic
 */
public class MysqlJdbcTaskExecutionRepository extends AbstractJdbcTaskExecutionRepository {

    private static final String CREATE_SQL =
        "insert into task_execution " +
        "  (id,parent_id,job_id,serialized_execution,status,progress,create_time,priority,task_number) " +
        "values " +
        "  (:id,:parentId,:jobId,:serializedExecution,:status,:progress,:createTime,:priority,:taskNumber)";

    private static final String MERGE_SQL =
        "update task_execution set " +
        "  serialized_execution=:serializedExecution,status=:status,progress=:progress,start_time=:startTime,end_time=:endTime where id = :id ";

    @Override
    protected String getCreateSql() {
        return CREATE_SQL;
    }

    @Override
    protected String getMergeSQL() {
        return MERGE_SQL;
    }
}
