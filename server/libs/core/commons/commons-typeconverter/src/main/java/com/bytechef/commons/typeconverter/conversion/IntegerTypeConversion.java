
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
 * Convert to an integer by parsing the value as a string
 *
 * @author Todd Fast
 */
public class IntegerTypeConversion implements TypeConverter.Conversion<Integer> {

    @Override
    public Object[] getTypeKeys() {
        return new Object[] {
            Integer.class,
            Integer.TYPE,
            Integer.class.getName(),
            TypeConverter.TYPE_INT,
            TypeConverter.TYPE_INTEGER
        };
    }

    @Override
    public Integer convert(Object value) {
        if (value == null) {
            return null;
        }

        if (!(value instanceof Integer)) {
            String v = value.toString();

            v = v.trim();

            if (v.length() == 0) {
                value = null;
            } else {
                value = Integer.parseInt(v);
            }
        }

        return (Integer) value;
    }
}
