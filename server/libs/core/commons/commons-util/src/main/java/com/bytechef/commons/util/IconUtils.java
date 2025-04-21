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

package com.bytechef.commons.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author Ivica Cardic
 */
public class IconUtils {

    public static String readIcon(String icon) {
        if (icon != null && icon.startsWith("path:")) {
            ClassLoader classLoader = IconUtils.class.getClassLoader();

            try (InputStream inputStream = classLoader.getResourceAsStream(icon.replace("path:", ""))) {
                if (inputStream != null) {
                    icon = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return icon;
    }
}
