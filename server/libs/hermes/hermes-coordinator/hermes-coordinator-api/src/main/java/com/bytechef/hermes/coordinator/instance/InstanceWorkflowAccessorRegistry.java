
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.coordinator.instance;

import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 */
@Component
public class InstanceWorkflowAccessorRegistry {

    private final Map<Integer, InstanceWorkflowAccessor> instanceAccessorMap;

    public InstanceWorkflowAccessorRegistry(List<InstanceWorkflowAccessor> instanceWorkflowAccessors) {
        this.instanceAccessorMap = instanceWorkflowAccessors
            .stream()
            .collect(
                Collectors.toMap(
                    InstanceWorkflowAccessor::getType, instanceWorkflowAccessor -> instanceWorkflowAccessor));
    }

    public InstanceWorkflowAccessor getInstanceWorkflowAccessor(int type) {
        return Validate.notNull(instanceAccessorMap.get(type), "instanceWorkflowAccessor");
    }
}
