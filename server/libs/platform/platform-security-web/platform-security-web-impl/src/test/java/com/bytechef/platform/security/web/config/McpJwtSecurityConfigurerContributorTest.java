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

package com.bytechef.platform.security.web.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.bytechef.platform.security.web.config.McpResourceServerProperties.Issuer;
import com.bytechef.platform.security.web.mcp.McpServerSecretVerifier;
import com.bytechef.platform.security.web.mcp.SelfIssuedTokenRevocationChecker;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/**
 * @author Ivica Cardic
 */
class McpJwtSecurityConfigurerContributorTest {

    @Test
    void testSelfIssuerWithoutRevocationCheckerFailsAtStartup() {
        McpJwtSecurityConfigurerContributor mcpJwtSecurityConfigurerContributor = createContributor(
            true, new StaticListableBeanFactory());

        assertThatThrownBy(mcpJwtSecurityConfigurerContributor::getSecurityConfigurerAdapter)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SelfIssuedTokenRevocationChecker");
    }

    @Test
    void testSelfIssuerWithRevocationCheckerBuildsConfigurer() {
        StaticListableBeanFactory staticListableBeanFactory = new StaticListableBeanFactory();

        staticListableBeanFactory.addBean(
            "selfIssuedTokenRevocationChecker", (SelfIssuedTokenRevocationChecker) (tokenValue, tenantId) -> false);

        McpJwtSecurityConfigurerContributor mcpJwtSecurityConfigurerContributor = createContributor(
            true, staticListableBeanFactory);

        assertThat((Object) mcpJwtSecurityConfigurerContributor.getSecurityConfigurerAdapter())
            .isInstanceOf(McpJwtSecurityConfigurer.class);
    }

    @Test
    void testExternalIssuerWithoutRevocationCheckerBuildsConfigurer() {
        McpJwtSecurityConfigurerContributor mcpJwtSecurityConfigurerContributor = createContributor(
            false, new StaticListableBeanFactory());

        assertThat((Object) mcpJwtSecurityConfigurerContributor.getSecurityConfigurerAdapter())
            .isInstanceOf(McpJwtSecurityConfigurer.class);
    }

    private static McpJwtSecurityConfigurerContributor createContributor(
        boolean self, StaticListableBeanFactory staticListableBeanFactory) {

        Issuer issuer = new Issuer();

        issuer.setUri("https://as.bytechef.test");
        issuer.setTenantClaim("tenant_id");
        issuer.setSelf(self);

        McpResourceServerProperties mcpResourceServerProperties = new McpResourceServerProperties();

        mcpResourceServerProperties.setIssuers(List.of(issuer));

        return new McpJwtSecurityConfigurerContributor(
            mcpResourceServerProperties, mock(UserService.class),
            staticListableBeanFactory.getBeanProvider(McpServerSecretVerifier.class),
            staticListableBeanFactory.getBeanProvider(SelfIssuedTokenRevocationChecker.class));
    }
}
