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

package com.bytechef.workflow.definition;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface Input {

    /**
     * The component input this input takes its options from - for example Slack's {@code channel}, so a connected user
     * picks a channel from a list instead of typing its id.
     */
    default Optional<ComponentInputReference> getComponentInput() {
        return Optional.empty();
    }

    String getLabel();

    String getName();

    boolean isRequired();

    String getType();

    /**
     * Points at the input group named {@code groupName} that the component {@code componentName}, version
     * {@code componentVersion}, declares with {@code component(...).inputs(...)}.
     */
    record ComponentInputReference(String componentName, int componentVersion, String groupName) {

        public ComponentInputReference {
            Objects.requireNonNull(componentName, "componentName");
            Objects.requireNonNull(groupName, "groupName");
        }
    }
}
