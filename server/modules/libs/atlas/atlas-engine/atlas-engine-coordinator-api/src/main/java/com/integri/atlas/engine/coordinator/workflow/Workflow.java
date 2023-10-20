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

package com.integri.atlas.engine.coordinator.workflow;

import com.integri.atlas.engine.core.Accessor;
import com.integri.atlas.engine.core.error.Errorable;
import com.integri.atlas.engine.core.task.WorkflowTask;
import java.util.List;

/**
 * Workflows are the the blueprints that describe
 * the execution of a job.
 *
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Jun 12, 2016
 */
public interface Workflow extends Errorable {
    /**
     * Returns the unique identifier of the workflow.
     */
    String getId();

    /**
     * Returns a descriptive name for the workflow.
     */
    String getLabel();

    /**
     * Returns the steps that make up the workflow.
     */
    List<WorkflowTask> getTasks();

    /**
     * Returns the workflow's expected inputs
     */
    List<Accessor> getInputs();

    /**
     * Returns the workflow's expected outputs
     */
    List<Accessor> getOutputs();

    /**
     * Defines the maximum number of times that
     * this message may be retries.
     *
     * @return int the maximum number of retries.
     */
    int getRetry();
}
