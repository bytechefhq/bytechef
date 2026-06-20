/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class ApiCollectionFacadeImplTest {

    @Mock
    private ApiCollectionService apiCollectionService;

    @Mock
    private TagService tagService;

    @InjectMocks
    private ApiCollectionFacadeImpl apiCollectionFacade;

    @Test
    void testGetApiCollectionTagsScopesToWorkspace() {
        ApiCollection apiCollection = new ApiCollection();

        apiCollection.setTagIds(List.of(10L, 11L));

        when(apiCollectionService.getApiCollections(5L, null, null, null)).thenReturn(List.of(apiCollection));
        when(tagService.getTags(List.of(10L, 11L))).thenReturn(List.of(new Tag("a"), new Tag("b")));

        List<Tag> tags = apiCollectionFacade.getApiCollectionTags(5L);

        assertThat(tags).hasSize(2);
    }
}
