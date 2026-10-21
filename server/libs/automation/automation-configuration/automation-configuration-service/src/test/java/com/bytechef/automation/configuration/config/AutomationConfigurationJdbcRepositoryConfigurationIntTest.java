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

package com.bytechef.automation.configuration.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwnerContributor;
import com.bytechef.automation.configuration.ownership.ProjectOwnerContributor;
import com.bytechef.automation.configuration.service.ProjectOwnershipServiceImpl;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ProjectIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@ProjectIntTestConfigurationSharedMocks
public class AutomationConfigurationJdbcRepositoryConfigurationIntTest {

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    public void testTheEnterpriseEditionPropertyRequiresTheEmbeddedOwnerContributors() {
        ProjectOwnershipServiceImpl projectOwnershipServiceImpl = createProjectOwnershipService(
            new MockEnvironment().withProperty("bytechef.edition", "ee"));

        assertThatThrownBy(projectOwnershipServiceImpl::afterSingletonsInstantiated)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("enterprise edition");
    }

    @Test
    public void testWithoutTheEditionPropertyTheEmbeddedOwnerContributorsAreOptional() {
        ProjectOwnershipServiceImpl projectOwnershipServiceImpl = createProjectOwnershipService(
            new MockEnvironment());

        assertThatCode(projectOwnershipServiceImpl::afterSingletonsInstantiated).doesNotThrowAnyException();
    }

    private ProjectOwnershipServiceImpl createProjectOwnershipService(MockEnvironment environment) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

        return (ProjectOwnershipServiceImpl) new AutomationConfigurationJdbcRepositoryConfiguration()
            .projectOwnershipService(
                jdbcClient, beanFactory.getBeanProvider(ProjectDeploymentOwnerContributor.class),
                beanFactory.getBeanProvider(ProjectOwnerContributor.class), environment);
    }
}
