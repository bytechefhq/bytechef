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

import com.bytechef.hermes.definition.Display;
import com.bytechef.hermes.definition.Property;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class Action {

    public static final String ACTION = "action";

    protected Display display;
    protected String name;
    protected List<Property<?>> inputs;

    protected Action() {}

    public Action(String name) {
        this.name = name;
    }

    public Action display(Display display) {
        this.display = display;

        return this;
    }

    public Action inputs(Property<?>... inputs) {
        this.inputs = List.of(inputs);

        return this;
    }

    public Display getDisplay() {
        return display;
    }

    public String getName() {
        return name;
    }

    public List<Property<?>> getInputs() {
        return inputs;
    }
}
