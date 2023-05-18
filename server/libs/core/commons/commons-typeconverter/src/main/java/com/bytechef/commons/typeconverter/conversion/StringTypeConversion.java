
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.commons.typeconverter.conversion;

import com.bytechef.commons.typeconverter.TypeConverter;

import java.nio.charset.StandardCharsets;

/**
 * Converts the value to a string. If the value is a byte or char array, it is converted to a string via
 * {@link toString()}.
 *
 * @author Todd Fast
 */
public class StringTypeConversion implements TypeConverter.Conversion<String> {

    @Override
    public Object[] getTypeKeys() {
        return new Object[] {
            String.class,
            String.class.getName(),
            TypeConverter.TYPE_STRING
        };
    }

    @Override
    public String convert(Object value, Object typeKey) {
        if (value == null) {
            return null;
        }

        Class<?> valueClass = value.getClass();

        if (valueClass.isArray()) {
            // This is a byte array; we can convert it to a string
            if (valueClass.getComponentType() == Byte.TYPE) {
                value = new String((byte[]) value, StandardCharsets.UTF_8);
            } else if (valueClass.getComponentType() == Character.TYPE) {
                value = new String((char[]) value);
            }
        } else if (!(value instanceof String)) {
            value = value.toString();
        }

        return (String) value;
    }
}
