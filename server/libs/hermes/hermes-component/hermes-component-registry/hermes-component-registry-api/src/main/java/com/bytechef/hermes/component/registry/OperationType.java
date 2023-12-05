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

package com.bytechef.hermes.component.registry;

/**
 * @author Ivica Cardic
 *
 * @param componentName          The component name
 * @param componentVersion       The component version
 * @param componentOperationName The component action or trigger name
 */
public record OperationType(String componentName, int componentVersion, String componentOperationName) {

    public static OperationType ofType(String type) {
        String[] typeItems = type.split("/");

        if (typeItems.length < 2) {
            throw new IllegalArgumentException("Wrong type format: %s".formatted(type));
        }

        return new OperationType(
            typeItems[0], Integer.parseInt(typeItems[1].replace("v", "")), typeItems.length == 2 ? null : typeItems[2]);
    }
}
