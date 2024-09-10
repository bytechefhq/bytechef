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

package com.bytechef.component.datastream;

import static com.bytechef.component.definition.ComponentDSL.component;
import static com.bytechef.platform.component.definition.DataStreamComponentDefinition.DATA_STREAM;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.datastream.action.DataStreamStreamAction;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.DataStreamComponentDefinition;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(DATA_STREAM + "_v1_ComponentHandler")
public class DataStreamComponentHandler implements ComponentHandler {

    private final DataStreamComponentDefinition componentDefinition;

    public DataStreamComponentHandler(Job job, JobLauncher jobLauncher) {
        this.componentDefinition = new DataStreamComponentDefinitionImpl(
            component(DATA_STREAM)
                .title("Data Stream")
                .description("With the Data Stream, you can transfer large amounts of data efficiently.")
                .icon("path:assets/data-stream.svg")
                .categories(ComponentCategory.HELPERS)
                .actions(new DataStreamStreamAction(job, jobLauncher).actionDefinition));
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
