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

package com.bytechef.security.web.oauth2;

import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Custom OAuth2UserService that integrates OAuth2 provider users with the internal ByteChef user model. On each OAuth2
 * login, this service finds or creates the corresponding internal user and returns a {@link CustomOAuth2User} whose
 * {@code getName()} returns the internal login — required for remember-me compatibility.
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnProperty(prefix = "bytechef.security.social-login", name = "enabled", havingValue = "true")
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final String GITHUB_EMAILS_URL = "https://api.github.com/user/emails";

    private final AuthorityService authorityService;
    private final RestTemplate restTemplate;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public CustomOAuth2UserService(AuthorityService authorityService, UserService userService) {
        this.authorityService = authorityService;
        this.restTemplate = new RestTemplate();
        this.userService = userService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration()
            .getRegistrationId()
            .toUpperCase();

        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = extractEmail(registrationId, attributes, userRequest);
        String firstName = extractFirstName(registrationId, attributes);
        String lastName = extractLastName(registrationId, attributes);
        String imageUrl = extractImageUrl(registrationId, attributes);
        String providerId = extractProviderId(registrationId, attributes);

        if (email == null) {
            throw new OAuth2AuthenticationException("Email not available from OAuth2 provider " + registrationId);
        }

        User user = userService.findOrCreateSocialUser(
            email, firstName, lastName, imageUrl, registrationId, providerId);

        List<SimpleGrantedAuthority> grantedAuthorities = user.getAuthorityIds()
            .stream()
            .map(authorityService::fetchAuthority)
            .map(Optional::get)
            .map(Authority::getName)
            .map(SimpleGrantedAuthority::new)
            .toList();

        return new CustomOAuth2User(user.getLogin(), grantedAuthorities, attributes);
    }

    private String extractEmail(
        String registrationId, Map<String, Object> attributes, OAuth2UserRequest userRequest) {

        if ("GOOGLE".equals(registrationId)) {
            return (String) attributes.get("email");
        }

        if ("GITHUB".equals(registrationId)) {
            String email = (String) attributes.get("email");

            if (email == null) {
                email = fetchGitHubPrimaryEmail(userRequest);
            }

            return email;
        }

        return (String) attributes.get("email");
    }

    private String extractFirstName(String registrationId, Map<String, Object> attributes) {
        if ("GOOGLE".equals(registrationId)) {
            return (String) attributes.get("given_name");
        }

        if ("GITHUB".equals(registrationId)) {
            String name = (String) attributes.get("name");

            if (name != null && name.contains(" ")) {
                return name.substring(0, name.indexOf(' '));
            }

            return name;
        }

        return (String) attributes.get("given_name");
    }

    private String extractImageUrl(String registrationId, Map<String, Object> attributes) {
        if ("GOOGLE".equals(registrationId)) {
            return (String) attributes.get("picture");
        }

        if ("GITHUB".equals(registrationId)) {
            return (String) attributes.get("avatar_url");
        }

        return (String) attributes.get("picture");
    }

    private String extractLastName(String registrationId, Map<String, Object> attributes) {
        if ("GOOGLE".equals(registrationId)) {
            return (String) attributes.get("family_name");
        }

        if ("GITHUB".equals(registrationId)) {
            String name = (String) attributes.get("name");

            if (name != null && name.contains(" ")) {
                return name.substring(name.indexOf(' ') + 1);
            }

            return null;
        }

        return (String) attributes.get("family_name");
    }

    private String extractProviderId(String registrationId, Map<String, Object> attributes) {
        if ("GOOGLE".equals(registrationId)) {
            return (String) attributes.get("sub");
        }

        if ("GITHUB".equals(registrationId)) {
            Object githubId = attributes.get("id");

            return githubId != null ? githubId.toString() : null;
        }

        return (String) attributes.get("sub");
    }

    private String fetchGitHubPrimaryEmail(OAuth2UserRequest userRequest) {
        String accessToken = userRequest.getAccessToken()
            .getTokenValue();

        HttpHeaders headers = new HttpHeaders();

        headers.setBearerAuth(accessToken);

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
            GITHUB_EMAILS_URL, HttpMethod.GET, new HttpEntity<>(headers),
            new ParameterizedTypeReference<>() {});

        List<Map<String, Object>> emails = response.getBody();

        if (emails == null) {
            return null;
        }

        for (Map<String, Object> emailEntry : emails) {
            Boolean primary = (Boolean) emailEntry.get("primary");
            Boolean verified = (Boolean) emailEntry.get("verified");

            if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified)) {
                return (String) emailEntry.get("email");
            }
        }

        return null;
    }
}
