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

package com.integri.atlas.engine.task.execution;

import com.integri.atlas.engine.Constants;
import com.integri.atlas.engine.error.Error;
import com.integri.atlas.engine.error.ErrorObject;
import com.integri.atlas.engine.priority.Prioritizable;
import com.integri.atlas.engine.task.SimpleWorkflowTask;
import com.integri.atlas.engine.task.Task;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * @author Arik Cohen
 */
public class SimpleTaskExecution extends SimpleWorkflowTask implements TaskExecution {

    public SimpleTaskExecution() {
        this(Collections.emptyMap());
    }

    private SimpleTaskExecution(TaskExecution aSource) {
        this(aSource.asMap());
    }

    public SimpleTaskExecution(Map<String, Object> aSource) {
        super(aSource);
    }

    private SimpleTaskExecution(Task aSource) {
        super(aSource);
    }

    @Override
    public String getId() {
        return getString(Constants.ID);
    }

    public void setId(String aId) {
        set(Constants.ID, aId);
    }

    @Override
    public String getParentId() {
        return getString(Constants.PARENT_ID);
    }

    public void setParentId(String aParentId) {
        set(Constants.PARENT_ID, aParentId);
    }

    @Override
    public String getJobId() {
        return getString(Constants.JOB_ID);
    }

    public void setJobId(String aJobId) {
        set(Constants.JOB_ID, aJobId);
    }

    @Override
    public TaskStatus getStatus() {
        String status = getString(Constants.STATUS);
        if (status == null) return null;
        return TaskStatus.valueOf(status);
    }

    @Override
    public int getProgress() {
        return get(Constants.PROGRESS, Integer.class, 0);
    }

    public void setProgress(int progress) {
        set(Constants.PROGRESS, progress);
    }

    @Override
    public Object getOutput() {
        return get(Constants.OUTPUT);
    }

    public void setOutput(Object aOutput) {
        set(Constants.OUTPUT, aOutput);
    }

    @Override
    public Error getError() {
        if (containsKey(Constants.ERROR)) {
            return new ErrorObject(getMap(Constants.ERROR));
        }
        return null;
    }

    public void setError(Error aError) {
        set(Constants.ERROR, aError);
    }

    public void setStatus(TaskStatus aStatus) {
        set(Constants.STATUS, aStatus);
    }

    @Override
    public Date getCreateTime() {
        return getDate(Constants.CREATE_TIME);
    }

    public void setCreateTime(Date aDate) {
        set(Constants.CREATE_TIME, aDate);
    }

    @Override
    public Date getStartTime() {
        return getDate(Constants.START_TIME);
    }

    public void setStartTime(Date aDate) {
        set(Constants.START_TIME, aDate);
    }

    @Override
    public Date getEndTime() {
        return getDate(Constants.END_TIME);
    }

    public void setEndTime(Date aDate) {
        set(Constants.END_TIME, aDate);
    }

    @Override
    public long getExecutionTime() {
        if (getDate(Constants.EXECUTION_TIME) != null) {
            return getDate(Constants.EXECUTION_TIME).getTime();
        }
        return 0;
    }

    public void setExecutionTime(long aExecutionTime) {
        set(Constants.EXECUTION_TIME, aExecutionTime);
    }

    @Override
    public int getRetry() {
        return getInteger(Constants.RETRY, 0);
    }

    public void setRetry(int aRetry) {
        set(Constants.RETRY, aRetry);
    }

    @Override
    public int getRetryAttempts() {
        return getInteger(Constants.RETRY_ATTEMPTS, 0);
    }

    @Override
    public String getRetryDelay() {
        return getString(Constants.RETRY_DELAY, "1s");
    }

    @Override
    public long getRetryDelayMillis() {
        long delay = Duration.parse("PT" + getRetryDelay()).toMillis();
        int retryAttempts = getRetryAttempts();
        int retryDelayFactor = getRetryDelayFactor();
        return delay * retryAttempts * retryDelayFactor;
    }

    public void setRetryAttempts(int aRetryAttempts) {
        set(Constants.RETRY_ATTEMPTS, aRetryAttempts);
    }

    @Override
    public int getRetryDelayFactor() {
        return getInteger(Constants.RETRY_DELAY_FACTOR, 2);
    }

    @Override
    public int getPriority() {
        return getInteger(Constants.PRIORITY, Prioritizable.DEFAULT_PRIORITY);
    }

    public void setPriority(int aPriority) {
        set(Constants.PRIORITY, aPriority);
    }

    /**
     * Creates a mutation {@link SimpleTaskExecution} instance which
     * is a copy of a {@link TaskExecution}.
     *
     * @param aSource
     *          The {@link TaskExecution} instance to copy.
     * @return the new {@link SimpleTaskExecution}
     */
    public static SimpleTaskExecution of(TaskExecution aSource) {
        return new SimpleTaskExecution(aSource);
    }

    /**
     * Creates a {@link SimpleTaskExecution} instance for the given
     * Key-Value pair.
     *
     * @return The new {@link SimpleTaskExecution}.
     */
    public static SimpleTaskExecution of(String aKey, Object aValue) {
        return new SimpleTaskExecution(Collections.singletonMap(aKey, aValue));
    }

    /**
     * Creates a {@link SimpleTaskExecution} instance for the given Key-Value
     * map.
     *
     * @return The new {@link SimpleTaskExecution}.
     */
    public static SimpleTaskExecution of(Map<String, Object> aSource) {
        return new SimpleTaskExecution(aSource);
    }
}
