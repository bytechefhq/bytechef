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

package com.bytechef.server.promotion;

import static com.bytechef.ee.automation.promotion.test.assertion.PromotionContributorAssertions.assertEveryContributorCallsConnectionScopeCheck;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.ai.promotion.contributor.A2aServerPromotionContributor;
import com.bytechef.ee.automation.ai.promotion.contributor.McpServerPromotionContributor;
import com.bytechef.ee.automation.apiplatform.promotion.contributor.ApiCollectionPromotionContributor;
import com.bytechef.ee.automation.configuration.promotion.contributor.ProjectDeploymentPromotionContributor;
import com.bytechef.ee.automation.data.table.promotion.contributor.DataTablePromotionContributor;
import com.bytechef.ee.embedded.configuration.promotion.contributor.AutomationConfigurationPromotionContributor;
import com.bytechef.ee.embedded.configuration.promotion.contributor.IntegrationInstanceConfigurationPromotionContributor;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class EnvironmentPromotionContributorConnectionScopeTest {

    @Test
    void testEveryEnvironmentPromotionContributorOnTheServerClasspathCallsConnectionScopeCheck() {
        Set<Class<?>> contributorClasses = assertEveryContributorCallsConnectionScopeCheck("com.bytechef.ee");

        assertThat(contributorClasses).contains(
            A2aServerPromotionContributor.class, ApiCollectionPromotionContributor.class,
            AutomationConfigurationPromotionContributor.class, DataTablePromotionContributor.class,
            IntegrationInstanceConfigurationPromotionContributor.class, McpServerPromotionContributor.class,
            ProjectDeploymentPromotionContributor.class);
    }
}
