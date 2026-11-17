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

package com.bytechef.platform.oauth2.authorizationserver.jackson;

import com.bytechef.security.web.authentication.TenantUser;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.node.MissingNode;
import tools.jackson.databind.type.TypeFactory;

/**
 * @author Ivica Cardic
 */
class TenantUserDeserializer extends ValueDeserializer<TenantUser> {

    private static final TypeReference<Set<GrantedAuthority>> GRANTED_AUTHORITY_SET = new TypeReference<>() {};

    @Override
    public TenantUser deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
        throws JacksonException {

        JsonNode jsonNode = deserializationContext.readTree(jsonParser);

        TypeFactory typeFactory = deserializationContext.getTypeFactory();

        JavaType grantedAuthoritySetType = typeFactory.constructType(GRANTED_AUTHORITY_SET);

        Set<GrantedAuthority> authorities = deserializationContext.readTreeAsValue(
            readJsonNode(jsonNode, "authorities"), grantedAuthoritySetType);

        if (authorities == null) {
            return deserializationContext.reportInputMismatch(
                TenantUser.class, "TenantUser is missing its authorities");
        }

        String username = readRequiredString(deserializationContext, jsonNode, "username");
        String tenantId = readRequiredString(deserializationContext, jsonNode, "tenantId");
        String password = readOptionalString(jsonNode, "password");

        TenantUser tenantUser = new TenantUser(username, password == null ? "" : password, authorities, tenantId);

        if (password == null) {
            tenantUser.eraseCredentials();
        }

        return tenantUser;
    }

    private static JsonNode readJsonNode(JsonNode jsonNode, String field) {
        return jsonNode.has(field) ? jsonNode.get(field) : MissingNode.getInstance();
    }

    private static @Nullable String readOptionalString(JsonNode jsonNode, String field) {
        JsonNode fieldNode = readJsonNode(jsonNode, field);

        return fieldNode.isString() ? fieldNode.stringValue() : null;
    }

    private static String readRequiredString(
        DeserializationContext deserializationContext, JsonNode jsonNode, String field) {

        String value = readOptionalString(jsonNode, field);

        if (value == null || value.isBlank()) {
            return deserializationContext.reportInputMismatch(
                TenantUser.class, "TenantUser is missing a non-blank '%s'", field);
        }

        return value;
    }
}
