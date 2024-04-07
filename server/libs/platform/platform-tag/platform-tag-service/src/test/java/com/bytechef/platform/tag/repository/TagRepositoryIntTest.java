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

package com.bytechef.platform.tag.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.tag.config.TagIntTestConfiguration;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = TagIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
public class TagRepositoryIntTest {

    @Autowired
    private TagRepository tagRepository;

    @AfterEach
    public void afterEach() {
        tagRepository.deleteAll();
    }

    @Test
    public void testCreate() {
        Tag tag = tagRepository.save(new Tag("name"));

        assertThat(tag).isEqualTo(tagRepository.findById(Validate.notNull(tag.getId(), "id"))
            .get());
    }

    @Test
    public void testDelete() {
        Tag tag = tagRepository.save(new Tag("name"));

        Tag resultTag = OptionalUtils.get(tagRepository.findById(Validate.notNull(tag.getId(), "id")));

        assertThat(resultTag).isEqualTo(tag);

        tagRepository.deleteById(Validate.notNull(resultTag.getId(), "id"));

        assertThat(tagRepository.findById(tag.getId())).isEmpty();
    }

    @Test
    public void testFindById() {
        Tag tag = tagRepository.save(new Tag("name"));

        assertThat(tagRepository.findById(Validate.notNull(tag.getId(), "id"))).hasValue(tag);
    }

    @Test
    public void testUpdate() {
        Tag tag = tagRepository.save(new Tag("name"));

        assertThat(tagRepository.findById(Validate.notNull(tag.getId(), "id"))).hasValue(tag);

        tag.setName("name2");

        tag = tagRepository.save(tag);

        assertThat(tagRepository.findById(Validate.notNull(tag.getId(), "id"))).hasValue(tag);
    }
}
