
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
 * Convert to a double by parsing the value as a string
 *
 * @author Todd Fast
 */
public class DoubleTypeConversion implements TypeConverter.Conversion<Double> {

    @Override
    public Object[] getTypeKeys() {
        return new Object[] {
            Double.class,
            Double.TYPE,
            Double.class.getName(),
            TypeConverter.TYPE_DOUBLE
        };
    }

    @Override
    public Double convert(Object value, Object typeKey) {
        if (value == null) {
            return null;
        }

        if (!(value instanceof Double)) {
            String v = value.toString();

            v = v.trim();

            if (v.length() == 0) {
                value = null;
            } else {
                value = Double.parseDouble(v);
            }
        }

        return (Double) value;
    }
}
