
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

package com.bytechef.commons.typeconverter;

/**
 * @author Todd Fast
 */
public class TestSubclass extends TestSuperclass
    implements TypeConverter.Listener, TypeConverter.Convertible {

    public void beforeConversion(Object targetTypeKey) {
        System.out.println("--- beforeConversion(" + targetTypeKey + ")");
    }

    public Object afterConversion(Object targetTypeKey, Object convertedValue) {
        System.out.println("--- afterConversion(" +
            targetTypeKey + "," + convertedValue + ")");
        return convertedValue;
    }

    public TypeConverter.Conversion getTypeConversion(final Object targetTypeKey) {
        return new TypeConverter.Conversion() {

            @Override
            public Object[] getTypeKeys() {
                return new Object[] {
                    TestSubclass.class
                };
            }

            @Override
            public Object convert(Object value, Object typeKey) {
                System.out.println("--- Converting value to type \"" + key +
                    "\": " + value);
                return "Converted Test value";
            }

            private Object key = targetTypeKey;
        };
    }
}
