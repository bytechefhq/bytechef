/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.facade;

import com.bytechef.ee.platform.contextstore.domain.ContextStore;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;

/**
 * @version ee
 * @author Ivica Cardic
 */
public interface ContextStoreFacade {

    List<ContextStore> getContextStores(Long workspaceId, Long environmentId);

    ContextStore getContextStore(Long id);

    List<Tag> getContextStoreTags(Long workspaceId);

    Long findContextStoreIdByName(Long workspaceId, String name, Long environmentId);

    ContextStore createContextStore(ContextStore contextStore, Long workspaceId, Long environmentId);

    ContextStore updateContextStore(Long workspaceId, ContextStore contextStore);

    List<Tag> updateContextStoreTags(Long workspaceId, Long id, List<Tag> tags);

    void deleteContextStore(Long workspaceId, Long id);
}
