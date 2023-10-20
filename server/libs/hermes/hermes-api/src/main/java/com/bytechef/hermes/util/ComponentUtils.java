
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

package com.bytechef.hermes.util;

/**
 * @author Ivica Cardic
 */
public class ComponentUtils {

    public static ComponentType getComponentType(String type) {
        String[] typeItems = type.split("/");

        return new ComponentType(typeItems[0], Integer.parseInt(typeItems[1].replace("v", "")), typeItems[2]);
    }

    public record ComponentType(String componentName, int componentVersion, String operationName) {
    }
}
