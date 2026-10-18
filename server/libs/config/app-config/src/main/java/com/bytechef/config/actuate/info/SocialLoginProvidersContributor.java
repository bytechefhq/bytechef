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

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Security;
import com.bytechef.config.ApplicationProperties.Security.SocialLogin;
import com.bytechef.config.ApplicationProperties.Security.SocialLogin.Provider;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Publishes which social login providers have credentials, so the login and sign-up pages offer only buttons that lead
 * somewhere.
 *
 * @author Ivica Cardic
 */
@Component
public class SocialLoginProvidersContributor implements InfoContributor {

    private final ApplicationProperties applicationProperties;

    @SuppressFBWarnings("EI")
    public SocialLoginProvidersContributor(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("socialLoginProviders", getProviders());
    }

    private List<String> getProviders() {
        List<String> providers = new ArrayList<>();

        Security security = applicationProperties.getSecurity();

        if (security == null || security.getSocialLogin() == null || !security.getSocialLogin()
            .isEnabled()) {

            return providers;
        }

        SocialLogin socialLogin = security.getSocialLogin();

        if (hasClientId(socialLogin.getGoogle())) {
            providers.add("google");
        }

        if (hasClientId(socialLogin.getGithub())) {
            providers.add("github");
        }

        return providers;
    }

    private static boolean hasClientId(Provider provider) {
        return provider != null && StringUtils.hasText(provider.getClientId());
    }
}
