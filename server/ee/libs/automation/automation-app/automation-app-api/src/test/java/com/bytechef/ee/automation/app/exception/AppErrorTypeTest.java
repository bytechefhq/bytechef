/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class AppErrorTypeTest {

    @Test
    void testNameOfNamesEveryConstant() throws IllegalAccessException {
        int constantCount = 0;

        for (Field field : AppErrorType.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == AppErrorType.class) {
                AppErrorType errorType = (AppErrorType) field.get(null);

                assertThat(AppErrorType.nameOf(errorType.getErrorKey())).isEqualTo(field.getName());

                constantCount++;
            }
        }

        assertThat(constantCount).isEqualTo(29);
    }

    @Test
    void testNameOfUnknownKeyIsNull() {
        assertThat(AppErrorType.nameOf(999)).isNull();
    }
}
