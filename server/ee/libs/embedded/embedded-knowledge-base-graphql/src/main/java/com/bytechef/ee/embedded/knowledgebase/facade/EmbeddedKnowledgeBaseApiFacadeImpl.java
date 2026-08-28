/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.knowledgebase.facade;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
import com.bytechef.platform.owner.Owner;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
@SuppressFBWarnings("EI")
public class EmbeddedKnowledgeBaseApiFacadeImpl implements EmbeddedKnowledgeBaseApiFacade {

    private final KnowledgeBaseService knowledgeBaseService;

    public EmbeddedKnowledgeBaseApiFacadeImpl(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    /**
     * Filtering by an account answers "what would this account see", not "what does this account own" -- the vendor's
     * unowned knowledge bases are shared with every account and so appear under each of them. The same choice the data
     * table facade makes, for the same reason.
     */
    @Override
    @PreAuthorize("isTenantAdmin()")
    public List<KnowledgeBase> getKnowledgeBases(int environment, @Nullable Long ownerId) {
        return knowledgeBaseService.getKnowledgeBases(environment, toOwner(ownerId));
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public void assignKnowledgeBaseOwner(long knowledgeBaseId, @Nullable Long ownerId) {
        knowledgeBaseService.assignOwner(knowledgeBaseId, ownerId == null ? null : Owner.connectedUser(ownerId));
    }

    private static Optional<Owner> toOwner(@Nullable Long ownerId) {
        return ownerId == null ? Optional.empty() : Optional.of(Owner.connectedUser(ownerId));
    }
}
