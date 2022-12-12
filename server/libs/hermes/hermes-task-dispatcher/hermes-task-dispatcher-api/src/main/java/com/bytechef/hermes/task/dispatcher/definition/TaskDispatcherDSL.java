
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

package com.bytechef.hermes.task.dispatcher.definition;

import com.bytechef.hermes.definition.DefinitionDSL;
import com.bytechef.hermes.definition.Property;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public final class TaskDispatcherDSL extends DefinitionDSL {

    public static ModifiableTaskDispatcherDefinition taskDispatcher(String name) {
        return new ModifiableTaskDispatcherDefinition(name);
    }

    public static ModifiableProperty.ModifiableObjectProperty task() {
        return task(null);
    }

    public static ModifiableProperty.ModifiableObjectProperty task(String name) {
        return buildObject(name, "The task or task dispatcher to use.", "TASK");
    }

    public static final class ModifiableTaskDispatcherDefinition extends TaskDispatcherDefinition {

        private ModifiableTaskDispatcherDefinition(String name) {
            super(name);
        }

        public ModifiableTaskDispatcherDefinition display(ModifiableDisplay display) {
            this.display = display;

            return this;
        }

        public ModifiableTaskDispatcherDefinition display(ModifiableResources resources) {
            this.resources = resources;

            return this;
        }

        public <P extends Property<?>> ModifiableTaskDispatcherDefinition output(P... output) {
            if (output != null) {
                this.output = List.of(output);
            }

            return this;
        }

        public <P extends Property<?>> ModifiableTaskDispatcherDefinition properties(P... properties) {
            if (properties != null) {
                this.properties = List.of(properties);
            }

            return this;
        }

        public ModifiableTaskDispatcherDefinition version(int version) {
            this.version = version;

            return this;
        }

        public <P extends Property<?>> ModifiableTaskDispatcherDefinition taskProperties(P... taskProperties) {
            this.taskProperties = List.of(taskProperties);

            return this;
        }
    }
}
