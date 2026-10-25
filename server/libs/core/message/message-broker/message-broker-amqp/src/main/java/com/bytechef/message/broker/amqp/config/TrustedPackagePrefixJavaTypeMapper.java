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

package com.bytechef.message.broker.amqp.config;

import java.util.List;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import tools.jackson.databind.JavaType;

/**
 * Trusts message types by package prefix. Spring AMQP matches trusted packages exactly, while ByteChef messages live in
 * many {@code com.bytechef} sub-packages.
 *
 * @author Ivica Cardic
 */
class TrustedPackagePrefixJavaTypeMapper extends DefaultJacksonJavaTypeMapper {

    private static final List<String> TRUSTED_PACKAGE_PREFIXES = List.of("com.bytechef.", "java.lang.", "java.util.");

    TrustedPackagePrefixJavaTypeMapper() {
        setTrustedPackages("*");
    }

    @Override
    public JavaType toJavaType(MessageProperties properties) {
        for (String headerName : List.of(
            getClassIdFieldName(), getContentClassIdFieldName(), getKeyClassIdFieldName())) {

            String typeId = retrieveHeaderAsString(properties, headerName);

            if (typeId != null && !isTrusted(typeId)) {
                throw new IllegalArgumentException(
                    "The class '%s' is not in the trusted packages: %s".formatted(typeId, TRUSTED_PACKAGE_PREFIXES));
            }
        }

        return super.toJavaType(properties);
    }

    private static boolean isTrusted(String typeId) {
        String className = typeId.replaceFirst("^\\[L", "");

        return TRUSTED_PACKAGE_PREFIXES.stream()
            .anyMatch(className::startsWith);
    }
}
