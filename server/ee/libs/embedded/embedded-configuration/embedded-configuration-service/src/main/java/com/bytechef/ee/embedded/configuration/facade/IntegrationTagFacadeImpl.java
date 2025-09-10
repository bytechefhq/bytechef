/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class IntegrationTagFacadeImpl implements IntegrationTagFacade {

    private final IntegrationService integrationService;
    private final TagService tagService;

    @SuppressFBWarnings("EI")
    public IntegrationTagFacadeImpl(IntegrationService integrationService, TagService tagService) {
        this.integrationService = integrationService;
        this.tagService = tagService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> getIntegrationTags() {
        List<Integration> integrations = integrationService.getIntegrations();

        List<Long> tagIds = integrations.stream()
            .map(Integration::getTagIds)
            .flatMap(Collection::stream)
            .toList();

        return tagService.getTags(tagIds);
    }

    @Override
    public void updateIntegrationTags(long id, List<Tag> tags) {
        tags = CollectionUtils.isEmpty(tags) ? Collections.emptyList() : tagService.save(tags);

        integrationService.update(id, CollectionUtils.map(tags, Tag::getId));
    }
}
