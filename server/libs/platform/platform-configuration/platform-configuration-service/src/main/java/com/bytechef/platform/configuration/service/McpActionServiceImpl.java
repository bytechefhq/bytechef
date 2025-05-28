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
import com.bytechef.platform.configuration.domain.McpAction;
import com.bytechef.platform.configuration.repository.McpActionRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link McpActionService} interface.
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class McpActionServiceImpl implements McpActionService {

    private final McpActionRepository mcpActionRepository;

    public McpActionServiceImpl(McpActionRepository mcpActionRepository) {
        this.mcpActionRepository = mcpActionRepository;
    }

    @Override
    public McpAction create(McpAction mcpAction) {
        return mcpActionRepository.save(mcpAction);
    }

    @Override
    public McpAction update(McpAction mcpAction) {
        McpAction currentMcpAction = OptionalUtils.get(mcpActionRepository.findById(mcpAction.getId()));

        currentMcpAction.setName(mcpAction.getName());
        // Skip updating parameters for now to avoid type casting issues
        currentMcpAction.setMcpComponentId(mcpAction.getMcpComponentId());
        currentMcpAction.setVersion(mcpAction.getVersion());

        return mcpActionRepository.save(currentMcpAction);
    }

    @Override
    public void delete(long mcpActionId) {
        mcpActionRepository.deleteById(mcpActionId);
    }

    @Override
    public Optional<McpAction> fetchMcpAction(long mcpActionId) {
        return mcpActionRepository.findById(mcpActionId);
    }

    @Override
    public List<McpAction> getMcpActions() {
        return mcpActionRepository.findAll();
    }

    @Override
    public List<McpAction> getMcpActionsByComponentId(long mcpComponentId) {
        return mcpActionRepository.findAllByMcpComponentId(mcpComponentId);
    }
}
