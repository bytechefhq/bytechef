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

package com.bytechef.platform.knowledgebase.service;

import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.knowledgebase.audit.KnowledgeBaseAuditEvents;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@ConditionalOnProperty(prefix = "bytechef.ai.knowledge-base", name = "enabled", havingValue = "true")
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;

    public KnowledgeBaseServiceImpl(KnowledgeBaseRepository knowledgeBaseRepository) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
    }

    @Override
    public KnowledgeBase createKnowledgeBase(KnowledgeBase knowledgeBase) {
        return knowledgeBaseRepository.save(knowledgeBase);
    }

    @Override
    public void deleteKnowledgeBase(Long id) {
        knowledgeBaseRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeBase getKnowledgeBase(Long id) {
        return knowledgeBaseRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Knowledge base " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBase> getKnowledgeBases() {
        return knowledgeBaseRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBase> getKnowledgeBases(int environment) {
        return knowledgeBaseRepository.findAllByEnvironment(environment);
    }

    @Override
    @Audited(event = KnowledgeBaseAuditEvents.KB_UPDATED, platform = PlatformType.AUTOMATION, data = {
        @AuditData(key = "knowledgeBaseId", value = "#id"),
        @AuditData(key = "name", value = "#result?.name")
    })
    public KnowledgeBase updateKnowledgeBase(Long id, KnowledgeBase knowledgeBase) {
        KnowledgeBase existingKnowledgeBase = getKnowledgeBase(id);

        existingKnowledgeBase.setName(knowledgeBase.getName());
        existingKnowledgeBase.setDescription(knowledgeBase.getDescription());
        existingKnowledgeBase.setMaxChunkSize(knowledgeBase.getMaxChunkSize());
        existingKnowledgeBase.setMinChunkSizeChars(knowledgeBase.getMinChunkSizeChars());
        existingKnowledgeBase.setOverlap(knowledgeBase.getOverlap());

        return knowledgeBaseRepository.save(existingKnowledgeBase);
    }
}
