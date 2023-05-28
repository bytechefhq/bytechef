
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

package com.bytechef.hermes.coordinator.job;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 */
@Component
public class InstanceFacadeRegistry {

    private final Map<String, InstanceFacade> instanceFacadeMap;

    public InstanceFacadeRegistry(List<InstanceFacade> instanceFacades) {
        this.instanceFacadeMap = instanceFacades
            .stream()
            .collect(Collectors.toMap(InstanceFacade::getType, instanceFacade -> instanceFacade));
    }

    public InstanceFacade getInstanceFacade(String type) {
        return instanceFacadeMap.get(type);
    }
}
