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
 */

package com.integri.atlas.repository.engine.jdbc.job;

/**
 * @author Ivica Cardic
 */
public class MysqlJdbcJobRepository extends AbstractJdbcJobRepository {

    private static final String CREATE_SQL =
        "insert into job (id,create_time,start_time,status,current_task,workflow_id,label,priority,inputs,webhooks,outputs,parent_task_execution_id) values (:id,:createTime,:startTime,:status,:currentTask,:workflowId,:label,:priority,:inputs,:webhooks,:outputs,:parentTaskExecutionId)";

    private static final String MERGE_SQL =
        "update job set status=:status,start_time=:startTime,end_time=:endTime,current_task=:currentTask,workflow_id=:workflowId,label=:label,outputs=:outputs where id = :id ";

    @Override
    protected String getCreateSQL() {
        return CREATE_SQL;
    }

    @Override
    protected String getMergeSql() {
        return MERGE_SQL;
    }
}
