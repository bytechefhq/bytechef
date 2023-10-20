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
import com.integri.atlas.engine.core.DSL;
import com.integri.atlas.engine.core.MapObject;
import com.integri.atlas.engine.core.error.Error;
import com.integri.atlas.engine.core.error.ErrorObject;
import com.integri.atlas.engine.core.task.WorkflowTask;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * @author Arik Cohen
 */
public class SimpleWorkflow extends MapObject implements Workflow {

    public SimpleWorkflow(Map<String, Object> aSource) {
        super(aSource);
    }

    @Override
    public String getId() {
        return getString(DSL.ID);
    }

    @Override
    public String getLabel() {
        return getString(DSL.LABEL);
    }

    @Override
    public List<WorkflowTask> getTasks() {
        return getList(DSL.TASKS, WorkflowTask.class);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public List<Accessor> getInputs() {
        return getList(DSL.INPUTS, Accessor.class, Collections.emptyList());
    }

    @Override
    public List<Accessor> getOutputs() {
        return getList(DSL.OUTPUTS, Accessor.class, Collections.emptyList());
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

    @Override
    public int getRetry() {
        return getInteger(DSL.RETRY, 0);
    }
}
