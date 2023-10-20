
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

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

/**
 * Converts a byte array to an object via deserialization, or returns the value as-is
 *
 * @author Todd Fast
 */
public class ObjectTypeConversion implements TypeConverter.Conversion<Object> {

    @Override
    public Object[] getTypeKeys() {
        return new Object[] {
            Object.class,
            Object.class.getName(),
            TypeConverter.TYPE_OBJECT
        };
    }

    @Override
    public Object convert(Object value) {
        if (value == null) {
            return null;
        }

        Class<?> valueClass = value.getClass();

        if (valueClass.isArray()) {
            // This is a byte array; presume we can convert it to an object
            if (valueClass.getComponentType() == Byte.TYPE) {
                try (ByteArrayInputStream bis = new ByteArrayInputStream((byte[]) value);
                    ObjectInputStream ois = new ObjectInputStream(bis)) {

                    value = ois.readObject();
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                        "Could not deserialize object", e);
                }
            } else {
                ; // value is OK as is
            }
        }

        return value;
    }
}
