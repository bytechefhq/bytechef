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

package com.bytechef.hermes.component.definition;

import com.bytechef.hermes.component.PerformFunction;
import com.bytechef.hermes.definition.Display;
import com.bytechef.hermes.definition.Property;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public final class ComponentAction extends Action {

    private Object exampleOutput;
    private List<Property<?>> outputSchema;

    @JsonIgnore
    private PerformFunction performFunction;

    private ComponentAction() {}

    public ComponentAction(String name) {
        super(name);
    }

    public ComponentAction display(Display display) {
        this.display = display;

        return this;
    }

    public ComponentAction exampleOutput(Object exampleOutput) {
        this.exampleOutput = exampleOutput;

        return this;
    }

    public ComponentAction inputs(Property<?>... inputs) {
        this.inputs = List.of(inputs);

        return this;
    }

    @JsonIgnore
    public ComponentAction outputSchema(Property<?>... outputSchema) {
        this.outputSchema = List.of(outputSchema);

        return this;
    }

    @JsonIgnore
    public ComponentAction performFunction(PerformFunction performFunction) {
        this.performFunction = performFunction;

        return this;
    }

    public Object getExampleOutput() {
        return exampleOutput;
    }

    public List<Property<?>> getOutputSchema() {
        return outputSchema;
    }

    public PerformFunction getPerformFunction() {
        return performFunction;
    }
}
