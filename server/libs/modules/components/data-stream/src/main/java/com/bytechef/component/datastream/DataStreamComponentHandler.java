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

package com.bytechef.component.datastream;

import static com.bytechef.component.datastream.constant.DataStreamConstants.DATA_STREAM;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.datastream.action.DataStreamStreamAction;
import com.bytechef.component.datastream.batch.InMemoryBatchJobFactory;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.DataStreamComponentDefinition;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(DATA_STREAM + "_v1_ComponentHandler")
public class DataStreamComponentHandler implements ComponentHandler {

    private final DataStreamComponentDefinition componentDefinition;

    public DataStreamComponentHandler(
        @Qualifier("dataStreamJob") Job job,
        JobLauncher jobLauncher,
        InMemoryBatchJobFactory inMemoryBatchJobFactory) {

        this.componentDefinition = new DataStreamComponentDefinitionImpl(
            component(DATA_STREAM)
                .title("Data Stream")
                .description("With the Data Stream, you can transfer large amounts of data efficiently.")
                .icon("path:assets/data-stream.svg")
                .categories(ComponentCategory.HELPERS)
                .actions(
                    new DataStreamStreamAction(job, jobLauncher, inMemoryBatchJobFactory).actionDefinition));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class DataStreamComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements DataStreamComponentDefinition {

        public DataStreamComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
