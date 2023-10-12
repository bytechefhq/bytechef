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

package com.bytechef.hermes.component;

import com.bytechef.hermes.component.definition.ComponentDefinition;

/**
 * Default component definition factory.
 *
 * @author Ivica Cardic
 */
public interface ComponentDefinitionFactory {

    /**
     *
     * @return
     */
    ComponentDefinition getDefinition();

    default String getName() {
        ComponentDefinition componentDefinition = getDefinition();

        return componentDefinition.getName();
    }

    default int getVersion() {
        ComponentDefinition componentDefinition = getDefinition();

        return componentDefinition.getVersion();
    }
}
