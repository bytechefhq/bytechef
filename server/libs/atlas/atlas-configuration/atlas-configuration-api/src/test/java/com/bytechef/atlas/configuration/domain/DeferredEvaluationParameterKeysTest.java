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

package com.bytechef.atlas.configuration.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DeferredEvaluationParameterKeysTest {

    @Test
    void testForTaskTypeReturnsRegisteredKeys() {
        DeferredEvaluationParameterKeys.register("testType/", "keyA", "keyB");

        Set<String> result = DeferredEvaluationParameterKeys.forTaskType("testType/v1");

        assertEquals(Set.of("keyA", "keyB"), result);
    }

    @Test
    void testForTaskTypeReturnsEmptySetForUnregisteredType() {
        Set<String> result = DeferredEvaluationParameterKeys.forTaskType("unknownType/v1");

        assertTrue(result.isEmpty());
    }

    @Test
    void testForTaskTypeReturnsEmptySetForNullType() {
        Set<String> result = DeferredEvaluationParameterKeys.forTaskType(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void testRegisterOverwritesPreviousRegistration() {
        DeferredEvaluationParameterKeys.register("overwrite/", "oldKey");
        DeferredEvaluationParameterKeys.register("overwrite/", "newKey");

        Set<String> result = DeferredEvaluationParameterKeys.forTaskType("overwrite/v1");

        assertEquals(Set.of("newKey"), result);
    }

    @Test
    void testForTaskTypeMatchesByPrefix() {
        DeferredEvaluationParameterKeys.register("prefix/", "prefixKey");

        assertEquals(Set.of("prefixKey"), DeferredEvaluationParameterKeys.forTaskType("prefix/v1"));
        assertEquals(Set.of("prefixKey"), DeferredEvaluationParameterKeys.forTaskType("prefix/v2"));
        assertTrue(DeferredEvaluationParameterKeys.forTaskType("otherprefix/v1")
            .isEmpty());
    }

}
