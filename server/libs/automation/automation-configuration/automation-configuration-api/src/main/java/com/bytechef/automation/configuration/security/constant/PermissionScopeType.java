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

package com.bytechef.automation.configuration.security.constant;

/**
 * Marker interface for permission scope enums. Lets
 * {@link com.bytechef.automation.configuration.service.PermissionService} accept typed scope arguments without taking a
 * compile-time dependency on the EE enum
 * {@code com.bytechef.ee.automation.configuration.security.constant.PermissionScope}. The EE enum implements this
 * interface; {@link Enum#name()} satisfies the contract automatically.
 *
 * <p>
 * Tightening from {@code Enum<?>} to this marker prevents callers from passing an unrelated enum (e.g.,
 * {@code Thread.State.RUNNABLE}), which would have compiled and silently returned {@code false} from a permission
 * check.
 *
 * @author Ivica Cardic
 */
public interface PermissionScopeType {

    String name();
}
