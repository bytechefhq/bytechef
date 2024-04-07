/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.tag.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.tag.config.TagIntTestConfiguration;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.repository.TagRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = TagIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class TagServiceIntTest {

    private Tag tag;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private TagService tagService;

    @BeforeEach
    public void beforeEach() {
        tag = tagRepository.save(new Tag("name"));
    }

    @AfterEach
    public void afterEach() {
        tagRepository.deleteAll();
    }

    @Test
    public void testDelete() {
        tagService.delete(Validate.notNull(tag.getId(), "id"));

        assertThat(tagRepository.findById(tag.getId())).isEmpty();
    }

    @Test
    public void testGetTags() {
        assertThat(tagService.getTags(List.of(Validate.notNull(tag.getId(), "id")))).isEqualTo(List.of(tag));
    }

    @Test
    public void testSave() {
        tag.setName("name1");

        assertThat(tagService.save(List.of(tag, new Tag("name2"), new Tag("name3")))).hasSize(3);
        assertThat(tagRepository.findAll()
            .stream()
            .map(Tag::getName)
            .toList()).contains("name1", "name2", "name3");
    }

}
