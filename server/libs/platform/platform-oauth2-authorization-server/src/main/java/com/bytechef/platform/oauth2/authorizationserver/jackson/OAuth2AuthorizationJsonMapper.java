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

import com.bytechef.platform.security.web.authentication.TenantWebAuthenticationDetails;
import com.bytechef.security.web.authentication.TenantUser;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.jackson.SecurityJacksonModules;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

/**
 * @author Ivica Cardic
 */
public final class OAuth2AuthorizationJsonMapper {

    private static final JsonMapper JSON_MAPPER = createJsonMapper();

    private OAuth2AuthorizationJsonMapper() {
    }

    @SuppressFBWarnings(value = "MS_EXPOSE_REP", justification = "Jackson 3 JsonMapper is immutable")
    public static JsonMapper get() {
        return JSON_MAPPER;
    }

    private static JsonMapper createJsonMapper() {
        BasicPolymorphicTypeValidator.Builder typeValidatorBuilder = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType(TenantUser.class)
            .allowIfSubType(TenantWebAuthenticationDetails.class);

        return JsonMapper.builder()
            .addModules(
                SecurityJacksonModules.getModules(
                    OAuth2AuthorizationJsonMapper.class.getClassLoader(), typeValidatorBuilder))
            .addMixIn(TenantUser.class, TenantUserMixin.class)
            .addMixIn(TenantWebAuthenticationDetails.class, TenantWebAuthenticationDetailsMixin.class)
            .build();
    }
}
