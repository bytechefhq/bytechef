/*
 * Copyright 2025 ByteChef
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

import static com.bytechef.component.datastream.constant.DataStreamConstants.CLUSTER_ELEMENT_NAME;
import static com.bytechef.component.datastream.constant.DataStreamConstants.CONNECTION_PARAMETERS;
import static com.bytechef.component.datastream.constant.DataStreamConstants.INPUT_PARAMETERS;
import static com.bytechef.component.datastream.constant.DataStreamConstants.TENANT_ID;
import static com.bytechef.platform.configuration.constant.WorkflowExtConstants.COMPONENT_NAME;
import static com.bytechef.platform.configuration.constant.WorkflowExtConstants.COMPONENT_VERSION;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.definition.ContextFactory;
import com.bytechef.platform.component.definition.ParametersFactory;
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
public abstract class AbstractItemDelegate {

    protected boolean editorEnvironment;
    protected String componentName;
    protected String clusterElementName;
    protected int componentVersion;
    protected Parameters connectionParameters;
    protected ContextFactory contextFactory;
    protected Parameters inputParameters;
    protected String tenantId;

    private final ClusterElementType clusterElementType;

    protected AbstractItemDelegate(ClusterElementType clusterElementType, ContextFactory contextFactory) {
        this.contextFactory = contextFactory;
        this.clusterElementType = clusterElementType;
    }

    @BeforeStep
    @SuppressWarnings("unchecked")
    public void beforeStep(final StepExecution stepExecution) {
        JobParameters jobParameters = stepExecution
            .getJobExecution()
            .getJobParameters();

        JobParameter<?> jobParameter = Objects.requireNonNull(jobParameters.getParameter(clusterElementType.name()));

        Map<String, ?> clusterElementMap = (Map<String, ?>) jobParameter.getValue();

        componentName = MapUtils.getRequiredString(clusterElementMap, COMPONENT_NAME);
        componentVersion = MapUtils.getRequiredInteger(clusterElementMap, COMPONENT_VERSION);
        clusterElementName = MapUtils.getRequiredString(clusterElementMap, CLUSTER_ELEMENT_NAME);

        Map<String, ?> connectionParameterMap = MapUtils.getMap(clusterElementMap, CONNECTION_PARAMETERS);

        connectionParameters = connectionParameterMap == null
            ? null : ParametersFactory.createParameters(connectionParameterMap);

        inputParameters = ParametersFactory.createParameters(MapUtils.getMap(clusterElementMap, INPUT_PARAMETERS));

        jobParameter = Validate.notNull(jobParameters.getParameter(TENANT_ID), "tenantId is required");

        tenantId = (String) jobParameter.getValue();

        jobParameter = Validate.notNull(
            jobParameters.getParameter(MetadataConstants.EDITOR_ENVIRONMENT), "editorEnvironment is required");

        editorEnvironment = (boolean) jobParameter.getValue();

        doBeforeStep(stepExecution);
    }

    protected abstract void doBeforeStep(StepExecution stepExecution);
}
