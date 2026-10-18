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

package com.bytechef.config.actuate.info;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Security;
import com.bytechef.config.ApplicationProperties.Security.SocialLogin;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.Info;

/**
 * @author Ivica Cardic
 */
class SocialLoginProvidersContributorTest {

    @Test
    void testContributesOnlyProvidersWithClientId() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.setSecurity(new Security());

        SocialLogin socialLogin = applicationProperties.getSecurity()
            .getSocialLogin();

        socialLogin.setEnabled(true);
        socialLogin.getGoogle()
            .setClientId("google-client");

        assertThat(contribute(applicationProperties).get("socialLoginProviders")).asList()
            .containsExactly("google");
    }

    @Test
    void testContributesNoProvidersWhenSocialLoginIsDisabled() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.setSecurity(new Security());

        SocialLogin socialLogin = applicationProperties.getSecurity()
            .getSocialLogin();

        socialLogin.getGoogle()
            .setClientId("google-client");
        socialLogin.getGithub()
            .setClientId("github-client");

        assertThat(contribute(applicationProperties).get("socialLoginProviders")).asList()
            .isEmpty();
    }

    @Test
    void testContributesNoProvidersWhenSecurityIsNotBound() {
        assertThat(contribute(new ApplicationProperties()).get("socialLoginProviders")).asList()
            .isEmpty();
    }

    private static Info contribute(ApplicationProperties applicationProperties) {
        Info.Builder builder = new Info.Builder();

        new SocialLoginProvidersContributor(applicationProperties).contribute(builder);

        return builder.build();
    }
}
