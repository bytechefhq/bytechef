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
import com.bytechef.platform.configuration.domain.McpTool;
import com.bytechef.platform.configuration.repository.McpToolRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link McpToolService} interface.
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class McpToolServiceImpl implements McpToolService {

    private final McpToolRepository mcpToolRepository;

    public McpToolServiceImpl(McpToolRepository mcpToolRepository) {
        this.mcpToolRepository = mcpToolRepository;
    }

    @Override
    public McpTool create(McpTool mcpTool) {
        return mcpToolRepository.save(mcpTool);
    }

    @Override
    public McpTool update(McpTool mcpTool) {
        McpTool currentMcpTool = OptionalUtils.get(mcpToolRepository.findById(mcpTool.getId()));

        currentMcpTool.setName(mcpTool.getName());
        currentMcpTool.setMcpComponentId(mcpTool.getMcpComponentId());
        currentMcpTool.setVersion(mcpTool.getVersion());

        return mcpToolRepository.save(currentMcpTool);
    }

    @Override
    public void delete(long mcpToolId) {
        mcpToolRepository.deleteById(mcpToolId);
    }

    @Override
    public Optional<McpTool> fetchMcpTool(long mcpToolId) {
        return mcpToolRepository.findById(mcpToolId);
    }

    @Override
    public List<McpTool> getMcpTools() {
        return mcpToolRepository.findAll();
    }

    @Override
    public List<McpTool> getMcpComponentMcpTools(long mcpComponentId) {
        return mcpToolRepository.findAllByMcpComponentId(mcpComponentId);
    }
}
