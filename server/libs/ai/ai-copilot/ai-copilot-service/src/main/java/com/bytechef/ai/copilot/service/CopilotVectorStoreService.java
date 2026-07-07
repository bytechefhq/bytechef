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

package com.bytechef.ai.copilot.service;

import com.bytechef.ai.copilot.domain.CopilotVectorStore;
import com.bytechef.ai.copilot.repository.CopilotVectorStoreRepository;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * @author Marko Kriskovic
 */
@Service
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class CopilotVectorStoreService {
    private final CopilotVectorStoreRepository repository;

    public CopilotVectorStoreService(CopilotVectorStoreRepository repository) {
        this.repository = repository;
    }

    public long count() {
        return repository.count();
    }

    public List<CopilotVectorStore> findAll() {
        return repository.findAll();
    }
}
