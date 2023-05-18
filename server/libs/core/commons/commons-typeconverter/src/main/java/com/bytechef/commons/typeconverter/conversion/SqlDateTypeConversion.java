
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

import java.sql.Date;

/**
 * Convert to a {@link SqlDate} by parsing a value as a string of form <code>yyyy-[m]m-[d]d</code>.
 *
 * @see java.sql.Date#valueOf(String)
 *
 * @author Todd Fast
 */
public class SqlDateTypeConversion implements TypeConverter.Conversion<Date> {

    @Override
    public Object[] getTypeKeys() {
        return new Object[] {
            java.sql.Date.class,
            java.sql.Date.class.getName(),
            TypeConverter.TYPE_SQL_DATE
        };
    }

    @Override
    public Date convert(Object value) {
        if (value == null) {
            return null;
        }

        if (!(value instanceof java.sql.Date)) {
            String v = value.toString();

            v = v.trim();

            if (v.length() == 0) {
                value = null;
            } else {
                // Value must be in the "yyyy-mm-dd" format
                value = Date.valueOf(v);
            }
        }
        return (Date) value;
    }
}
