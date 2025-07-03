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

package com.bytechef.platform.configuration.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.configuration.domain.McpComponent;
import com.bytechef.platform.configuration.repository.McpComponentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link McpComponentService} interface.
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class McpComponentServiceImpl implements McpComponentService {

    private final McpComponentRepository mcpComponentRepository;

    public McpComponentServiceImpl(McpComponentRepository mcpComponentRepository) {
        this.mcpComponentRepository = mcpComponentRepository;
    }

    @Override
    public McpComponent create(McpComponent mcpComponent) {
        return mcpComponentRepository.save(mcpComponent);
    }

    @Override
    public McpComponent update(McpComponent mcpComponent) {
        McpComponent currentMcpComponent = OptionalUtils.get(mcpComponentRepository.findById(mcpComponent.getId()));

        currentMcpComponent.setConnectionId(mcpComponent.getConnectionId());
        currentMcpComponent.setVersion(mcpComponent.getVersion());

        return mcpComponentRepository.save(currentMcpComponent);
    }

    @Override
    public void delete(long mcpComponentId) {
        mcpComponentRepository.deleteById(mcpComponentId);
    }

    @Override
    public McpComponent getMcpComponent(long mcpComponentId) {
        return OptionalUtils.get(mcpComponentRepository.findById(mcpComponentId));
    }

    @Override
    public List<McpComponent> getMcpComponents() {
        return mcpComponentRepository.findAll();
    }

    @Override
    public List<McpComponent> getMcpServerMcpComponents(long mcpServerId) {
        return mcpComponentRepository.findAllByMcpServerId(mcpServerId);
    }
}
