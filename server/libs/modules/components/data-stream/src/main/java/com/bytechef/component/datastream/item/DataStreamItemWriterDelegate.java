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

import static com.bytechef.component.datastream.constant.DataStreamConstants.DESTINATION;

import com.bytechef.component.definition.DataStreamItemWriter;
import com.bytechef.platform.component.definition.ContextFactory;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.tenant.util.TenantUtils;
import java.util.Map;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamWriter;

/**
 * @author Ivica Cardic
 */
public class DataStreamItemWriterDelegate extends AbstractDataStreamItemDelegate
    implements ItemStreamWriter<Map<String, ?>> {

    private final ComponentDefinitionService componentDefinitionService;
    private DataStreamItemWriter dataStreamItemWriter;

    public DataStreamItemWriterDelegate(
        ComponentDefinitionService componentDefinitionService, ContextFactory contextFactory) {

        super(DESTINATION, contextFactory);

        this.componentDefinitionService = componentDefinitionService;
    }

    @Override
    public void close() throws ItemStreamException {
        if (dataStreamItemWriter != null) {
            dataStreamItemWriter.close();
        }
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        TenantUtils.runWithTenantId(
            tenantId, () -> dataStreamItemWriter.open(inputParameters, connectionParameters, context));
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        TenantUtils.runWithTenantId(tenantId, () -> dataStreamItemWriter.update(context));
    }

    @Override
    public void write(Chunk<? extends Map<String, ?>> chunk) {
        TenantUtils.runWithTenantId(tenantId, () -> dataStreamItemWriter.write(chunk.getItems(), context));
    }

    protected void doBeforeStep(final StepExecution stepExecution) {
        dataStreamItemWriter = componentDefinitionService.getDataStreamItemWriter(componentName, componentVersion);
    }
}
