/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.knowledgebase.facade;

import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The embedded console's view of knowledge base ownership. This is the HTTP surface and it owns authorization: every
 * method is gated {@code isTenantAdmin()} on the implementation, never on the controller, because a controller wired to
 * an unguarded facade compiles fine and silently drops the check.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface EmbeddedKnowledgeBaseApiFacade {

    /**
     * @param environment the environment ordinal to list
     * @param ownerId     the connected user to filter to, or null for every knowledge base in the tenant
     */
    List<KnowledgeBase> getKnowledgeBases(int environment, @Nullable Long ownerId);

    /**
     * @param knowledgeBaseId the knowledge base to assign
     * @param ownerId         the connected user to assign it to, or null to return it to the vendor
     */
    void assignKnowledgeBaseOwner(long knowledgeBaseId, @Nullable Long ownerId);
}
