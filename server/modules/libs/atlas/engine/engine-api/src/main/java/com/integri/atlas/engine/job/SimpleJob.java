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

package com.integri.atlas.engine.job;

import com.integri.atlas.engine.Accessor;
import com.integri.atlas.engine.Constants;
import com.integri.atlas.engine.MapObject;
import com.integri.atlas.engine.error.Error;
import com.integri.atlas.engine.error.ErrorObject;
import com.integri.atlas.engine.priority.Prioritizable;
import com.integri.atlas.engine.task.execution.TaskExecution;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * An implementation of the {@link Job} interface
 * based on {@link MapObject}.
 *
 * @author Arik Cohen
 */
public class SimpleJob extends MapObject implements Job {

    public SimpleJob() {
        super(Collections.emptyMap());
    }

    public SimpleJob(Map<String, Object> aSource) {
        super(aSource);
    }

    public SimpleJob(Job aSource) {
        super(aSource.asMap());
    }

    @Override
    public String getId() {
        return getString(Constants.ID);
    }

    @Override
    public String getParentTaskExecutionId() {
        return getString(Constants.PARENT_TASK_EXECUTION_ID);
    }

    public void setParentTaskExecutionId(String aTaskExecutionId) {
        set(Constants.PARENT_TASK_EXECUTION_ID, aTaskExecutionId);
    }

    public void setId(String aId) {
        set(Constants.ID, aId);
    }

    @Override
    public int getCurrentTask() {
        return getInteger(Constants.CURRENT_TASK, -1);
    }

    public void setCurrentTask(int aCurrentStep) {
        set(Constants.CURRENT_TASK, aCurrentStep);
    }

    @Override
    public String getLabel() {
        return getString(Constants.LABEL);
    }

    public void setLabel(String aLabel) {
        set(Constants.LABEL, aLabel);
    }

    @Override
    public Error getError() {
        if (get(Constants.ERROR) != null) {
            return new ErrorObject(getMap(Constants.ERROR));
        }
        return null;
    }

    public void setError(Error aError) {
        set(Constants.ERROR, aError);
    }

    @Override
    public List<TaskExecution> getExecutions() {
        List<TaskExecution> list = getList(Constants.EXECUTIONS, TaskExecution.class);
        return list != null ? list : Collections.emptyList();
    }

    public void setExecutions(List<TaskExecution> taskExecutions) {
        put(Constants.EXECUTIONS, taskExecutions);
    }

    @Override
    public JobStatus getStatus() {
        String value = getString(Constants.STATUS);
        return value != null ? JobStatus.valueOf(value) : null;
    }

    public void setStatus(JobStatus aStatus) {
        set(Constants.STATUS, aStatus);
    }

    public void setEndTime(Date aEndTime) {
        set(Constants.END_TIME, aEndTime);
    }

    public void setStartTime(Date aStartTime) {
        set(Constants.START_TIME, aStartTime);
    }

    @Override
    public Date getCreateTime() {
        return getDate(Constants.CREATE_TIME);
    }

    public void setCreateTime(Date aCreateTime) {
        set(Constants.CREATE_TIME, aCreateTime);
    }

    @Override
    public String getWorkflowId() {
        return getString(Constants.WORKFLOW_ID);
    }

    public void setWorkflowId(String aWorkflowId) {
        set(Constants.WORKFLOW_ID, aWorkflowId);
    }

    @Override
    public Date getStartTime() {
        return getDate(Constants.START_TIME);
    }

    @Override
    public Date getEndTime() {
        return getDate(Constants.END_TIME);
    }

    @Override
    public int getPriority() {
        return getInteger(Constants.PRIORITY, Prioritizable.DEFAULT_PRIORITY);
    }

    public void setPriority(int aPriority) {
        set(Constants.PRIORITY, aPriority);
    }

    @Override
    public Accessor getInputs() {
        Map<String, Object> map = getMap(Constants.INPUTS);
        return map != null ? new MapObject(map) : new MapObject();
    }

    public void setInputs(Accessor aInputs) {
        set(Constants.INPUTS, aInputs);
    }

    @Override
    public Accessor getOutputs() {
        Map<String, Object> map = getMap(Constants.OUTPUTS);
        return map != null ? new MapObject(map) : new MapObject();
    }

    public void setOutputs(Accessor aOutputs) {
        set(Constants.OUTPUTS, aOutputs);
    }

    @Override
    public List<Accessor> getWebhooks() {
        return getList(Constants.WEBHOOKS, Accessor.class, Collections.emptyList());
    }

    public void setWebhooks(List<Accessor> aWebhooks) {
        set(Constants.WEBHOOKS, aWebhooks);
    }
}
