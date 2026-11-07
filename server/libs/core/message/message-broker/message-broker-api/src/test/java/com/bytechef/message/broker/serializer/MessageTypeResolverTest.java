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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class MessageTypeResolverTest {

    @Test
    void testIsAllowed() {
        assertTrue(MessageTypeResolver.isAllowed("com.bytechef.message.event.SomeEvent"));
        assertFalse(MessageTypeResolver.isAllowed(null));
        assertFalse(MessageTypeResolver.isAllowed("java.lang.Runtime"));
        assertFalse(
            MessageTypeResolver.isAllowed("org.springframework.context.support.ClassPathXmlApplicationContext"));
        assertFalse(MessageTypeResolver.isAllowed("com.bytechefhq.evil.Gadget"));
    }

    @Test
    void testResolveAllowsBytechefType() throws ClassNotFoundException {
        assertEquals(MessageTypeResolver.class, MessageTypeResolver.resolve(MessageTypeResolver.class.getName()));
    }

    @Test
    void testResolveRejectsNonAllowlistedType() {
        assertThrows(IllegalArgumentException.class, () -> MessageTypeResolver.resolve("java.lang.Runtime"));
        assertThrows(
            IllegalArgumentException.class,
            () -> MessageTypeResolver.resolve(
                "org.springframework.context.support.ClassPathXmlApplicationContext"));
        assertThrows(IllegalArgumentException.class, () -> MessageTypeResolver.resolve(null));
    }
}
