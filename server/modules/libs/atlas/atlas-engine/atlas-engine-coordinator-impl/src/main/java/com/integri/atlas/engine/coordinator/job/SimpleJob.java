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

package com.integri.atlas.engine.coordinator.job;

import com.integri.atlas.engine.core.Accessor;
import com.integri.atlas.engine.core.DSL;
import com.integri.atlas.engine.core.MapObject;
import com.integri.atlas.engine.core.error.Error;
import com.integri.atlas.engine.core.error.ErrorObject;
import com.integri.atlas.engine.core.error.Prioritizable;
import com.integri.atlas.engine.core.task.TaskExecution;
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
        return getString(DSL.ID);
    }

    @Override
    public String getParentTaskExecutionId() {
        return getString(DSL.PARENT_TASK_EXECUTION_ID);
    }

    public void setParentTaskExecutionId(String aTaskExecutionId) {
        set(DSL.PARENT_TASK_EXECUTION_ID, aTaskExecutionId);
    }

    public void setId(String aId) {
        set(DSL.ID, aId);
    }

    @Override
    public int getCurrentTask() {
        return getInteger(DSL.CURRENT_TASK, -1);
    }

    public void setCurrentTask(int aCurrentStep) {
        set(DSL.CURRENT_TASK, aCurrentStep);
    }

    @Override
    public String getLabel() {
        return getString(DSL.LABEL);
    }

    public void setLabel(String aLabel) {
        set(DSL.LABEL, aLabel);
    }

    @Override
    public Error getError() {
        if (get(DSL.ERROR) != null) {
            return new ErrorObject(getMap(DSL.ERROR));
        }
        return null;
    }

    public void setError(Error aError) {
        set(DSL.ERROR, aError);
    }

    @Override
    public List<TaskExecution> getExecution() {
        List<TaskExecution> list = getList(DSL.EXECUTION, TaskExecution.class);
        return list != null ? list : Collections.emptyList();
    }

    @Override
    public JobStatus getStatus() {
        String value = getString(DSL.STATUS);
        return value != null ? JobStatus.valueOf(value) : null;
    }

    public void setStatus(JobStatus aStatus) {
        set(DSL.STATUS, aStatus);
    }

    public void setEndTime(Date aEndTime) {
        set(DSL.END_TIME, aEndTime);
    }

    public void setStartTime(Date aStartTime) {
        set(DSL.START_TIME, aStartTime);
    }

    @Override
    public Date getCreateTime() {
        return getDate(DSL.CREATE_TIME);
    }

    public void setCreateTime(Date aCreateTime) {
        set(DSL.CREATE_TIME, aCreateTime);
    }

    @Override
    public String getPipelineId() {
        return getString(DSL.PIPELINE_ID);
    }

    public void setPipelineId(String aPipelineId) {
        set(DSL.PIPELINE_ID, aPipelineId);
    }

    @Override
    public Date getStartTime() {
        return getDate(DSL.START_TIME);
    }

    @Override
    public Date getEndTime() {
        return getDate(DSL.END_TIME);
    }

    @Override
    public int getPriority() {
        return getInteger(DSL.PRIORITY, Prioritizable.DEFAULT_PRIORITY);
    }

    public void setPriority(int aPriority) {
        set(DSL.PRIORITY, aPriority);
    }

    @Override
    public Accessor getInputs() {
        Map<String, Object> map = getMap(DSL.INPUTS);
        return map != null ? new MapObject(map) : new MapObject();
    }

    public void setInputs(Accessor aInputs) {
        set(DSL.INPUTS, aInputs);
    }

    @Override
    public Accessor getOutputs() {
        Map<String, Object> map = getMap(DSL.OUTPUTS);
        return map != null ? new MapObject(map) : new MapObject();
    }

    public void setOutputs(Accessor aOutputs) {
        set(DSL.OUTPUTS, aOutputs);
    }

    @Override
    public List<Accessor> getWebhooks() {
        return getList(DSL.WEBHOOKS, Accessor.class, Collections.emptyList());
    }

    public void setWebhooks(List<Accessor> aWebhooks) {
        set(DSL.WEBHOOKS, aWebhooks);
    }
}
