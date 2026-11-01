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

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.security.web.config.OAuth2LoginCustomizer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * @author Ivica Cardic
 */
class SingleTenantOAuth2LoginCustomizerTest {

    @Test
    void testSocialLoginIsSkippedWhenLoginDisabled() {
        new ApplicationContextRunner()
            .withPropertyValues(
                "bytechef.security.login.enabled=false", "bytechef.security.social-login.enabled=true",
                "bytechef.security.sso.enabled=false", "bytechef.tenant.mode=single")
            .withUserConfiguration(
                CustomOAuth2UserService.class, SingleTenantOAuth2LoginCustomizer.class, SocialOidcUserService.class)
            .run(context -> assertThat(context)
                .hasNotFailed()
                .doesNotHaveBean(OAuth2LoginCustomizer.class));
    }
}
