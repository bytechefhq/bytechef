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

import static com.bytechef.component.datastream.constant.DataStreamConstants.SOURCE;

import com.bytechef.component.definition.DataStreamItemReader;
import com.bytechef.platform.component.definition.ContextFactory;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.tenant.util.TenantUtils;
import java.util.Map;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;

/**
 * @author Ivica Cardic
 */
public class DataStreamItemReaderDelegate extends AbstractDataStreamItemDelegate
    implements ItemStreamReader<Map<String, ?>> {

    private final ComponentDefinitionService componentDefinitionService;
    private DataStreamItemReader dataStreamItemReader;

    public DataStreamItemReaderDelegate(
        ComponentDefinitionService componentDefinitionService, ContextFactory contextFactory) {

        super(SOURCE, contextFactory);

        this.componentDefinitionService = componentDefinitionService;
    }

    @Override
    public void close() throws ItemStreamException {
        if (dataStreamItemReader != null) {
            dataStreamItemReader.close();
        }
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        TenantUtils.runWithTenantId(
            tenantId, () -> dataStreamItemReader.open(inputParameters, connectionParameters, context));
    }

    @Override
    public Map<String, ?> read() {
        return TenantUtils.callWithTenantId(tenantId, () -> dataStreamItemReader.read(context));
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        TenantUtils.runWithTenantId(tenantId, () -> dataStreamItemReader.update(context));
    }

    protected void doBeforeStep(final StepExecution stepExecution) {
        dataStreamItemReader = componentDefinitionService.getDataStreamItemReader(componentName, componentVersion);
    }
}
