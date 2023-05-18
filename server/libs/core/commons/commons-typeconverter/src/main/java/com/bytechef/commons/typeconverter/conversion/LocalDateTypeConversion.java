
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Convert to a {@link LocalDate} by parsing a value as a string of form {@link DateTimeFormatter.ISO_LOCAL_DATE}.
 *
 * @author Ivica Cardic
 */
public class LocalDateTypeConversion implements TypeConverter.Conversion<LocalDate> {

    @Override
    public Object[] getTypeKeys() {
        return new Object[] {
            LocalDate.class,
            LocalDate.class.getName(),
            TypeConverter.TYPE_LOCAL_DATE
        };
    }

    @Override
    public LocalDate convert(Object value) {
        if (value == null) {
            return null;
        }

        if (!(value instanceof LocalDate)) {
            String v = value.toString();

            v = v.trim();

            if (v.length() == 0) {
                value = null;
            } else {
                value = LocalDate.parse((String) value, DateTimeFormatter.ISO_LOCAL_DATE);
            }
        }
        return (LocalDate) value;
    }
}
