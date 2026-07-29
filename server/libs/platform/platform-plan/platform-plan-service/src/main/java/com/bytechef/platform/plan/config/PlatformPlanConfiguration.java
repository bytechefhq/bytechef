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

package com.bytechef.platform.plan.config;

import com.bytechef.platform.plan.provider.PlanLimitsProvider;
import com.bytechef.platform.plan.provider.PropertiesPlanLimitsProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the properties-backed {@link PlanLimitsProvider} unless a billing integration supplies its own bean.
 *
 * @author Ivica Cardic
 */
@Configuration
@EnableConfigurationProperties(PlanProperties.class)
public class PlatformPlanConfiguration {

    @Bean
    @ConditionalOnMissingBean(PlanLimitsProvider.class)
    PlanLimitsProvider planLimitsProvider(PlanProperties planProperties) {
        return new PropertiesPlanLimitsProvider(planProperties);
    }
}
