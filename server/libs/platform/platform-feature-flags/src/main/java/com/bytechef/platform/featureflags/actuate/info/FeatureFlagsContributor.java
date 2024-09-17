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

package com.bytechef.platform.featureflags.actuate.info;

import com.bytechef.config.ApplicationProperties;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

/**
 * Feature flags info contributor.
 *
 * @author Ivica Cardic
 */
@Component
public class FeatureFlagsContributor implements InfoContributor {

    private static final Logger log = LoggerFactory.getLogger(FeatureFlagsContributor.class);

    private final List<String> featureFlags;

    public FeatureFlagsContributor(ApplicationProperties applicationProperties) {
        this.featureFlags = applicationProperties.getFeatureFlags();

        if (featureFlags != null && !featureFlags.isEmpty()) {
            log.info("Feature flags: {}", featureFlags);
        }
    }

    @Override
    public void contribute(Info.Builder builder) {
        if (featureFlags != null) {
            builder.withDetail(
                "featureFlags",
                featureFlags.stream()
                    .collect(Collectors.toMap(featureFlag -> featureFlag, featureFlag -> true)));
        }
    }
}
