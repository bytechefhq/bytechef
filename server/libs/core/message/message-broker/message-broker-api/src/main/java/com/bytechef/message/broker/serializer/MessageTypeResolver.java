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
 * Resolves a message type name received over a message broker into a {@link Class} while enforcing a package allowlist.
 *
 * <p>
 * Broker payloads carry the originating Java class name so the consumer can deserialize them back into the right type.
 * Passing that attacker-influenceable name straight to {@link Class#forName(String)} and then deserializing into the
 * resolved type is a classic polymorphic-deserialization RCE: a forged message can name an arbitrary gadget class.
 * ByteChef only ever publishes its own message types, so resolution is restricted to the {@code com.bytechef.} package.
 *
 * @author Ivica Cardic
 */
public final class MessageTypeResolver {

    private static final String ALLOWED_PACKAGE_PREFIX = "com.bytechef.";

    private MessageTypeResolver() {
    }

    /**
     * Returns {@code true} when the given type name is eligible for resolution (non-null and within the allowlisted
     * package).
     */
    public static boolean isAllowed(String typeName) {
        return typeName != null && typeName.startsWith(ALLOWED_PACKAGE_PREFIX);
    }

    /**
     * Resolves the given type name to a {@link Class}, rejecting any name outside the allowlisted package before it
     * reaches {@link Class#forName(String)}.
     *
     * @throws ClassNotFoundException   if the (allowlisted) class cannot be found
     * @throws IllegalArgumentException if the type name is null or outside the allowlisted package
     */
    public static Class<?> resolve(String typeName) throws ClassNotFoundException {
        if (!isAllowed(typeName)) {
            throw new IllegalArgumentException(
                "Refusing to resolve a message type outside the allowlisted package: " + typeName);
        }

        return Class.forName(typeName);
    }
}
