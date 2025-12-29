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

import static com.bytechef.component.definition.datastream.ItemReader.SOURCE;

import com.bytechef.component.definition.datastream.ItemReader;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.tenant.TenantContext;
import java.util.Map;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

/**
 * @author Ivica Cardic
 */
public class ItemStreamReaderDelegate extends AbstractItemStreamDelegate
    implements ItemStreamReader<Map<String, Object>> {

    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private ItemReader itemReader;

    public ItemStreamReaderDelegate(
        ClusterElementDefinitionService clusterElementDefinitionService, ContextFactory contextFactory) {

        super(SOURCE, contextFactory);

        this.clusterElementDefinitionService = clusterElementDefinitionService;
    }

    @Override
    public void close() throws ItemStreamException {
        itemReader.close();
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        ItemStreamExecutionContext itemStreamExecutionContext = new ItemStreamExecutionContext(executionContext);

        TenantContext.runWithTenantId(
            tenantId, () -> itemReader.open(
                inputParameters, connectionParameters, clusterElementContext, itemStreamExecutionContext));
    }

    @Override
    public Map<String, Object> read() {
        return TenantContext.callWithTenantId(tenantId, () -> itemReader.read());
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        ItemStreamExecutionContext itemStreamExecutionContext = new ItemStreamExecutionContext(executionContext);

        TenantContext.runWithTenantId(
            tenantId, () -> itemReader.update(
                inputParameters, connectionParameters, clusterElementContext, itemStreamExecutionContext));
    }

    protected void doBeforeStep(final StepExecution stepExecution) {
        itemReader = clusterElementDefinitionService.getClusterElement(
            componentName, componentVersion, clusterElementName);
    }
}
