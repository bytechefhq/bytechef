
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.component.task.handler.loader;

import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.definition.registry.facade.ConnectionDefinitionFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface ComponentTaskHandlerFactoryLoader {

    List<ComponentTaskHandlerFactory> loadComponentTaskHandlerFactories();

    interface TaskHandlerFactory {

        TaskHandler<?> createTaskHandler(ConnectionDefinitionFacade connectionDefinitionFacade);
    }

    @SuppressFBWarnings("EI")
    record ComponentTaskHandlerFactory(
        ComponentDefinition componentDefinition, List<TaskHandlerFactoryEntry> taskHandlerFactoryEntries) {
    }

    record TaskHandlerFactoryEntry(String actionDefinitionName, TaskHandlerFactory taskHandlerFactory) {
    }
}
