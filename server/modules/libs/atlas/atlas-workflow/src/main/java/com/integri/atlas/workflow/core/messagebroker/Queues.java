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
package com.integri.atlas.workflow.core.messagebroker;

import com.integri.atlas.workflow.core.uuid.UUIDGenerator;

public interface Queues {

  static final String COMPLETIONS = "completions";
  static final String ERRORS      = "errors";
  static final String JOBS        = "jobs";
  static final String SUBFLOWS    = "subflows";
  static final String EXECUTE     = "execute";
  static final String DLQ         = "dlq";
  static final String CONTROL     = "x.control." + UUIDGenerator.generate();
  static final String TASKS       = "tasks";
  static final String EVENTS      = "events";

}
