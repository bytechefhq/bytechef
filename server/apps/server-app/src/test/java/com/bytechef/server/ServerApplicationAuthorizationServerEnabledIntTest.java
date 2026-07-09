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

package com.bytechef.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.oauth2.authorizationserver.config.Oauth2AuthorizationServerConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

/**
 * Verifies that when {@code bytechef.oauth2.authorization-server.enabled=true} the whole application context still
 * loads: the embedded authorization server's highest-precedence, {@code securityMatcher}-scoped filter chain coexists
 * with {@code SecurityConfiguration}'s actuator, API and catch-all chains. An unscoped authorization-server chain would
 * make Spring Security fail context startup, so a successful load is the regression guard for the co-hosting scoping.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "bytechef.oauth2.authorization-server.enabled=true", "bytechef.edition=ee"
    })
@Import({
    PostgreSQLContainerConfiguration.class, ServerApplicationIntTest.ServerApplicationIntTestConfiguration.class
})
class ServerApplicationAuthorizationServerEnabledIntTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void testContextLoadsWithAuthorizationServerEnabled() {
        assertThat(applicationContext.getBeanNamesForType(Oauth2AuthorizationServerConfiguration.class)).isNotEmpty();
    }
}
