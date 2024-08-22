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

package com.bytechef.platform.component.definition;

import com.bytechef.component.definition.ActionDefinition;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class ActionDefinitionWrapper extends AbstractActionDefinitionWrapper implements ActionDefinition {

    protected final PerformFunction performFunction;

    public ActionDefinitionWrapper(ActionDefinition actionDefinition, PerformFunction performFunction) {
        super(actionDefinition);

        this.performFunction = performFunction;
    }

    @Override
    public Optional<PerformFunction> getPerform() {
        return Optional.ofNullable(performFunction);
    }

    @Override
    public String toString() {
        return "ActionDefinitionWrapper{" +
            ", name='" + name + '\'' +
            ", title='" + title + '\'' +
            ", description='" + description + '\'' +
            ", batch=" + batch +
            ", deprecated=" + deprecated +
            ", help=" + help +
            ", outputResponse=" + outputResponse +
            ", properties=" + properties +
            ", metadata=" + metadata +
            "} ";
    }
}
