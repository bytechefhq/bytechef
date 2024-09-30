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

package com.bytechef.platform.user.actuate.info;

import com.bytechef.config.ApplicationProperties;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

/**
 * Feature flags info contributor.
 *
 * @author Ivica Cardic
 */
@Component
public class SignUpContributor implements InfoContributor {

    private final ApplicationProperties.SignUp signUp;

    public SignUpContributor(ApplicationProperties applicationProperties) {
        this.signUp = applicationProperties.getSignUp();
    }

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("signUp", signUp);
    }
}
