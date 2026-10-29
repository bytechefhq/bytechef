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

package com.bytechef.automation.configuration.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ProjectErrorTypeTest {

    @Test
    void testEveryProjectErrorTypeHasItsOwnErrorKey() {
        List<Integer> errorKeys = Arrays.stream(ProjectErrorType.class.getDeclaredFields())
            .filter(field -> Modifier.isStatic(field.getModifiers()) && field.getType() == ProjectErrorType.class)
            .map(ProjectErrorTypeTest::readErrorKey)
            .toList();

        assertThat(errorKeys).hasSizeGreaterThanOrEqualTo(12)
            .doesNotHaveDuplicates();
    }

    @Test
    void testTheProjectVersionAndToolErrorTypesShareOneClass() {
        assertThat(ProjectErrorType.PROJECT_VERSION_NOT_FOUND.getErrorKey()).isEqualTo(100);
        assertThat(ProjectErrorType.CREATE_PROJECT.getErrorKey()).isEqualTo(104);
    }

    private static int readErrorKey(Field field) {
        try {
            return ((ProjectErrorType) field.get(null)).getErrorKey();
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
