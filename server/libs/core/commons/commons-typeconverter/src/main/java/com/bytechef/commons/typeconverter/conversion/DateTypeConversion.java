
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Convert to a {@link Date} by parsing a value as a string of form <code>yyyy-MM-dd'T'HH:mm:ss.SSSZ</code>.
 *
 * @see SimpleDateFormat#parse(String)
 *
 * @author Ivica Cardic
 */
public class DateTypeConversion implements TypeConverter.Conversion<Date> {

    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    @Override
    public Object[] getTypeKeys() {
        return new Object[] {
            Date.class,
            Date.class.getName(),
            TypeConverter.TYPE_DATE
        };
    }

    @Override
    public Date convert(Object value) {
        if (value == null) {
            return null;
        }

        if (!(value instanceof Date)) {
            String v = value.toString();

            v = v.trim();

            if (v.length() == 0) {
                value = null;
            } else {
                SimpleDateFormat dateFormat = new SimpleDateFormat(TIMESTAMP_FORMAT);

                try {
                    value = dateFormat.parse((String) value);
                } catch (ParseException parseException) {
                    throw new RuntimeException(parseException);
                }
            }
        }
        return (Date) value;
    }
}
