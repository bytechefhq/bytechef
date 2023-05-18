
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

/**
 * Convert to a character by parsing the first character of the value as a string
 *
 * @author Todd Fast
 */
public class CharacterTypeConversion implements TypeConverter.Conversion<Character> {

    @Override
    public Object[] getTypeKeys() {
        return new Object[] {
            Character.class,
            Character.TYPE,
            Character.class.getName(),
            TypeConverter.TYPE_CHAR,
            TypeConverter.TYPE_CHARACTER,
        };
    }

    @Override
    public Character convert(Object value, Object typeKey) {
        if (value == null) {
            return null;
        }

        if (!(value instanceof Character)) {
            String v = value.toString();

            v = v.trim();

            if (v.length() == 0) {
                value = null;
            } else {
                value = v.charAt(0);
            }
        }

        return (Character) value;
    }
}
