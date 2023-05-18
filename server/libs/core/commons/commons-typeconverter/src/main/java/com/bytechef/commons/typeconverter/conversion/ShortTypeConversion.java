
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
 * Convert to a short by parsing the value as a string
 *
 * @author Todd Fast
 */
public class ShortTypeConversion implements TypeConverter.Conversion<Short> {

    @Override
    public Object[] getTypeKeys() {
        return new Object[] {
            Short.class,
            Short.TYPE,
            Short.class.getName(),
            TypeConverter.TYPE_SHORT
        };
    }

    @Override
    public Short convert(Object value) {
        if (value == null) {
            return null;
        }

        if (!(value instanceof Short)) {
            String v = value.toString();

            v = v.trim();

            if (v.length() == 0) {
                value = null;
            } else {
                value = Short.parseShort(v);
            }
        }
        return (Short) value;
    }
}
