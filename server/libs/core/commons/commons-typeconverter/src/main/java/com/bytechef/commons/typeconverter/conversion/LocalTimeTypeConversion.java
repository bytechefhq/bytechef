
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

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Convert to a {@link LocalTime} by parsing a value as a string of form {@link DateTimeFormatter.ISO_LOCAL_TIME}.
 *
 * @author Ivica Cardic
 */
public class LocalTimeTypeConversion implements TypeConverter.Conversion<LocalTime> {

    @Override
    public Object[] getTypeKeys() {
        return new Object[] {
            LocalTime.class,
            LocalTime.class.getName(),
            TypeConverter.TYPE_LOCAL_TIME
        };
    }

    @Override
    public LocalTime convert(Object value, Object typeKey) {
        if (value == null) {
            return null;
        }

        if (!(value instanceof LocalTime)) {
            String v = value.toString();

            v = v.trim();

            if (v.length() == 0) {
                value = null;
            } else {
                value = LocalTime.parse((String) value, DateTimeFormatter.ISO_LOCAL_TIME);
            }
        }
        return (LocalTime) value;
    }
}
