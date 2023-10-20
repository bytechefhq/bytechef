/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.atlas;

/**
 * @author Arik Cohen
 * @since May 26, 2017
 */
public class Constants {

    public static final String ID = "id";

    public static final String PARENT_ID = "parentId";

    public static final String JOB_ID = "jobId";

    public static final String LABEL = "label";

    public static final String NODE = "node";

    public static final String TYPE = "type";

    public static final String NAME = "name";

    public static final String RETRY = "retry";

    public static final String RETRY_ATTEMPTS = "retryAttempts";

    public static final String RETRY_DELAY = "retryDelay";

    public static final String RETRY_DELAY_FACTOR = "retryDelayFactor";

    public static final String EXECUTION_TIME = "executionTime";

    public static final String CREATE_TIME = "createTime";

    public static final String WORKFLOW_ID = "workflowId";

    public static final String START_TIME = "startTime";

    public static final String TASK_NUMBER = "taskNumber";

    public static final String TIMEOUT = "timeout";

    public static final String END_TIME = "endTime";

    public static final String STATUS = "status";

    public static final String PROGRESS = "progress";

    public static final String OUTPUT = "output";

    public static final String ERROR = "error";

    public static final String TASKS = "tasks";

    public static final String INPUTS = "inputs";

    public static final String INPUT = "input";

    public static final String EXECUTIONS = "executions";

    public static final String PRIORITY = "priority";

    public static final String IF = "if";

    public static final String LOOP = "loop";

    public static final String LOOP_BREAK = "loop-break";

    public static final String SWITCH = "switch";

    public static final String EACH = "each";

    public static final String PARALLEL = "parallel";

    public static final String SEQUENCE = "sequence";

    public static final String SUBFLOW = "subflow";

    public static final String FORK = "fork";

    public static final String CURRENT_TASK = "currentTask";

    public static final String PARENT_TASK_EXECUTION_ID = "parentTaskExecutionId";

    public static final String MAP = "map";

    public static final String WEBHOOKS = "webhooks";

    public static final String URL = "url";

    public static final String REQUIRED = "required";

    public static final String EVENT = "event";

    public static final String OUTPUTS = "outputs";

    public static final String VALUE = "value";

    public static final String PRE = "pre";

    public static final String POST = "post";

    public static final String PROPERTIES = "properties";

    public static final String FINALIZE = "finalize";

    public static final String UPDATE_TIME = "updateTime";

    public static final String[] RESERVED_WORDS = new String[] {
        ID,
        PARENT_ID,
        JOB_ID,
        RETRY_ATTEMPTS,
        EXECUTION_TIME,
        CREATE_TIME,
        START_TIME,
        TASK_NUMBER,
        END_TIME,
        STATUS,
        ERROR,
        EXECUTIONS,
        PRIORITY,
        CURRENT_TASK,
        PARENT_TASK_EXECUTION_ID,
        OUTPUT,
    };
}
