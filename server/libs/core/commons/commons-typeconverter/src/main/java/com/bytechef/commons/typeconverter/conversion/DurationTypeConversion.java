
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

import java.time.Duration;

/**
 * Convert to a {@link Duration} by parsing a value as a string of form <code>PT[value]</code>.
 *
 * @author Ivica Cardic
 */
public class DurationTypeConversion implements TypeConverter.Conversion<Duration> {

    @Override
    public Object[] getTypeKeys() {
        return new Object[] {
            Duration.class,
            Duration.class.getName(),
            TypeConverter.TYPE_DURATION
        };
    }

    @Override
    public Duration convert(Object value, Object typeKey) {
        if (value == null) {
            return null;
        }

        if (!(value instanceof Duration)) {
            String v = value.toString();

            v = v.trim();

            if (v.length() == 0) {
                value = null;
            } else {
                value = Duration.parse("PT" + value);
            }
        }
        return (Duration) value;
    }
}
