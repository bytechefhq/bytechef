
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.tag.service;

import com.bytechef.tag.config.TagIntTestConfiguration;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.repository.TagRepository;
import com.bytechef.test.annotation.EmbeddedSql;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ivica Cardic
 */
@EmbeddedSql
@SpringBootTest(classes = TagIntTestConfiguration.class)
class TagServiceIntTest {

    private Tag tag;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private TagService tagService;

    @BeforeEach
    @SuppressFBWarnings("NP")
    public void beforeEach() {
        tagRepository.deleteAll();

        tag = tagRepository.save(new Tag("name"));
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testDelete() {
        tagService.delete(tag.getId());

        assertThat(tagRepository.findById(tag.getId())).isEmpty();
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testGetTags() {
        assertThat(tagService.getTags(List.of(tag.getId()))).isEqualTo(List.of(tag));
    }

    @Test
    public void testSave() {
        tag.setName("name1");

        assertThat(tagService.save(List.of(tag, new Tag("name2"), new Tag("name3")))).hasSize(3);
        assertThat(StreamSupport.stream(tagRepository.findAll()
            .spliterator(), false)
            .map(Tag::getName)
            .toList()).contains("name1", "name2", "name3");
    }

}
