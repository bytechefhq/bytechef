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

package com.bytechef.message.broker.serializer;

/**
 * @author Ivica Cardic
 */
public final class MessageTypeResolver {

    public static final String ALLOWED_PACKAGE = "com.bytechef";

    private static final String ALLOWED_PACKAGE_PREFIX = ALLOWED_PACKAGE + ".";

    private MessageTypeResolver() {
    }

    public static boolean isAllowed(String typeName) {
        return typeName != null && typeName.startsWith(ALLOWED_PACKAGE_PREFIX);
    }

    public static Class<?> resolve(String typeName) throws ClassNotFoundException {
        if (!isAllowed(typeName)) {
            throw new IllegalArgumentException(
                "Refusing to resolve a message type outside the allowlisted package: " + typeName);
        }

        return Class.forName(typeName);
    }
}
