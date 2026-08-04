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

package com.bytechef.platform.component.visibility;

/**
 * Extension point for administrative component visibility. Implementations decide whether a component is visible
 * (enabled) for the current tenant. CE ships no implementation, so all components are visible; EE ships a
 * persistence-backed implementation. Consumed on component listing and before action/trigger execution.
 *
 * @author Ivica Cardic
 */
public interface ComponentVisibilityProvider {

    /**
     * @return {@code true} if the component is visible/enabled for the current tenant.
     */
    boolean isVisible(String componentName);

    /**
     * @return {@code true} if the action is visible/enabled for the current tenant. A disabled component implies all
     *         its actions are invisible.
     */
    default boolean isActionVisible(String componentName, String actionName) {
        return isVisible(componentName);
    }

    /**
     * @return {@code true} if the trigger is visible/enabled for the current tenant. A disabled component implies all
     *         its triggers are invisible.
     */
    default boolean isTriggerVisible(String componentName, String triggerName) {
        return isVisible(componentName);
    }
}
