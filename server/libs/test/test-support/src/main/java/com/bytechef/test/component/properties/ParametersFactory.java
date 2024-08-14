/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.test.component.properties;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.Parameters;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.lang.reflect.Constructor;
import java.util.Map;

/**
 * @author Igor Beslic
 */
public class ParametersFactory {

    public static Parameters createParameters(Map<String, Object> map) {
        try {
            return (Parameters) parametersImplConstructor.newInstance(map);
        } catch (Exception exception) {
            throw new RuntimeException("Unable to instantiate Parameters", exception);
        }
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper() {
        {
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            registerModule(new JavaTimeModule());
            registerModule(new Jdk8Module());
        }
    };

    static {
        MapUtils.initObjectMapper(OBJECT_MAPPER);
    }

    private static final Class<?> parametersImplClass;
    private static final Constructor<?> parametersImplConstructor;

    static {
        try {
            parametersImplClass = Class.forName("com.bytechef.platform.component.registry.definition.ParametersImpl");

            parametersImplConstructor = parametersImplClass.getConstructor(Map.class);
        } catch (ClassNotFoundException | NoSuchMethodException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

}
