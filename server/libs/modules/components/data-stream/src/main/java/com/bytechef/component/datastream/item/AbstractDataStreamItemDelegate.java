/*
 * Copyright 2023-present ByteChef Inc.
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
 */

package com.bytechef.component.datastream.item;

import static com.bytechef.component.datastream.constant.DataStreamConstants.CONNECTION_PARAMETERS;
import static com.bytechef.component.datastream.constant.DataStreamConstants.INPUT_PARAMETERS;
import static com.bytechef.component.datastream.constant.DataStreamConstants.INSTANCE_ID;
import static com.bytechef.component.datastream.constant.DataStreamConstants.INSTANCE_WORKFLOW_ID;
import static com.bytechef.component.datastream.constant.DataStreamConstants.JOB_ID;
import static com.bytechef.component.datastream.constant.DataStreamConstants.STREAM;
import static com.bytechef.component.datastream.constant.DataStreamConstants.TENANT_ID;
import static com.bytechef.component.datastream.constant.DataStreamConstants.TEST_ENVIRONMENT;
import static com.bytechef.component.datastream.constant.DataStreamConstants.TYPE;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.DataStreamContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ContextFactory;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.constant.AppType;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractDataStreamItemDelegate {

    protected String componentName;
    protected int componentVersion;
    protected Parameters connectionParameters;
    protected DataStreamContext context;
    protected Parameters inputParameters;
    protected String tenantId;
    protected boolean testEnvironment;

    private final String componentTypeName;
    private final ContextFactory contextFactory;

    protected AbstractDataStreamItemDelegate(String componentTypeName, ContextFactory contextFactory) {
        this.contextFactory = contextFactory;
        this.componentTypeName = componentTypeName;
    }

    @BeforeStep
    public void beforeStep(final StepExecution stepExecution) {
        JobParameters jobParameters = stepExecution
            .getJobExecution()
            .getJobParameters();

        Map<String, ?> componentTypeParameterMap = getParameterMap(jobParameters, componentTypeName);

        componentName = MapUtils.getRequiredString(componentTypeParameterMap, "componentName");
        componentVersion = MapUtils.getRequiredInteger(componentTypeParameterMap, "componentVersion");

        Map<String, ?> connectionParameterMap = MapUtils.getMap(
            MapUtils.getMap(getParameterMap(jobParameters, CONNECTION_PARAMETERS), componentTypeName, Map.of()),
            "parameters");

        connectionParameters = connectionParameterMap == null
            ? null : ParametersFactory.createParameters(connectionParameterMap);

        inputParameters = ParametersFactory.createParameters(
            MapUtils.getMap(getParameterMap(jobParameters, INPUT_PARAMETERS), componentTypeName, Map.of()));

        JobParameter<?> jobParameter = jobParameters.getParameter(INSTANCE_ID);

        Long instanceId = null;

        if (jobParameter != null) {
            instanceId = (Long) jobParameter.getValue();
        }

        jobParameter = jobParameters.getParameter(INSTANCE_WORKFLOW_ID);

        Long instanceWorkflowId = null;

        if (jobParameter != null) {
            instanceWorkflowId = (Long) jobParameter.getValue();
        }

        jobParameter = jobParameters.getParameter(JOB_ID);

        long jobId = (Long) Validate.notNull(jobParameter, "jobId is required")
            .getValue();

        jobParameter = jobParameters.getParameter(TENANT_ID);

        tenantId = (String) Validate.notNull(jobParameter, "tenantId is required")
            .getValue();

        jobParameter = jobParameters.getParameter(TEST_ENVIRONMENT);

        testEnvironment = (boolean) Validate.notNull(jobParameter, "testEnvironment is required")
            .getValue();

        jobParameter = jobParameters.getParameter(TYPE);

        AppType type = null;

        if (jobParameter != null) {
            type = AppType.valueOf((String) jobParameter.getValue());
        }

        context = new DataStreamContextImpl(
            contextFactory.createActionContext(
                componentName, componentVersion, STREAM, type, instanceId, instanceWorkflowId,
                null, jobId, null, testEnvironment));

        doBeforeStep(stepExecution);
    }

    protected abstract void doBeforeStep(StepExecution stepExecution);

    @SuppressWarnings("unchecked")
    private Map<String, ?> getParameterMap(JobParameters jobParameters, String key) {
        JobParameter<?> jobParameter = Objects.requireNonNull(jobParameters.getParameter(key));

        return (Map<String, ?>) jobParameter.getValue();
    }
}
