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

package com.bytechef.platform.definition;

import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 *
 * @param name      The component action/component trigger/task dispatcher name
 * @param version   The component action/component trigger/task dispatcher version
 * @param operation The component action or trigger name
 */
public record WorkflowNodeType(String name, int version, @Nullable String operation) {

    public static WorkflowNodeType ofType(String type) {
        String[] typeItems = type.split("/");

        if (typeItems.length < 2) {
            throw new IllegalArgumentException("Wrong type format: %s".formatted(type));
        }

        return new WorkflowNodeType(
            typeItems[0], Integer.parseInt(typeItems[1].replace("v", "")), typeItems.length == 2 ? null : typeItems[2]);
    }
}
