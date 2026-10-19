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

package com.bytechef.component.logger.util;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * @author Ivica Cardic
 */
public class LoggerUtils {

    private LoggerUtils() {
    }

    public static String toMessage(Object text) {
        if (text == null) {
            return "";
        }

        if (text instanceof Object[] objects) {
            return Arrays.deepToString(objects);
        }

        Class<?> textClass = text.getClass();

        if (textClass.isArray()) {
            Object[] objects = new Object[Array.getLength(text)];

            for (int index = 0; index < objects.length; index++) {
                objects[index] = Array.get(text, index);
            }

            return Arrays.toString(objects);
        }

        return String.valueOf(text);
    }
}
