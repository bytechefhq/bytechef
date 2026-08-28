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

import com.bytechef.platform.knowledgebase.audit.KnowledgeBaseAuditEvent;
import com.bytechef.platform.knowledgebase.audit.KnowledgeBaseAuditPublisher;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseRepository;
import com.bytechef.platform.owner.Owner;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@ConditionalOnProperty(prefix = "bytechef.ai.knowledge-base", name = "enabled", havingValue = "true")
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseAuditPublisher knowledgeBaseAuditPublisher;
    private final KnowledgeBaseRepository knowledgeBaseRepository;

    public KnowledgeBaseServiceImpl(
        KnowledgeBaseAuditPublisher knowledgeBaseAuditPublisher, KnowledgeBaseRepository knowledgeBaseRepository) {

        this.knowledgeBaseAuditPublisher = knowledgeBaseAuditPublisher;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
    }

    @Override
    public KnowledgeBase createKnowledgeBase(KnowledgeBase knowledgeBase) {
        KnowledgeBase savedKnowledgeBase = knowledgeBaseRepository.save(knowledgeBase);

        Map<String, Object> data = new HashMap<>();

        data.put("name", savedKnowledgeBase.getName());

        knowledgeBaseAuditPublisher.publish(KnowledgeBaseAuditEvent.KB_CREATED, savedKnowledgeBase.getId(), data);

        return savedKnowledgeBase;
    }

    @Override
    public void deleteKnowledgeBase(Long id) {
        knowledgeBaseRepository.deleteById(id);

        knowledgeBaseAuditPublisher.publish(KnowledgeBaseAuditEvent.KB_DELETED, id);
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeBase getKnowledgeBase(Long id) {
        return getKnowledgeBase(id, Optional.empty());
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeBase getKnowledgeBase(Long id, Optional<Owner> owner) {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("KnowledgeBase not found: " + id));

        if (!isReadableBy(knowledgeBase, owner)) {
            // Deliberately the same message: a caller must not be able to tell "someone else's" from "does not
            // exist", or the id space becomes an enumeration oracle.
            throw new RuntimeException("KnowledgeBase not found: " + id);
        }

        return knowledgeBase;
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBase> getKnowledgeBases() {
        return knowledgeBaseRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBase> getKnowledgeBases(Optional<Owner> owner) {
        return knowledgeBaseRepository.findAll()
            .stream()
            .filter(knowledgeBase -> isReadableBy(knowledgeBase, owner))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBase> getKnowledgeBases(int environment) {
        return knowledgeBaseRepository.findAllByEnvironment(environment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBase> getKnowledgeBases(int environment, Optional<Owner> owner) {
        return knowledgeBaseRepository.findAllByEnvironment(environment)
            .stream()
            .filter(knowledgeBase -> isReadableBy(knowledgeBase, owner))
            .toList();
    }

    /**
     * Three rules, matching the data table ones: an unowned knowledge base belongs to the vendor and is readable by
     * everyone, an empty owner is an admin or automation caller and reads everything, and an owned one is readable only
     * by that exact owner.
     */
    private static boolean isReadableBy(KnowledgeBase knowledgeBase, Optional<Owner> owner) {
        Long ownerId = knowledgeBase.getOwnerId();

        if (ownerId == null || owner.isEmpty()) {
            return true;
        }

        Owner curOwner = owner.get();

        return curOwner.id() == ownerId && curOwner.type() == knowledgeBase.getOwnerType();
    }

    @Override
    public KnowledgeBase updateKnowledgeBase(Long id, KnowledgeBase knowledgeBase) {
        KnowledgeBase existingKnowledgeBase = getKnowledgeBase(id);

        existingKnowledgeBase.setName(knowledgeBase.getName());
        existingKnowledgeBase.setDescription(knowledgeBase.getDescription());
        existingKnowledgeBase.setMaxChunkSize(knowledgeBase.getMaxChunkSize());
        existingKnowledgeBase.setMinChunkSizeChars(knowledgeBase.getMinChunkSizeChars());
        existingKnowledgeBase.setOverlap(knowledgeBase.getOverlap());

        KnowledgeBase savedKnowledgeBase = knowledgeBaseRepository.save(existingKnowledgeBase);

        knowledgeBaseAuditPublisher.publish(KnowledgeBaseAuditEvent.KB_UPDATED, id);

        return savedKnowledgeBase;
    }

    @Override
    @Transactional
    public void assignOwner(long id, @Nullable Owner owner) {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("KnowledgeBase not found: " + id));

        knowledgeBase.setOwnerId(owner == null ? null : owner.id());
        knowledgeBase.setOwnerType(owner == null ? null : owner.type());

        knowledgeBaseRepository.save(knowledgeBase);

        knowledgeBaseAuditPublisher.publish(KnowledgeBaseAuditEvent.KB_UPDATED, id);
    }
}
