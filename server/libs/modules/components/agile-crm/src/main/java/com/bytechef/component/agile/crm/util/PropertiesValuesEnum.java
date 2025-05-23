/*
 * Copyright 2025 ByteChef
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

package com.bytechef.component.agile.crm.util;

import com.bytechef.component.agile.crm.constant.AgileCrmConstants;

/**
 * @author Nikolina Spehar
 */
public enum PropertiesValuesEnum {

    ADDRESS(AgileCrmConstants.ADDRESS),
    COMPANY(AgileCrmConstants.COMPANY),
    EMAIL(AgileCrmConstants.EMAIL),
    FIRST_NAME(AgileCrmConstants.FIRST_NAME),
    LAST_NAME(AgileCrmConstants.LAST_NAME),
    PHONE(AgileCrmConstants.PHONE),
    WEBSITE(AgileCrmConstants.WEBSITE),
    CITY(AgileCrmConstants.CITY),
    STATE(AgileCrmConstants.STATE),
    COUNTRY(AgileCrmConstants.COUNTRY),
    ZIP_CODE(AgileCrmConstants.ZIP_CODE),;

    private final String value;

    PropertiesValuesEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
