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

package com.bytechef.platform.configuration.accessor;

import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class JobPrincipalAccessorRegistry {

    private final Map<PlatformType, JobPrincipalAccessor> joPrincipalAccessorMap;

    public JobPrincipalAccessorRegistry(List<JobPrincipalAccessor> jobPrincipalAccessors) {
        this.joPrincipalAccessorMap = jobPrincipalAccessors.stream()
            .collect(
                Collectors.toMap(JobPrincipalAccessor::getType, instanceWorkflowAccessor -> instanceWorkflowAccessor));
    }

    public JobPrincipalAccessor getJobPrincipalAccessor(PlatformType type) {
        return Validate.notNull(joPrincipalAccessorMap.get(type), "joPrincipalAccessor");
    }
}
