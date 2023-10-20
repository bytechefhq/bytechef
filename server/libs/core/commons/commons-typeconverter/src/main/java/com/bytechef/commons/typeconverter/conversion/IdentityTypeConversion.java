
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
 * Returns the value as-is (no conversion)
 *
 * @author Todd Fast
 */
public class IdentityTypeConversion
    implements TypeConverter.Conversion<Object> {

    @Override
    public Object[] getTypeKeys() {
        return new Object[] {};
    }

    @Override
    public Object convert(Object value, Object typeKey) {
        return value;
    }
}
