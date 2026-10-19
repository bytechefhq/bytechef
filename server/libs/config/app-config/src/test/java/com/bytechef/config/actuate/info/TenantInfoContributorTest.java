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
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.Info;

/**
 * @author Ivica Cardic
 */
class TenantInfoContributorTest {

    @Test
    void testContributeDefaults() {
        Info info = contribute(new ApplicationProperties());

        assertThat(info.getDetails()).containsEntry("tenant", Map.of("mode", "single", "deletionGracePeriodDays", 30L));
    }

    @Test
    void testContributeMultiTenantWithCustomGracePeriod() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getTenant()
            .setMode(ApplicationProperties.Tenant.Mode.MULTI);
        applicationProperties.getTenant()
            .getDeletion()
            .setGracePeriod(Duration.ofDays(7));

        Info info = contribute(applicationProperties);

        assertThat(info.getDetails()).containsEntry("tenant", Map.of("mode", "multi", "deletionGracePeriodDays", 7L));
    }

    private static Info contribute(ApplicationProperties applicationProperties) {
        Info.Builder builder = new Info.Builder();

        new TenantInfoContributor(applicationProperties).contribute(builder);

        return builder.build();
    }
}
