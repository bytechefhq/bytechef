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

package com.bytechef.component.liferay.constant;

import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;

import com.bytechef.component.definition.ActionDefinition.PropertiesFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableDynamicPropertiesProperty;
import com.bytechef.component.liferay.util.LiferayUtils;

/**
 * @author Nikolina Spehar
 */
public class LiferayConstants {

    public static final String APPLICATION = "application";
    public static final String BODY = "body";
    public static final String CONTEXT_NAME = "contextName";
    public static final String DISCOVER = "discover";
    public static final String ENDPOINT = "endpoint";
    public static final String GET = "GET";
    public static final String HEADER = "header";
    public static final String HIDDEN_PROPERTIES = "hiddenProperties";
    public static final String METHOD = "method";
    public static final String NAME = "name";
    public static final String PATH = "path";
    public static final String PARAMETERS = "parameters";
    public static final String POST = "POST";
    public static final String PROPERTIES = "properties";
    public static final String QUERY = "query";
    public static final String SERVICE = "service";
    public static final String SERVICES = "services";
    public static final String TYPE = "type";

    public static final ModifiableDynamicPropertiesProperty PARAMETERS_DYNAMIC_PROPERTY = dynamicProperties(PARAMETERS)
        .propertiesLookupDependsOn(SERVICE)
        .properties((PropertiesFunction) LiferayUtils::createParameters)
        .required(true);

}
