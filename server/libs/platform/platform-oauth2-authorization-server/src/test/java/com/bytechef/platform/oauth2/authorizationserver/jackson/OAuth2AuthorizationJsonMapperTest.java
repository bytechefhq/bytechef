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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.platform.security.web.authentication.TenantWebAuthenticationDetails;
import com.bytechef.security.web.authentication.TenantUser;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.jackson.SecurityJacksonModules;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class OAuth2AuthorizationJsonMapperTest {

    private static final String REMOTE_ADDRESS = "localhost";
    private static final String TENANT_ID = "000001";

    private static final TypeReference<Map<String, Object>> ATTRIBUTES_TYPE = new TypeReference<>() {};

    @Test
    void testGetReadsBackRememberMeTenantPrincipal() {
        JsonMapper jsonMapper = OAuth2AuthorizationJsonMapper.get();

        String json = jsonMapper.writeValueAsString(createAttributes());

        Map<String, Object> attributes = jsonMapper.readValue(json, ATTRIBUTES_TYPE);

        assertThat(attributes.get(Principal.class.getName()))
            .isInstanceOfSatisfying(RememberMeAuthenticationToken.class, authentication -> {
                assertThat(authentication.getName()).isEqualTo("test1@example.com");
                assertThat(authentication.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_USER");
                assertThat(authentication.getPrincipal())
                    .isInstanceOfSatisfying(TenantUser.class, tenantUser -> {
                        assertThat(tenantUser.getUsername()).isEqualTo("test1@example.com");
                        assertThat(tenantUser.getTenantId()).isEqualTo(TENANT_ID);
                        assertThat(tenantUser.getAuthorities())
                            .extracting(GrantedAuthority::getAuthority)
                            .containsExactly("ROLE_USER");
                    });
                assertThat(authentication.getDetails())
                    .isInstanceOfSatisfying(TenantWebAuthenticationDetails.class, details -> {
                        assertThat(details.getRemoteAddress()).isEqualTo(REMOTE_ADDRESS);
                        assertThat(details.getSessionId()).isEqualTo("session-id");
                        assertThat(details.getTenantId()).isEqualTo(TENANT_ID);
                    });
            });
    }

    @Test
    void testGetReadsBackSingleTenantRememberMePrincipal() {
        JsonMapper jsonMapper = OAuth2AuthorizationJsonMapper.get();

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        User user = new User("admin@example.com", "", authorities);

        user.eraseCredentials();

        RememberMeAuthenticationToken authentication = new RememberMeAuthenticationToken(
            "remember-me-key", user, authorities);

        authentication.setDetails(new TenantWebAuthenticationDetails(REMOTE_ADDRESS, "session-id", null));

        Map<String, Object> attributes = new HashMap<>();

        attributes.put(Principal.class.getName(), authentication);

        String json = jsonMapper.writeValueAsString(Collections.unmodifiableMap(attributes));

        Map<String, Object> readAttributes = jsonMapper.readValue(json, ATTRIBUTES_TYPE);

        assertThat(readAttributes.get(Principal.class.getName()))
            .isInstanceOfSatisfying(RememberMeAuthenticationToken.class, readAuthentication -> {
                assertThat(readAuthentication.getPrincipal())
                    .isExactlyInstanceOf(User.class)
                    .extracting("username")
                    .isEqualTo("admin@example.com");
                assertThat(readAuthentication.getDetails())
                    .isInstanceOfSatisfying(TenantWebAuthenticationDetails.class, details -> {
                        assertThat(details.getRemoteAddress()).isEqualTo(REMOTE_ADDRESS);
                        assertThat(details.getTenantId()).isNull();
                    });
            });
    }

    @Test
    void testGetKeepsErasedCredentialsOfSignedInTenantUser() {
        JsonMapper jsonMapper = OAuth2AuthorizationJsonMapper.get();

        String json = jsonMapper.writeValueAsString(createSignedInTenantUserAttributes());

        Map<String, Object> attributes = jsonMapper.readValue(json, ATTRIBUTES_TYPE);

        assertThat(attributes.get(Principal.class.getName()))
            .isInstanceOfSatisfying(UsernamePasswordAuthenticationToken.class, authentication -> assertThat(
                authentication.getPrincipal())
                    .isInstanceOfSatisfying(TenantUser.class, tenantUser -> {
                        assertThat(tenantUser.getPassword()).isNull();
                        assertThat(tenantUser.getTenantId()).isEqualTo(TENANT_ID);
                    }));
    }

    @Test
    void testGetRejectsTenantUserWithoutTenantId() {
        JsonMapper jsonMapper = OAuth2AuthorizationJsonMapper.get();

        String json = jsonMapper.writeValueAsString(createSignedInTenantUserAttributes());

        String jsonWithoutTenantId = json.replace(",\"tenantId\":\"" + TENANT_ID + "\"", "");

        assertThat(jsonWithoutTenantId).isNotEqualTo(json);
        assertThatThrownBy(() -> jsonMapper.readValue(jsonWithoutTenantId, ATTRIBUTES_TYPE))
            .isInstanceOf(JacksonException.class)
            .hasMessageContaining("tenantId");
    }

    @Test
    void testSecurityJacksonModulesAloneRejectTenantPrincipal() {
        JsonMapper jsonMapper = JsonMapper.builder()
            .addModules(SecurityJacksonModules.getModules(getClass().getClassLoader()))
            .build();

        assertThatThrownBy(() -> jsonMapper.readValue(
            OAuth2AuthorizationJsonMapper.get()
                .writeValueAsString(createAttributes()),
            ATTRIBUTES_TYPE))
                .isInstanceOf(JacksonException.class);
    }

    private static Map<String, Object> createAttributes() {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        TenantUser tenantUser = new TenantUser("test1@example.com", "", authorities, TENANT_ID);

        RememberMeAuthenticationToken authentication = new RememberMeAuthenticationToken(
            "remember-me-key", tenantUser, authorities);

        authentication.setDetails(new TenantWebAuthenticationDetails(REMOTE_ADDRESS, "session-id", TENANT_ID));

        Map<String, Object> attributes = new HashMap<>();

        attributes.put(Principal.class.getName(), authentication);

        return Collections.unmodifiableMap(attributes);
    }

    private static Map<String, Object> createSignedInTenantUserAttributes() {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        TenantUser tenantUser = new TenantUser("test1@example.com", "password-hash", authorities, TENANT_ID);

        tenantUser.eraseCredentials();

        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
            tenantUser, null, authorities);

        Map<String, Object> attributes = new HashMap<>();

        attributes.put(Principal.class.getName(), authentication);

        return Collections.unmodifiableMap(attributes);
    }
}
