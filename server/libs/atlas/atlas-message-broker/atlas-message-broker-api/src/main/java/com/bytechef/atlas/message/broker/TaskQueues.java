
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

package com.bytechef.atlas.message.broker;

import java.util.UUID;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
public final class TaskQueues {

    public static final String COMPLETIONS = "completions";
    public static final String CONTROL = "x.control." + UUID.randomUUID();
    public static final String DLQ = "dlq";
    public static final String ERRORS = "errors";
    public static final String EVENTS = "events";
    public static final String JOBS = "jobs";
    public static final String RESTARTS = "restarts";
    public static final String SUBFLOWS = "subflows";
    public static final String STOPS = "stops";
    public static final String TASKS = "tasks";
}
