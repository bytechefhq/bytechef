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

package com.bytechef.platform.component.util;

import com.bytechef.component.definition.Authorization;
import java.util.Objects;

/**
 * @author Igor Beslic
 */
public class AuthorizationUtils {

    /**
     * Returns true if value converts to valid authorisation type enumeration. NONE is not valid type as it should
     * signal absence of authorisation mechanism.
     *
     * @param value authorisation name
     * @return false if value converts to NONE or value does not mach any of enumeration names, otherwise returns true
     */
    public static boolean isApplicable(String value) {
        if (value == null) {
            return false;
        }

        for (Authorization.AuthorizationType authorizationType : Authorization.AuthorizationType.values()) {
            String name = authorizationType.getName();

            if ((authorizationType != Authorization.AuthorizationType.NONE) &&
                Objects.equals(value.toLowerCase(), name.toLowerCase())) {

                return true;
            }
        }

        return false;
    }
}
