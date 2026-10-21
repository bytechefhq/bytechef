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

package com.bytechef.platform.oauth2.authorizationserver;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.oauth2.authorizationserver.config.Oauth2AuthorizationServerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.web.SecurityFilterChain;

/**
 * @author Ivica Cardic
 */
class Oauth2AuthorizationServerConfigurationTest {

    private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
        .withUserConfiguration(Oauth2AuthorizationServerConfiguration.class);

    @Test
    void testNoAuthorizationServerBeansWhenPropertyIsAbsent() {
        applicationContextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(Oauth2AuthorizationServerConfiguration.class);
            assertThat(context.getBeansOfType(SecurityFilterChain.class)).isEmpty();
        });
    }

    @Test
    void testNoAuthorizationServerBeansWhenPropertyIsFalse() {
        applicationContextRunner.withPropertyValues("bytechef.oauth2.authorization-server.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(Oauth2AuthorizationServerConfiguration.class);
                assertThat(context.getBeansOfType(SecurityFilterChain.class)).isEmpty();
            });
    }
}
