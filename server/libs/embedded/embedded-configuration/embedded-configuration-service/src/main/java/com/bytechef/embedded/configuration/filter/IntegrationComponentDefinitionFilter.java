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

package com.bytechef.embedded.configuration.filter;

import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.filter.ComponentDefinitionFilter;
import com.bytechef.platform.constant.ModeType;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class IntegrationComponentDefinitionFilter implements ComponentDefinitionFilter {

    private static final List<String> COMPONENT_NAMES = List.of("apiPlatform", "webhook");

    @Override
    public boolean filter(ComponentDefinition componentDefinition) {
        return !COMPONENT_NAMES.contains(componentDefinition.getName());
    }

    @Override
    public boolean supports(ModeType type) {
        return ModeType.EMBEDDED.equals(type);
    }
}
