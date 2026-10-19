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
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class TenantInfoContributor implements InfoContributor {

    private final ApplicationProperties.Tenant tenant;

    public TenantInfoContributor(ApplicationProperties applicationProperties) {
        this.tenant = applicationProperties.getTenant();
    }

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail(
            "tenant",
            Map.of(
                "mode", tenant.getMode()
                    .name()
                    .toLowerCase(Locale.ROOT),
                "deletionGracePeriodDays", tenant.getDeletion()
                    .getGracePeriod()
                    .toDays()));
    }
}
