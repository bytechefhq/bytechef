/*
 * Copyright 2016-2020 the original author or authors.
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
 * Modifications copyright (C) 2023 ByteChef Inc.
 */

package com.bytechef.atlas.configuration.constant;

import java.util.List;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since May 26, 2017
 */
public class WorkflowConstants {

    public static final String DEFAULT = "default";
    public static final String DESCRIPTION = "description";
    public static final String EVENT = "event";
    public static final String FINALIZE = "finalize";
    public static final String INPUTS = "inputs";
    public static final String LABEL = "label";
    public static final String MAX_RETRIES = "maxRetries";
    public static final String METADATA = "metadata";
    public static final String NAME = "name";
    public static final String NODE = "node";
    public static final String OUTPUTS = "outputs";
    public static final String PARAMETERS = "parameters";
    public static final String PATH = "path";
    public static final String POST = "post";
    public static final String PRE = "pre";
    public static final String REQUIRED = "required";
    public static final String TASK_NUMBER = "taskNumber";
    public static final String TASKS = "tasks";
    public static final String TIMEOUT = "timeout";
    public static final String TYPE = "type";
    public static final String URL = "url";
    public static final String VALUE = "value";
    public static final String WORKFLOW_ID = "workflowId";

    public static final List<String> WORKFLOW_DEFINITION_CONSTANTS = List.of(
        DEFAULT, DESCRIPTION, FINALIZE, INPUTS, LABEL, METADATA, NAME, NODE, OUTPUTS, PARAMETERS, POST, PRE,
        MAX_RETRIES, REQUIRED, TASKS, TIMEOUT, TYPE, VALUE);
}
