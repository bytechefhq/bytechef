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

package com.integri.atlas.engine.core.task;

import com.integri.atlas.engine.core.DSL;
import com.integri.atlas.engine.core.error.Error;
import com.integri.atlas.engine.core.error.ErrorObject;
import com.integri.atlas.engine.core.error.Prioritizable;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

public class SimpleTaskExecution extends SimplePipelineTask implements TaskExecution {

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
        return getString(DSL.ID);
    }

    public void setId(String aId) {
        set(DSL.ID, aId);
    }

    @Override
    public String getParentId() {
        return getString(DSL.PARENT_ID);
    }

    public void setParentId(String aParentId) {
        set(DSL.PARENT_ID, aParentId);
    }

    @Override
    public String getJobId() {
        return getString(DSL.JOB_ID);
    }

    public void setJobId(String aJobId) {
        set(DSL.JOB_ID, aJobId);
    }

    @Override
    public TaskStatus getStatus() {
        String status = getString(DSL.STATUS);
        if (status == null) return null;
        return TaskStatus.valueOf(status);
    }

    @Override
    public int getProgress() {
        return get(DSL.PROGRESS, Integer.class, 0);
    }

    public void setProgress(int progress) {
        set(DSL.PROGRESS, progress);
    }

    @Override
    public Object getOutput() {
        return get(DSL.OUTPUT);
    }

    public void setOutput(Object aOutput) {
        set(DSL.OUTPUT, aOutput);
    }

    @Override
    public Error getError() {
        if (containsKey(DSL.ERROR)) {
            return new ErrorObject(getMap(DSL.ERROR));
        }
        return null;
    }

    public void setError(Error aError) {
        set(DSL.ERROR, aError);
    }

    public void setStatus(TaskStatus aStatus) {
        set(DSL.STATUS, aStatus);
    }

    @Override
    public Date getCreateTime() {
        return getDate(DSL.CREATE_TIME);
    }

    public void setCreateTime(Date aDate) {
        set(DSL.CREATE_TIME, aDate);
    }

    @Override
    public Date getStartTime() {
        return getDate(DSL.START_TIME);
    }

    public void setStartTime(Date aDate) {
        set(DSL.START_TIME, aDate);
    }

    @Override
    public Date getEndTime() {
        return getDate(DSL.END_TIME);
    }

    public void setEndTime(Date aDate) {
        set(DSL.END_TIME, aDate);
    }

    @Override
    public long getExecutionTime() {
        if (getDate(DSL.EXECUTION_TIME) != null) {
            return getDate(DSL.EXECUTION_TIME).getTime();
        }
        return 0;
    }

    public void setExecutionTime(long aExecutionTime) {
        set(DSL.EXECUTION_TIME, aExecutionTime);
    }

    @Override
    public int getRetry() {
        return getInteger(DSL.RETRY, 0);
    }

    public void setRetry(int aRetry) {
        set(DSL.RETRY, aRetry);
    }

    @Override
    public int getRetryAttempts() {
        return getInteger(DSL.RETRY_ATTEMPTS, 0);
    }

    @Override
    public String getRetryDelay() {
        return getString(DSL.RETRY_DELAY, "1s");
    }

    @Override
    public long getRetryDelayMillis() {
        long delay = Duration.parse("PT" + getRetryDelay()).toMillis();
        int retryAttempts = getRetryAttempts();
        int retryDelayFactor = getRetryDelayFactor();
        return delay * retryAttempts * retryDelayFactor;
    }

    public void setRetryAttempts(int aRetryAttempts) {
        set(DSL.RETRY_ATTEMPTS, aRetryAttempts);
    }

    @Override
    public int getRetryDelayFactor() {
        return getInteger(DSL.RETRY_DELAY_FACTOR, 2);
    }

    @Override
    public int getPriority() {
        return getInteger(DSL.PRIORITY, Prioritizable.DEFAULT_PRIORITY);
    }

    public void setPriority(int aPriority) {
        set(DSL.PRIORITY, aPriority);
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
