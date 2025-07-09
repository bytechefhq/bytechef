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

package com.bytechef.component.amplitude.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableArrayProperty;

/**
 * @author Nikolina Spehar
 */
public class AmplitudeConstants {

    public static final String API_KEY = "api_key";
    public static final String CARRIER = "carrier";
    public static final String CITY = "city";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_TYPE_URLENCODED = "application/x-www-urlencoded";
    public static final String COUNTRY = "country";
    public static final String DEVICE_BRAND = "device_brand";
    public static final String DEVICE_ID = "device_id";
    public static final String DMA = "dma";
    public static final String EVENT = "event";
    public static final String EVENT_TYPE = "event_type";
    public static final String ID = "id";
    public static final String IDENTIFICATION = "identification";
    public static final String IDENTIFIER = "identifier";
    public static final String KEY = "key";
    public static final String LANGUAGE = "language";
    public static final String OS_NAME = "os_name";
    public static final String PLATFORM = "platform";
    public static final String REGION = "region";
    public static final String USER_ID = "user_id";
    public static final String USER_PROPERTIES = "user_properties";
    public static final String VALUE = "value";

    public static final ModifiableArrayProperty USER_PROPERTIES_OBJECT = array(USER_PROPERTIES)
        .label("User Properties")
        .description("A dictionary of attribution properties prefixed with brackets [YOUR COMPANY].")
        .required(false)
        .items(
            object("property")
                .label("Property")
                .properties(
                    string(KEY)
                        .label("Key")
                        .description("The property key.")
                        .required(true),
                    string(VALUE)
                        .label("Value")
                        .description("The property value.")
                        .required(true)));

    private AmplitudeConstants() {
    }
}
